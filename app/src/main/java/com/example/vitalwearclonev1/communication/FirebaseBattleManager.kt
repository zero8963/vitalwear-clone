package com.example.vitalwearclonev1.communication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirebaseBattleManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var battleListener: ListenerRegistration? = null
    private var currentRoomId: String? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    suspend fun ensureAuthenticated(): String? {
        val user = auth.currentUser
        if (user != null) return user.uid
        return try {
            auth.signInAnonymously().await().user?.uid
        } catch (e: Exception) {
            Timber.e(e, "Firebase Auth failed")
            null
        }
    }

    suspend fun hostBattle(stats: Map<String, Any?>): String? {
        val uid = ensureAuthenticated() ?: return null
        val code = (100000..999999).random().toString()
        val roomId = code
        
        val roomData = mapOf(
            "host_uid" to uid,
            "host_stats" to stats,
            "status" to "WAITING",
            "seed" to System.currentTimeMillis(),
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        return try {
            db.collection("battle_rooms").document(roomId).set(roomData).await()
            currentRoomId = roomId
            roomId
        } catch (e: Exception) {
            Timber.e(e, "Error hosting battle")
            null
        }
    }

    suspend fun joinBattle(code: String, stats: Map<String, Any?>): Boolean {
        val uid = ensureAuthenticated() ?: return false
        val docRef = db.collection("battle_rooms").document(code)
        
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists() || snapshot.getString("status") != "WAITING") return false
            
            val update = mapOf(
                "guest_uid" to uid,
                "guest_stats" to stats,
                "status" to "READY"
            )
            docRef.update(update).await()
            currentRoomId = code
            true
        } catch (e: Exception) {
            Timber.e(e, "Error joining battle")
            false
        }
    }

    fun quickMatch(stats: Map<String, Any?>): Flow<BattleRoomUpdate> = callbackFlow {
        scope.launch {
            val uid = ensureAuthenticated() ?: run {
                close()
                return@launch
            }

            // Try to find an existing WAITING room
            val query = db.collection("battle_rooms")
                .whereEqualTo("status", "WAITING")
                .limit(1)
                .get()
                .await()

            if (!query.isEmpty) {
                val doc = query.documents[0]
                val roomId = doc.id
                val success = joinBattle(roomId, stats)
                if (success) {
                    // We joined an existing room
                    battleListener = db.collection("battle_rooms").document(roomId).addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        snapshot?.let { trySend(BattleRoomUpdate.fromSnapshot(it)) }
                    }
                } else {
                    hostAndListen(this@callbackFlow, stats)
                }
            } else {
                hostAndListen(this@callbackFlow, stats)
            }
        }
        awaitClose { battleListener?.remove() }
    }

    private fun hostAndListen(producer: kotlinx.coroutines.channels.ProducerScope<BattleRoomUpdate>, stats: Map<String, Any?>) {
        scope.launch {
            val roomId = hostBattle(stats)
            if (roomId != null) {
                battleListener = db.collection("battle_rooms").document(roomId).addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { producer.trySend(BattleRoomUpdate.fromSnapshot(it)) }
                }
            } else {
                producer.close()
            }
        }
    }

    fun observeBattle(roomId: String): Flow<BattleRoomUpdate> = callbackFlow {
        val registration = db.collection("battle_rooms").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(BattleRoomUpdate.fromSnapshot(it)) }
            }
        awaitClose { registration.remove() }
    }

    fun leaveBattle() {
        battleListener?.remove()
        battleListener = null
        currentRoomId = null
    }
}

data class BattleRoomUpdate(
    val status: String,
    val hostStats: Map<String, Any?>?,
    val guestStats: Map<String, Any?>?,
    val seed: Long,
    val hostUid: String?,
    val guestUid: String?
) {
    companion object {
        fun fromSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot): BattleRoomUpdate {
            return BattleRoomUpdate(
                status = snapshot.getString("status") ?: "UNKNOWN",
                hostStats = snapshot.get("host_stats") as? Map<String, Any?>,
                guestStats = snapshot.get("guest_stats") as? Map<String, Any?>,
                seed = snapshot.getLong("seed") ?: 0L,
                hostUid = snapshot.getString("host_uid"),
                guestUid = snapshot.getString("guest_uid")
            )
        }
    }
}
