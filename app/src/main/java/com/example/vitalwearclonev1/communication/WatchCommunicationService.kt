package com.example.vitalwearclonev1.communication

import com.example.vitalwearclonev1.common.communication.ChannelTypes
import com.example.vitalwearclonev1.lab.LabStorage
import com.example.vitalwearclonev1.lab.StoredMonster
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.DataInputStream
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Listens for incoming channels from the watch.
 */
class WatchCommunicationService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)
        when(channel.path) {
            ChannelTypes.LOGS_DATA -> {
                Timber.i("Watch is sending logs. Path: ${channel.path}")
            }
            ChannelTypes.CHARACTER_DATA -> {
                scope.launch {
                    handleCharacterData(channel)
                }
            }
            else -> {
                Timber.i("Unknown channel from watch: ${channel.path}")
            }
        }
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        super.onMessageReceived(messageEvent)
        Timber.d("Message received from watch: ${messageEvent.path}")
        if (messageEvent.path == "/REQUEST_HEALTH_SYNC") {
            scope.launch {
                val healthSyncManager = PhoneHealthSyncManager(applicationContext)
                if (healthSyncManager.hasAllPermissions()) {
                    healthSyncManager.syncNow()
                } else {
                    Timber.w("Watch requested health sync but permissions are missing")
                }
            }
        }
    }

    private suspend fun handleCharacterData(channel: ChannelClient.Channel) {
        val channelClient = Wearable.getChannelClient(applicationContext)
        try {
            channelClient.getInputStream(channel).await().use { inputStream ->
                val dataStream = DataInputStream(inputStream)
                val cardName = readString(dataStream)
                val charId = dataStream.readInt()
                val stage = dataStream.readInt()
                val atk = dataStream.readInt()
                val calories = dataStream.readInt()
                val spd = dataStream.readInt()
                val def = dataStream.readInt()
                val wins = dataStream.readInt()
                val winsReq = dataStream.readInt()
                val timeAlive = dataStream.readLong()
                val evolutionTime = dataStream.readLong()
                val attribute = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val mood = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val steps = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val bp = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val sp = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val winRatio = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val trophies = try { dataStream.readInt() } catch (e: Exception) { 0 }

                Timber.d("Received character from watch: $cardName (ID:$charId, Stage:$stage)")
                LabStorage.addMonster(applicationContext, StoredMonster(
                    cardName, charId, stage, atk, calories, spd, def, 
                    currentWins = wins, winsRequired = winsReq, 
                    timeAlive = timeAlive, evolutionTime = evolutionTime,
                    attribute = attribute, mood = mood, steps = steps, bp = bp, sp = sp, winRatio = winRatio, trophies = trophies
                ))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading character data from watch")
        } finally {
            channelClient.close(channel).await()
        }
    }

    private fun readString(inputStream: InputStream): String {
        val bytes = mutableListOf<Byte>()
        var b = inputStream.read()
        while (b != 0 && b != -1) {
            bytes.add(b.toByte())
            b = inputStream.read()
        }
        return String(bytes.toByteArray(), Charset.defaultCharset())
    }
}
