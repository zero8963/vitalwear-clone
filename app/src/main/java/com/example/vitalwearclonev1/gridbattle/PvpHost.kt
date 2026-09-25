package com.example.vitalwearclonev1.gridbattle

/**
 * Host-side PvP glue for [GridBattleScreen] (2026-09-25).
 *
 * The host runs the authoritative [BattleEngine] with a [RemoteEnemyDriver];
 * the guest's inputs arrive as "IN|..." packets and are queued here, then
 * applied once per frame. Snapshots go out ~12x/sec.
 */
class PvpHostBinding(
    val net: PvpNetManager,
    val guestInfo: PvpFighterInfo
) {
    private val inputQueue = ArrayDeque<String>()

    /** The guest left or the connection dropped mid-fight. */
    var onPeerLeft: () -> Unit = {}

    /** The guest tapped REMATCH on their end. */
    var onRematchRequested: () -> Unit = {}

    fun newEngine(config: BattleConfig): BattleEngine =
        BattleEngine(config).apply {
            enemyDriver = RemoteEnemyDriver()
            pvpHostMode = true
            guestBusterElement = guestInfo.busterElement?.let { ChipElement.valueOf(it) }
            guestBusterAnimKey = guestInfo.busterAnimKey
            guestSwordAnimKey = guestInfo.swordAnimKey
            guestFxSmall = guestInfo.fxSmall
            guestFxBig = guestInfo.fxBig
        }

    fun attach() {
        net.listener = object : PvpNetManager.Listener {
            override fun onMessage(fromEndpoint: String, msg: String) {
                when {
                    msg.startsWith("IN|") -> synchronized(inputQueue) {
                        inputQueue.add(msg.removePrefix("IN|"))
                    }
                    msg == "REQ_REMATCH" -> onRematchRequested()
                    msg == "BYE" -> onPeerLeft()
                }
            }

            override fun onEndpointFound(endpointId: String, name: String) {}
            override fun onEndpointLost(endpointId: String) {}
            override fun onConnected(endpointId: String) {}
            override fun onDisconnected(endpointId: String) { onPeerLeft() }
        }
    }

    fun detach() {
        net.listener = null
    }

    fun drainInputs(): List<String> = synchronized(inputQueue) {
        val out = inputQueue.toList()
        inputQueue.clear()
        out
    }

    fun sendSnapshot(snap: NetSnapshot) = net.send(snap.toWire())
    fun sendEnd(winner: Int) = net.send("END|$winner")
    fun sendRestart() = net.send("RESTART")
    fun sendBye() = net.send("BYE")
}

/**
 * Applies one guest input packet to the host engine.
 * The guest's d-pad "right" (toward the host) is +1 in the guest's view,
 * which is -1 in host coordinates — hence the dx negation.
 */
fun applyPvpInput(
    engine: BattleEngine,
    msg: String,
    playFx: (animKey: String?, fxAttackId: Int?) -> Unit
) {
    try {
        when {
            msg.startsWith("m:") -> {
                val parts = msg.removePrefix("m:").split(",")
                engine.moveEnemy(-parts[0].toInt(), parts[1].toInt())
            }
            msg == "c:1" -> engine.setEnemyCharging(true)
            msg == "c:0" -> engine.setEnemyCharging(false)
            msg == "b" -> engine.releaseEnemyBuster()?.let { playFx(it.animKey, it.fxAttackId) }
            msg == "s" -> engine.enemySword()?.let { playFx(it.animKey, it.fxAttackId) }
            msg.startsWith("chip:") -> {
                val ids = msg.removePrefix("chip:")
                    .split(",").mapNotNull { it.toIntOrNull() }
                engine.enemyFireChips(ids.mapNotNull { ChipLibrary.byId(it) })
            }
        }
    } catch (e: Exception) {
        // Malformed packet — drop it, the next snapshot heals the guest.
    }
}
