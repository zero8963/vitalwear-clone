package com.example.vitalwearclonev1.communication

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import com.example.vitalwearclonev1.lab.StoredMonster
import timber.log.Timber
import android.util.Base64
import com.example.vitalwearclonev1.common.util.Endian
import com.example.vitalwearclonev1.common.util.getUInt16
import com.example.vitalwearclonev1.common.util.toByteArray
import com.example.vitalwearclonev1.communication.secrets.SecretsManager

class VBNfcProtocol(private val tag: Tag, private val context: Context) {

    enum class SyncPhase {
        HANDSHAKE,
        TRANSFER,
        UNKNOWN
    }

    private val nfc = MifareUltralight.get(tag)

    companion object {
        private const val STATUS_READY_FLAG: Byte = 0x01
        
        // Swapped OP Codes based on user device behavior
        private const val OP_IDLE: Byte = 0x00
        private const val OP_UPLOAD: Byte = 0x03   // Watch -> Phone (Walk Out)
        private const val OP_DOWNLOAD: Byte = 0x02 // Phone -> Watch (Insert DIM)
        private const val OP_COMMIT: Byte = 0x01   // Phone -> Watch (Final)
        
        // Handshake Commands for Page 6
        private const val CMD_UPLOAD: Byte = 0x01   
        private const val CMD_DOWNLOAD: Byte = 0x02 

        private val SBOX = intArrayOf(
            0x45, 0x8a, 0x01, 0x2f, 0xa3, 0x6c, 0x78, 0xe9, 0xb0, 0x1d, 0x54, 0xc7, 0x32, 0x9b, 0xf6, 0x0d,
            0x8e, 0x25, 0x7a, 0xd1, 0x3c, 0xb9, 0xf0, 0x4d, 0x63, 0x17, 0xa8, 0xd5, 0x02, 0xfc, 0x96, 0xb4,
            0x19, 0xa2, 0x6b, 0x7f, 0xe8, 0xb1, 0x0d, 0x5a, 0xc6, 0x33, 0x9a, 0xf7, 0x0c, 0x45, 0x8b, 0x2d,
            0xd0, 0x3d, 0xb8, 0xf1, 0x4c, 0x62, 0x16, 0xa9, 0xd4, 0x03, 0xfd, 0x97, 0xb5, 0x8f, 0x24, 0x7b,
            0xa1, 0x6a, 0x7e, 0xe9, 0xb0, 0x1c, 0x5b, 0xc7, 0x32, 0x9b, 0xf6, 0x0d, 0x44, 0x8a, 0x25, 0x78,
            0x3c, 0xb9, 0xf0, 0x4d, 0x63, 0x17, 0xa8, 0xd5, 0x02, 0xfc, 0x96, 0xb4, 0x19, 0xa2, 0x6b, 0x7f,
            0xe8, 0xb1, 0x0d, 0x5a, 0xc6, 0x33, 0x9a, 0xf7, 0x0c, 0x45, 0x8b, 0x2d, 0xd0, 0x3d, 0xb8, 0xf1,
            0x4c, 0x62, 0x16, 0xa9, 0xd4, 0x03, 0xfd, 0x97, 0xb5, 0x8f, 0x24, 0x7b, 0xa1, 0x6a, 0x7e, 0xe9,
            0xb0, 0x1c, 0x5b, 0xc7, 0x32, 0x9b, 0xf6, 0x0d, 0x44, 0x8a, 0x25, 0x78, 0x3c, 0xb9, 0xf0, 0x4d,
            0x63, 0x17, 0xa8, 0xd5, 0x02, 0xfc, 0x96, 0xb4, 0x19, 0xa2, 0x6b, 0x7f, 0xe8, 0xb1, 0x0d, 0x5a,
            0xc6, 0x33, 0x9a, 0xf7, 0x0c, 0x45, 0x8b, 0x2d, 0xd0, 0x3d, 0xb8, 0xf1, 0x4c, 0x62, 0x16, 0xa9,
            0xd4, 0x03, 0xfd, 0x97, 0xb5, 0x8f, 0x24, 0x7b, 0xa1, 0x6a, 0x7e, 0xe9, 0xb0, 0x1c, 0x5b, 0xc7,
            0x32, 0x9b, 0xf6, 0x0d, 0x44, 0x8a, 0x25, 0x78, 0x3c, 0xb9, 0xf0, 0x4d, 0x63, 0x17, 0xa8, 0xd5,
            0x02, 0xfc, 0x96, 0xb4, 0x19, 0xa2, 0x6b, 0x7f, 0xe8, 0xb1, 0x0d, 0x5a, 0xc6, 0x33, 0x9a, 0xf7,
            0x0c, 0x45, 0x8b, 0x2d, 0xd0, 0x3d, 0xb8, 0xf1, 0x4c, 0x62, 0x16, 0xa9, 0xd4, 0x03, 0xfd, 0x97,
            0xb5, 0x8f, 0x24, 0x7b, 0xa1, 0x6a, 0x7e, 0xe9, 0xb0, 0x1c, 0x5b, 0xc7, 0x32, 0x9b, 0xf6, 0x0d
        )
    }

