package com.example.vitalwearclonev1.communication

import android.content.Intent
import com.example.vitalwearclonev1.card.CardManager
import com.example.vitalwearclonev1.common.communication.ChannelTypes
import com.example.vitalwearclonev1.monster.MonsterManager
import com.github.cfogrady.vb.dim.card.BemCard
import com.github.cfogrady.vb.dim.card.DimReader
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.ChannelClient.Channel
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.charset.Charset

import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import java.io.ByteArrayInputStream

class DataTransferService : WearableListenerService() {

    private val scope = CoroutineScope( SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Timber.d("Message received in background: ${messageEvent.path}")
        when (messageEvent.path) {
            "/HEALTH_SYNC" -> {
                try {
                    val data = messageEvent.data ?: return
                    val dis = DataInputStream(ByteArrayInputStream(data))
                    val steps = dis.readLong()
                    val calories = dis.readInt()
                    val hasWorkout = dis.readBoolean()
                    val startOfDay = try { dis.readLong() } catch (e: Exception) { 0L }
                    val weightKg = try { dis.readFloat() } catch (e: Exception) { 75f }
                    
                    Timber.i("Background received HEALTH_SYNC: Steps=$steps, Cals=$calories, WeightKg=$weightKg")
                    
                    val intent = Intent("com.example.vitalwearclonev1.HEALTH_SYNCED").apply {
                        putExtra("steps", steps)
                        putExtra("calories", calories)
                        putExtra("hasWorkout", hasWorkout)
                        putExtra("startOfDay", startOfDay)
                        putExtra("weight", weightKg)
                        setPackage(packageName)
                    }
                    sendBroadcast(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing health sync in background")
                }
            }
            "/WORKOUT_SESSION" -> {
                try {
                    val data = messageEvent.data ?: return
                    val dis = DataInputStream(ByteArrayInputStream(data))
                    val routineName = readString(dis)
                    val calories = dis.readInt()
                    
                    Timber.i("Background received WORKOUT_SESSION: $routineName, $calories kcal")
                    
                    val monsterManager = MonsterManager(this@DataTransferService)
                    // Calculate a multiplier based on calories (Base 150 = 1.0)
                    val multiplier = (calories.toFloat() / 150f).coerceIn(0.1f, 1.5f)
                    monsterManager.addTrainingBonus(routineName, multiplier)
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(this@DataTransferService, "Phone Workout: $routineName Complete!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    
                    val intent = Intent("com.example.vitalwearclonev1.WORKOUT_COMPLETE").apply {
                        putExtra("routine", routineName)
                        setPackage(packageName)
                    }
                    sendBroadcast(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing workout session in background")
                }
            }
            "/BATTLE" -> {
                val battleIntent = Intent("com.example.vitalwearclonev1.TRIGGER_BATTLE").apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                sendBroadcast(battleIntent)
                launchMainActivity(battleIntent)
            }
            "/P2P_BATTLE_REQ", "/P2P_BATTLE_ACK" -> {
                try {
                    val data = messageEvent.data ?: return
                    val dis = DataInputStream(ByteArrayInputStream(data))
                    val cardName = readString(dis)
                    val charId = dis.readInt()
                    val atk = dis.readInt()
                    val hp = dis.readInt()
                    val spd = dis.readInt()
                    val def = dis.readInt()
                    val seed = try { dis.readLong() } catch (e: Exception) { System.currentTimeMillis() }
                    val isBem = try { dis.readByte().toInt() == 1 } catch (e: Exception) { false }
                    val dimId = try { dis.readInt() } catch (e: Exception) { 0 }

                    Timber.d("Background P2P Battle message: $cardName, path: ${messageEvent.path}")

                    if (messageEvent.path == "/P2P_BATTLE_REQ") {
                        // Send ACK back to phone
                        handleP2PRequest(seed)
                    }

                    val battleIntent = Intent("com.example.vitalwearclonev1.TRIGGER_P2P_BATTLE").apply {
                        putExtra("cardName", cardName)
                        putExtra("charId", charId)
                        putExtra("atk", atk)
                        putExtra("hp", hp)
                        putExtra("spd", spd)
                        putExtra("def", def)
                        putExtra("seed", seed)
                        putExtra("isInitiator", messageEvent.path == "/P2P_BATTLE_ACK")
                        putExtra("isBem", isBem)
                        putExtra("dimId", dimId)
                        setPackage(packageName)
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    sendBroadcast(battleIntent)
                    launchMainActivity(battleIntent)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing P2P battle in background")
                }
            }
        }
    }

    private fun launchMainActivity(battleIntent: Intent? = null) {
        val intent = Intent(this, com.example.vitalwearclonev1.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (battleIntent != null) {
                putExtras(battleIntent)
                action = battleIntent.action
            }
        }
        startActivity(intent)
    }

    private fun handleP2PRequest(seed: Long) {
        scope.launch {
            try {
                val monsterManager = MonsterManager(this@DataTransferService)
                val cardManager = CardManager(this@DataTransferService)
                val myState = monsterManager.getCurrentMonster()
                if (myState != null) {
                    val myCard = cardManager.getCard(myState.cardName)
                    val dimIdAck = myCard?.header?.dimId ?: 0
                    val isBemLocal = myCard is BemCard
                    val nodes = Wearable.getNodeClient(this@DataTransferService).connectedNodes.await()
                    val messageClient = Wearable.getMessageClient(this@DataTransferService)
                    val payload = ByteArrayOutputStream().use { bos ->
                        val dos = DataOutputStream(bos)
                        dos.write(myState.cardName.toByteArray(Charset.defaultCharset()))
                        dos.writeByte(0)
                        dos.writeInt(myState.characterId)
                        dos.writeInt(myState.baseAp + myState.attackBonus)
                        dos.writeInt(myState.baseHp + myState.healthBonus)
                        dos.writeInt(myState.speedBonus)
                        dos.writeInt(myState.defenseBonus)
                        dos.writeLong(seed)
                        dos.writeByte(if (isBemLocal) 1 else 0)
                        dos.writeInt(dimIdAck)
                        dos.flush()
                        bos.toByteArray()
                    }
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, "/P2P_BATTLE_ACK", payload).await()
                    }
                    Timber.d("Sent P2P ACK back to phone from background")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending P2P ACK from background")
            }
        }
    }

    override fun onChannelOpened(channel: Channel) {
        super.onChannelOpened(channel)
        when (channel.path) {
            ChannelTypes.CARD_DATA -> {
                scope.launch {
                    handleCardData(channel)
                }
            }
            ChannelTypes.CHARACTER_DATA -> {
                scope.launch {
                    handleCharacterRestore(channel)
                }
            }
        }
    }

    private suspend fun handleCharacterRestore(channel: Channel) {
        val channelClient = Wearable.getChannelClient(this)
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
                val attr = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val mood = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val steps = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val bp = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val sp = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val winRatio = try { dataStream.readInt() } catch (e: Exception) { 0 }
                val trophies = try { dataStream.readInt() } catch (e: Exception) { 0 }

                Timber.d("Restoring character from phone: $cardName")
                val monsterManager = MonsterManager(this@DataTransferService)
                monsterManager.restoreMonster(cardName, charId, stage, atk, calories, spd, def,
                    attr, mood, steps, bp, sp, winRatio, trophies)
                
                // We should probably update restoreMonster to accept these new fields too
                val prefs = getSharedPreferences("monster_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt("current_wins", wins)
                    .putLong("current_time_alive", timeAlive)
                    .putLong("current_evolution_time", evolutionTime)
                    .apply()

                delay(200) // Safety for storage write
                
                // Notify UI
                val intent = Intent("com.example.vitalwearclonev1.CARD_IMPORTED").apply {
                    putExtra("cardName", cardName)
                    putExtra("timestamp", System.currentTimeMillis())
                    putExtra("isRestore", true)
                }
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restoring character from channel")
        } finally {
            channelClient.close(channel).await()
        }
    }

    private suspend fun handleCardData(channel: Channel) {
        val channelClient = Wearable.getChannelClient(this)
        try {
            channelClient.getInputStream(channel).await().use { inputStream ->
                val dataStream = DataInputStream(inputStream)
                val cardName = readString(dataStream)
                val uniqueSprites = dataStream.readByte().toInt() == 1
                val convertToBem = dataStream.readByte().toInt() == 1
                val payloadSize = dataStream.readInt()
                
                Timber.d("Receiving card: $cardName, unique: $uniqueSprites, size: $payloadSize")
                
                val card = DimReader().readCard(dataStream, convertToBem)
                Timber.d("Card loaded successfully: ${card.header.dimId}")
                
                // Save the card
                val cardManager = CardManager(this@DataTransferService)
                cardManager.saveCard(cardName, card)
                
                // Always set as current monster when a new card is imported
                val monsterManager = MonsterManager(this@DataTransferService)
                monsterManager.setCurrentMonster(cardName, 0)
                
                // Notify the app with a timestamp to force refresh
                val intent = Intent("com.example.vitalwearclonev1.CARD_IMPORTED").apply {
                    putExtra("cardName", cardName)
                    putExtra("timestamp", System.currentTimeMillis())
                }
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading card data from channel")
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