package com.example.vitalwearclonev1.communication

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.example.vitalwearclonev1.lab.LabStorage
import com.example.vitalwearclonev1.lab.StoredMonster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Base64
import timber.log.Timber

class VitalWearHceService : HostApduService() {
    companion object {
        private const val TAG = "VW_HCE_SERVICE"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_WRONG_LENGTH)
        }

        if (VitalWearHceProtocol.isSelectAidApdu(commandApdu)) {
            return VitalWearHceProtocol.buildResponse()
        }

        if (commandApdu.size < 2 || commandApdu[0] != VitalWearHceProtocol.CLA_VITALWEAR) {
            return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_WRONG_DATA)
        }

        val ins = commandApdu[1]
        val data = VitalWearHceProtocol.parseCommandData(commandApdu)

        return try {
            when (ins) {
                VitalWearHceProtocol.INS_NEGOTIATE -> handleNegotiate(data)
                VitalWearHceProtocol.INS_READ_CHUNK -> handleReadChunk(data)
                VitalWearHceProtocol.INS_WRITE_CHUNK -> handleWriteChunk(data)
                VitalWearHceProtocol.INS_COMMIT -> handleCommit()
                VitalWearHceProtocol.INS_STATUS -> handleStatus()
                else -> VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_FUNC_NOT_SUPPORTED)
            }
        } catch (error: Exception) {
            Timber.e(error, "APDU handling failed for INS=${String.format("0x%02X", ins)}")
            VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_INTERNAL_ERROR)
        }
    }

    override fun onDeactivated(reason: Int) {
        VitalWearHceSessionManager.clear(resetStatus = false)
    }

    private fun handleNegotiate(data: ByteArray): ByteArray {
        if (data.size < 2) {
            return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_WRONG_LENGTH)
        }

        val mode = data[0]
        val version = data[1]
        if (version != VitalWearHceProtocol.VERSION_1) {
            return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_FUNC_NOT_SUPPORTED)
        }

        if (VitalWearHceSessionManager.currentMode() == VitalWearHceSessionManager.Mode.IDLE) {
            when (mode) {
                VitalWearHceProtocol.MODE_WATCH_TO_PHONE -> {
                    // Watch wants to send to phone.
                    VitalWearHceSessionManager.armReceive()
                }
                VitalWearHceProtocol.MODE_PHONE_TO_WATCH -> {
                    // Phone wants to send to watch (Watch is reader).
                    val monster = PhoneMonsterManager(this).getCurrentMonster()
                    if (monster != null) {
                        val payload = formatMonsterForHce(monster)
                        VitalWearHceSessionManager.armSend(payload)
                    } else {
                        return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_CONDITIONS_NOT_SATISFIED)
                    }
                }
            }
        }

        val session = VitalWearHceSessionManager.negotiate(mode)
            ?: return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_CONDITIONS_NOT_SATISFIED)

        val responseData = byteArrayOf(
            VitalWearHceProtocol.VERSION_1,
            mode,
            ((VitalWearHceProtocol.DEFAULT_MAX_CHUNK_SIZE ushr 8) and 0xFF).toByte(),
            (VitalWearHceProtocol.DEFAULT_MAX_CHUNK_SIZE and 0xFF).toByte(),
            ((session.payloadLength ushr 24) and 0xFF).toByte(),
            ((session.payloadLength ushr 16) and 0xFF).toByte(),
            ((session.payloadLength ushr 8) and 0xFF).toByte(),
            (session.payloadLength and 0xFF).toByte(),
        )
        return VitalWearHceProtocol.buildResponse(responseData)
    }

    private fun handleReadChunk(data: ByteArray): ByteArray {
        if (data.size != 4) {
            return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_WRONG_LENGTH)
        }
        val offset =
            ((data[0].toInt() and 0xFF) shl 24) or
                ((data[1].toInt() and 0xFF) shl 16) or
                ((data[2].toInt() and 0xFF) shl 8) or
                (data[3].toInt() and 0xFF)

        val chunk = VitalWearHceSessionManager.readChunk(offset, VitalWearHceProtocol.DEFAULT_MAX_CHUNK_SIZE)
            ?: return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_CONDITIONS_NOT_SATISFIED)
        return VitalWearHceProtocol.buildResponse(chunk)
    }

    private fun handleWriteChunk(data: ByteArray): ByteArray {
        if (data.size < 4) {
            return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_WRONG_LENGTH)
        }
        val offset =
            ((data[0].toInt() and 0xFF) shl 24) or
                ((data[1].toInt() and 0xFF) shl 16) or
                ((data[2].toInt() and 0xFF) shl 8) or
                (data[3].toInt() and 0xFF)
        val chunk = data.copyOfRange(4, data.size)
        val accepted = VitalWearHceSessionManager.writeChunk(offset, chunk)
        return if (accepted) {
            VitalWearHceProtocol.buildResponse()
        } else {
            VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_WRONG_P1P2)
        }
    }

    private fun handleCommit(): ByteArray {
        return when (VitalWearHceSessionManager.currentMode()) {
            VitalWearHceSessionManager.Mode.SEND_TO_PHONE -> {
                VitalWearHceSessionManager.markSuccess()
                VitalWearHceSessionManager.clear(resetStatus = false)
                VitalWearHceProtocol.buildResponse()
            }

            VitalWearHceSessionManager.Mode.RECEIVE_FROM_PHONE -> {
                val payload = VitalWearHceSessionManager.takeReceivedPayload()
                    ?: return VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_CONDITIONS_NOT_SATISFIED)

                VitalWearHceSessionManager.clear(resetStatus = false)
                serviceScope.launch {
                    try {
                        val monster = parseMonsterFromHce(payload)
                        LabStorage.addMonster(this@VitalWearHceService, monster)
                        VitalWearHceSessionManager.markSuccess()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to import monster", e)
                        VitalWearHceSessionManager.markFailure()
                    }
                }
                VitalWearHceProtocol.buildResponse()
            }

            VitalWearHceSessionManager.Mode.IDLE -> {
                VitalWearHceSessionManager.markFailure()
                VitalWearHceProtocol.buildResponse(statusWord = VitalWearHceProtocol.SW_CONDITIONS_NOT_SATISFIED)
            }
        }
    }

    private fun handleStatus(): ByteArray {
        val statusByte = when (VitalWearHceSessionManager.transferStatus.value) {
            VitalWearHceSessionManager.TransferStatus.IDLE -> 0x00.toByte()
            VitalWearHceSessionManager.TransferStatus.ARMED_SEND -> 0x01.toByte()
            VitalWearHceSessionManager.TransferStatus.ARMED_RECEIVE -> 0x02.toByte()
            VitalWearHceSessionManager.TransferStatus.SYNCING -> 0x03.toByte()
            VitalWearHceSessionManager.TransferStatus.SUCCESS -> 0x04.toByte()
            VitalWearHceSessionManager.TransferStatus.FAILURE -> 0x05.toByte()
        }
        return VitalWearHceProtocol.buildResponse(byteArrayOf(statusByte))
    }

    private fun formatMonsterForHce(monster: PhoneMonsterManager.MonsterState): ByteArray {
        // Very simple pipe-separated format for proof of concept
        val data = "${monster.cardName}|${monster.characterId}|${monster.stage}|${monster.attackBonus}|${monster.healthBonus}|${monster.speedBonus}|${monster.defenseBonus}|${monster.xp}|${monster.level}|${monster.nickname ?: ""}|${monster.currentWins}|${monster.winsRequired}|${monster.timeAlive}|${monster.evolutionTime}"
        return data.toByteArray(Charsets.UTF_8)
    }

    private fun parseMonsterFromHce(payload: ByteArray): StoredMonster {
        val data = String(payload, Charsets.UTF_8)
        val parts = data.split("|")
        return StoredMonster(
            name = parts[0],
            charId = parts[1].toInt(),
            stage = parts[2].toInt(),
            attack = parts[3].toInt(),
            calories = parts[4].toInt(),
            speed = parts[5].toInt(),
            defense = parts[6].toInt(),
            xp = parts[7].toInt(),
            level = parts[8].toInt(),
            nickname = if (parts.size > 9) parts[9].ifEmpty { null } else null,
            currentWins = if (parts.size > 10) parts[10].toIntOrNull() ?: 0 else 0,
            winsRequired = if (parts.size > 11) parts[11].toIntOrNull() ?: 10 else 10,
            timeAlive = if (parts.size > 12) parts[12].toLongOrNull() ?: 0L else 0L,
            evolutionTime = if (parts.size > 13) parts[13].toLongOrNull() ?: 3600L else 3600L
        )
    }
}