    private fun deobfuscate(data: ByteArray): ByteArray {
        val secrets = SecretsManager(context).getSecrets()
        val table = secrets.vbCipher ?: SBOX
        val res = ByteArray(data.size)
        for (i in data.indices) {
            val idx = data[i].toInt() and 0xFF
            res[i] = table[idx % table.size].toByte()
        }
        return res
    }

    private fun calculateVbPassword(uid: ByteArray?): ByteArray {
        val password = ByteArray(4)
        if ((uid != null) && (uid.size >= 7)) {
            // Standard DIM Password
            password[0] = (uid[0].toInt() xor uid[4].toInt()).toByte()
            password[1] = (uid[1].toInt() xor uid[5].toInt()).toByte()
            password[2] = (uid[2].toInt() xor uid[6].toInt()).toByte()
            password[3] = (uid[3].toInt() xor 0x20).toByte()
        } else {
            password[0] = 0x12; password[1] = 0x34; password[2] = 0x56; password[3] = 0x78.toByte()
        }
        return password
    }

    private fun calculateHeroPassword(uid: ByteArray?): ByteArray {
        val pw = ByteArray(4)
        if ((uid != null) && (uid.size >= 7)) {
            // Vital Hero / Bracelet V / BE Password
            pw[0] = (uid[1].toInt() xor uid[3].toInt()).toByte()
            pw[1] = (uid[2].toInt() xor uid[4].toInt()).toByte()
            pw[2] = (uid[0].toInt() xor uid[5].toInt()).toByte()
            pw[3] = (uid[6].toInt() xor 0xFE).toByte()
        }
        return pw
    }

    private fun connectAndAuth(onProgress: (String) -> Unit): Boolean {
        try {
            nfc.timeout = 30000 
            nfc.connect()
            
            val uid = nfc.tag.id
            val vbPass = calculateVbPassword(uid)
            val heroPass = calculateHeroPassword(uid)
            val authNames = listOf("BNDI", "VB-PASS", "HERO-PASS", "NULL", "MAX")
            val passwords = listOf(
                byteArrayOf(0x42, 0x4E, 0x44, 0x49),
                vbPass,
                heroPass,
                byteArrayOf(0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
            )

            for (i in passwords.indices) {
                try {
                    nfc.transceive(byteArrayOf(0x1B.toByte()) + passwords[i])
                    onProgress("Unlocked: ${authNames[i]}")
                    return true
                } catch (_: Exception) {}
            }

            try { 
                nfc.readPages(4)
                onProgress("Unlocked: PASSIVE")
                Timber.d("NFC Auth: Passive Read Success")
                return true 
            } catch (e: Exception) {
                Timber.w("NFC Auth: Passive Read Failed: ${e.message}")
            }

            onProgress("Unlocked: FAILED")
            return false
        } catch (e: Exception) {
            onProgress("Conn Error")
            return false
        }
    }

    fun performSyncStep(
        isInitialTap: Boolean,
        monster: StoredMonster,
        realCardId: Int,
        onProgress: (String) -> Unit
    ): Pair<Boolean, SyncPhase> {
        try {
            onProgress("Connecting...")
            if (!connectAndAuth(onProgress)) return false to SyncPhase.UNKNOWN

            // Pulse to wake chip
            try { nfc.transceive(byteArrayOf(0x30.toByte(), 0x00.toByte())) } catch (_: Exception) {}

            // 1. READ STATE
            val read4 = nfc.readPages(4) ?: return false to SyncPhase.UNKNOWN
            val watchDimId = read4[1].toInt() and 0xFF
            val watchOp = read4[2].toInt() and 0xFF
            val watchDir = read4[12].toInt() and 0xFF 
            
            Timber.d("VBNfcProtocol performSyncStep: isInitial=$isInitialTap Op=$watchOp DimID=$watchDimId Dir=$watchDir Raw4=${read4.toHexString()}")
            onProgress("Op:$watchOp Dim:$watchDimId")

            // 2. HANDSHAKE (Tap 1)
            if (isInitialTap) {
                onProgress("Linking Watch...")
                val targetId = if (realCardId > 0) realCardId else (if (watchDimId > 0) watchDimId else 1)
                
                // Link Block for Download (Phone -> Watch)
                val handshakeBlock = byteArrayOf(
                    STATUS_READY_FLAG, 
                    targetId.toByte(), 
                    CMD_DOWNLOAD, 
                    (STATUS_READY_FLAG.toInt() xor targetId xor CMD_DOWNLOAD.toInt()).toByte()
                )

                try {
                    // Clear state
                    nfc.writePage(2, byteArrayOf(OP_IDLE, 0x00, 0x00, 0x00))
                    
                    // Write Handshake
                    var writeOk = false
                    for (retry in 1..5) {
                        try {
                            nfc.writePage(6, handshakeBlock)
                            writeOk = true
                            break
                        } catch (e: Exception) { Thread.sleep(50) }
                    }
                    if (!writeOk) {
                        onProgress("Auth Fail")
                        return false to SyncPhase.HANDSHAKE
                    }
                    
                    // Trigger "Insert DIM" (0x03)
                    nfc.writePage(2, byteArrayOf(OP_DOWNLOAD, 0x00, 0x00, 0x00))
                    try { nfc.writePage(7, byteArrayOf(OP_DOWNLOAD, 0x00, 0x00, 0x00)) } catch(_: Exception) {}
                    
                    onProgress("Link OK")
                    return true to SyncPhase.HANDSHAKE
                } catch (e: Exception) {
                    onProgress("Handshake Error")
                    return false to SyncPhase.HANDSHAKE
                }
            } else {
                // 3. TRANSFER DATA (Tap 2)
                if (watchOp == 1) {
                     // If watch is in Send mode, force reset to receive
                     nfc.writePage(2, byteArrayOf(OP_IDLE, 0x00, 0x00, 0x00))
                     Thread.sleep(200)
                }
                
                Timber.d("Starting internalTransfer for data.")
                return internalTransfer(monster, onProgress) to SyncPhase.TRANSFER
            }
        } catch (e: Exception) {
            onProgress("Error: ${e.message}")
            return false to SyncPhase.UNKNOWN
        } finally {
            try { nfc.close() } catch (_: Exception) {}
        }
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun internalTransfer(monster: StoredMonster, onProgress: (String) -> Unit): Boolean {
        try {
            Timber.d("internalTransfer started for ${monster.name}")
            
            val dataPayload = if (monster.rawPayload != null) {
                Timber.d("Using rawPayload for transfer")
                Base64.decode(monster.rawPayload, Base64.DEFAULT)
            } else {
                Timber.d("Formatting monster payload for transfer")
                formatMonsterPayload(monster)
            }
            
            if (dataPayload.size < 16) {
                Timber.e("Payload too small: ${dataPayload.size} bytes")
                return false
            }

            for (i in 0 until 4) {
                val block = 8 + i
                onProgress("Syncing ${i+1}/4...")
                val chunk = dataPayload.copyOfRange(i * 4, (i + 1) * 4)
                
                var success = false
                for (retry in 1..10) {
                    try {
                        Timber.d("Writing Page $block: ${chunk.toHexString()} (Attempt $retry)")
                        nfc.writePage(block, chunk)
                        success = true
                        break
                    } catch (e: Exception) {
                        Timber.w("Page $block write failed (Attempt $retry): ${e.message}")
                        Thread.sleep(100)
                    }
                }
                if (!success) return false
                Thread.sleep(200) 
            }

            try { 
                Timber.d("Writing Final Commit to Page 2")
                nfc.writePage(2, byteArrayOf(OP_COMMIT, 0x00, 0x00, 0x00)) 
                try { nfc.writePage(7, byteArrayOf(OP_COMMIT, 0x00, 0x00, 0x00)) } catch(_: Exception) {}
                Thread.sleep(500)
                nfc.writePage(2, byteArrayOf(OP_IDLE, 0x00, 0x00, 0x00))
            } catch (e: Exception) {
                Timber.w("Final commit failed: ${e.message}")
            }
            onProgress("Hero Synced!")
            return true
        } catch (e: Exception) {
            Timber.e(e, "internalTransfer exception")
            return false
        }
    }

    fun executeReceive(onProgress: (String) -> Unit): StoredMonster? {
        try {
            nfc.timeout = 15000
            onProgress("Connecting...")
            nfc.connect()

            val uid = tag.id
            val heroPass = calculateHeroPassword(uid)
            onProgress("Authenticating...")
            
            var authed = false
            val passwords = listOf(byteArrayOf(0x42, 0x4E, 0x44, 0x49), heroPass, calculateVbPassword(uid))
            for (p in passwords) {
                try { nfc.transceive(byteArrayOf(0x1B.toByte()) + p); authed = true; break } catch(_: Exception) {}
            }
            
            if (!authed) {
                 try { nfc.readPages(4); authed = true } catch(_: Exception) {}
            }
            
            Thread.sleep(300)

            // 1. Handshake
            onProgress("Linking Watch...")
            val read4 = try { nfc.readPages(4) } catch(e: Exception) { null }
            var watchDimId = 0
            if (read4 != null && read4.size >= 16) {
                watchDimId = read4[1].toInt() and 0xFF
                
                // Upload Handshake
                val authBlock = byteArrayOf(
                    STATUS_READY_FLAG, 
                    watchDimId.toByte(), 
                    CMD_UPLOAD, 
                    (STATUS_READY_FLAG.toInt() xor watchDimId xor CMD_UPLOAD.toInt()).toByte()
                )
                
                try {
                    nfc.writePage(2, byteArrayOf(OP_IDLE, 0x00, 0x00, 0x00))
                    nfc.writePage(6, authBlock)
                    
                    // Trigger Upload (0x02)
                    nfc.writePage(2, byteArrayOf(OP_UPLOAD, 0x00, 0x00, 0x00))
                    try { nfc.writePage(7, byteArrayOf(OP_UPLOAD, 0x00, 0x00, 0x00)) } catch(_: Exception) {}
                    
                    Thread.sleep(1500)
                } catch (e: Exception) {
                    Timber.e(e, "Receive handshake failed")
                }
            }

            // 2. Read Stats
            onProgress("Reading stats...")
            var statPayload: ByteArray? = null
            var retry = 60 // 18 seconds total
            while (statPayload == null && retry > 0) {
                try {
                    // Exhaustive search: Page 8, 12, 16, 20
                    val offsets = listOf(8, 12, 16, 20)
                    for (offset in offsets) {
                        val response = nfc.readPages(offset)
                        if (response != null && response.size >= 16) {
                            val candidate = response.copyOfRange(0, 16)
                            if (isMonsterDataValid(candidate)) {
                                statPayload = candidate
                                Timber.d("SUCCESS: Found Monster at Page $offset: ${statPayload.toHexString()}")
                                break
                            } else {
                                // Diagnostic log for every page we check
                                val raw = candidate.toHexString().take(8)
                                val clean = deobfuscate(candidate).toHexString().take(8)
                                Timber.d("Page $offset check: Raw=$raw Clean=$clean")
                            }
                        }
                    }
                    if (statPayload == null) {
                         // Pulse to keep alive
                         try { nfc.transceive(byteArrayOf(0x30.toByte(), 0x00.toByte())) } catch(_: Exception) {}
                    }
                } catch (e: Exception) {
                    Timber.w("Read attempt $retry error: ${e.message}")
                }
                retry--
                Thread.sleep(300)
            }

            if (statPayload == null) {
                onProgress("Stats Fail")
                return null
            }

            onProgress("Extracted!")
            val monster = parseMonsterPayload(statPayload, watchDimId)
            try { nfc.writePage(2, byteArrayOf(OP_IDLE, 0x00, 0x00, 0x00)) } catch (_: Exception) {}
            
            return monster.copy(rawPayload = Base64.encodeToString(statPayload, Base64.DEFAULT))
        } catch (e: Exception) {
            onProgress("Receive Failed")
            return null
        } finally {
            try { nfc.close() } catch (e: Exception) {}
        }
    }

    private fun parseMonsterPayload(payload: ByteArray, cardId: Int): StoredMonster {
        val clean = deobfuscate(payload)
        Timber.d("Deobfuscated Payload: ${clean.toHexString()}")
        
        val charId = clean[0].toInt() and 0xFF
        val stage = clean[1].toInt() and 0xFF
        val attribute = clean[2].toInt() and 0xFF
        val mood = clean[3].toInt() and 0xFF
        val vitals = clean.getUInt16(4, Endian.Big).toInt()
        val steps = clean.getUInt16(6, Endian.Big).toInt()
        val attack = clean.getUInt16(8, Endian.Big).toInt()
        val bp = clean.getUInt16(10, Endian.Big).toInt()
        val winRatio = clean[12].toInt() and 0xFF
        val trophies = clean.getUInt16(13, Endian.Big).toInt()
        
        return StoredMonster(
            name = if (cardId > 0) "HERO $cardId" else "DIM HERO",
            charId = charId,
            stage = stage,
            attack = attack,
            calories = vitals,
            speed = 100,
            defense = bp,
            currentWins = 0,
            winsRequired = 10,
            timeAlive = 0,
            evolutionTime = 3600,
            attribute = attribute,
            mood = mood,
            steps = steps,
            bp = bp,
            sp = 100,
            winRatio = winRatio,
            trophies = trophies
        )
    }
    
    private fun formatMonsterPayload(monster: StoredMonster): ByteArray {
        val payload = ByteArray(16)
        payload[0] = monster.charId.toByte()
        payload[1] = monster.stage.toByte()
        payload[2] = monster.attribute.toByte()
        payload[3] = monster.mood.toByte()
        
        val vitalsBytes = monster.calories.coerceIn(0, 9999).toUShort().toByteArray(Endian.Big)
        payload[4] = vitalsBytes[0]; payload[5] = vitalsBytes[1]
        
        val stepsBytes = monster.steps.coerceIn(0, 65535).toUShort().toByteArray(Endian.Big)
        payload[6] = stepsBytes[0]; payload[7] = stepsBytes[1]
        
        val atkBytes = monster.attack.coerceIn(0, 9999).toUShort().toByteArray(Endian.Big)
        payload[8] = atkBytes[0]; payload[9] = atkBytes[1]
        
        val bpBytes = monster.bp.coerceIn(0, 9999).toUShort().toByteArray(Endian.Big)
        payload[10] = bpBytes[0]; payload[11] = bpBytes[1]
        
        payload[12] = monster.winRatio.toByte()
        val trophiesBytes = monster.trophies.coerceIn(0, 65535).toUShort().toByteArray(Endian.Big)
        payload[13] = trophiesBytes[0]
        payload[14] = trophiesBytes[1]

        var checksum = 0
        for (i in 0 until 15) { checksum = checksum xor (payload[i].toInt() and 0xFF) }
        payload[15] = (checksum and 0xFF).toByte()
        return deobfuscate(payload)
    }

    private fun isMonsterDataValid(payload: ByteArray): Boolean {
        val clean = deobfuscate(payload)
        val charId = clean[0].toInt() and 0xFF
        val stage = clean[1].toInt() and 0xFF
        
        Timber.d("Validating Payload: Raw=${payload.toHexString().take(8)}... Deobfuscated: Char=$charId Stage=$stage")
        
        // If data is all 0 or all 0xFF, it's definitely not valid
        if (clean.all { it == 0.toByte() } || clean.all { it == 0xFF.toByte() }) return false
        
        // Allow a slightly wider range for custom DIMs/BEMs
        return charId < 255 && stage < 15
    }
}
