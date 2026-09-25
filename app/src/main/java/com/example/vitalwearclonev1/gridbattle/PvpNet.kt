package com.example.vitalwearclonev1.gridbattle

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Phone-to-phone Grid Battle PvP over WiFi (2026-09-25).
 *
 * No server, no Firebase, no fees: Google's Nearby Connections finds the
 * other phone directly (WiFi when both are on it, WiFi-Direct/Bluetooth
 * otherwise). One phone hosts and runs the battle engine; the guest just
 * plays — the host applies the guest's inputs and streams snapshots back.
 *
 * Wire protocol (UTF-8 strings):
 *   Guest -> Host: "HELLO|<PvpFighterInfo json>"
 *   Host -> Guest: "WELCOME|<PvpFighterInfo json>", "START", "SNAP|<NetSnapshot json>",
 *                  "END|<winner 0=host,1=guest>", "RESTART", "BYE"
 *   Guest -> Host: "IN|<input>" where input is m:dx,dy | c:1 | c:0 | b | s | chip:id,id
 *                  "REQ_REMATCH", "BYE"
 */

/** Nearby service id. Both phones must run this build. */
const val PVP_SERVICE_ID = "com.example.vitalwearclonev1.GRID_PVP"

private val pvpJson = Json { ignoreUnknownKeys = true }

/** Everything the other phone needs to field your fighter. */
@Serializable
data class PvpFighterInfo(
    val name: String,
    val cardName: String,
    val charId: Int,
    val maxHp: Int,
    val atkStat: Int,
    val effAtkMult: Float,
    val chargeRate: Float,
    val busterElement: String?,
    val busterAnimKey: String?,
    val swordAnimKey: String?,
    val fxSmall: Int?,
    val fxBig: Int?,
    val deck: List<Int>
)

@Serializable
data class NetProjectile(
    val x: Float,
    val y: Int,
    val vx: Float,
    val damage: Float,
    val fromPlayer: Boolean,
    val element: String?,
    val big: Boolean,
    val piercing: Boolean
)

@Serializable
data class NetMine(
    val x: Int,
    val y: Int,
    val timer: Float,
    val damage: Float,
    val fromPlayer: Boolean
)

/** Host-side attack animation event, so the guest can play the host's FX. */
@Serializable
data class NetFxEvent(
    val kind: String, // "buster" | "sword"
    val animKey: String?,
    val element: String?,
    val charged: Boolean,
    val fxId: Int?
)

/** Authoritative battle state, host -> guest ~12x/sec. */
@Serializable
data class NetSnapshot(
    val px: Int,
    val py: Int,
    val ex: Int,
    val ey: Int,
    val playerHp: Float,
    val enemyHp: Float,
    val projs: List<NetProjectile>,
    val mines: List<NetMine>,
    val time: Float,
    val winner: Int?,
    val lastPlayerHitAt: Float,
    val lastEnemyHitAt: Float,
    val enemyCharge: Float,
    val enemyCharging: Boolean,
    val fx: List<NetFxEvent>
)

fun PvpFighterInfo.toWire(): String = "HELLO|" + pvpJson.encodeToString(this)
/** Host -> guest handshake reply. */
fun PvpFighterInfo.toWelcomeWire(): String = "WELCOME|" + pvpJson.encodeToString(this)
fun parseFighterInfo(wire: String): PvpFighterInfo =
    pvpJson.decodeFromString(PvpFighterInfo.serializer(), wire.substringAfter("|"))

fun parseSnapshot(wire: String): NetSnapshot =
    pvpJson.decodeFromString(NetSnapshot.serializer(), wire.substringAfter("|"))

/** Host engine -> wire snapshot (drained FX events included). */
fun BattleEngine.netSnapshot(): NetSnapshot {
    val s = snapshot()
    return NetSnapshot(
        px = s.px, py = s.py, ex = s.ex, ey = s.ey,
        playerHp = s.playerHp, enemyHp = s.enemyHp,
        projs = s.projectiles.map {
            NetProjectile(it.x, it.y, it.vx, it.damage, it.fromPlayer, it.element?.name, it.big, it.piercing)
        },
        mines = s.mines.map { NetMine(it.x, it.y, it.timer, it.damage, it.fromPlayer) },
        time = s.time, winner = s.winner,
        lastPlayerHitAt = s.lastPlayerHitAt, lastEnemyHitAt = s.lastEnemyHitAt,
        enemyCharge = enemyCharge, enemyCharging = enemyCharging,
        fx = drainFxEvents()
    )
}

fun NetSnapshot.toWire(): String = "SNAP|" + pvpJson.encodeToString(this)

/**
 * Guest view: the guest sees themselves on the left, so the host's
 * coordinates are mirrored and player/enemy perspectives are swapped.
 */
fun NetSnapshot.toGuestView(guestMaxHp: Int, hostMaxHp: Int): BattleSnapshot = BattleSnapshot(
    px = 5 - ex, py = ey,
    ex = 5 - px, ey = py,
    playerHp = enemyHp, playerMaxHp = guestMaxHp.toFloat(),
    enemyHp = playerHp, enemyMaxHp = hostMaxHp.toFloat(),
    projectiles = projs.map {
        SimProjectile(
            6f - it.x, it.y, -it.vx, it.damage, !it.fromPlayer,
            it.element?.let { n -> ChipElement.valueOf(n) }, it.big, it.piercing
        )
    },
    mines = mines.map { SimMine(5 - it.x, it.y, it.timer, it.damage, !it.fromPlayer) },
    gauge = 0f, charge = 0f, charging = false,
    hand = emptyList(), selected = emptySet(),
    paused = false,
    winner = winner?.let { if (it == 0) 1 else 0 },
    lastPlayerHitAt = lastEnemyHitAt, lastEnemyHitAt = lastPlayerHitAt,
    time = time, canCustom = false
)

/** Host's engine config: we are the player, the guest is the enemy. */
fun PvpFighterInfo.hostConfig(guest: PvpFighterInfo): BattleConfig = BattleConfig(
    playerName = name,
    playerMaxHp = maxHp.coerceAtLeast(50),
    atkStat = atkStat,
    effAtkMult = effAtkMult,
    busterElement = busterElement?.let { ChipElement.valueOf(it) },
    busterAnimKey = busterAnimKey,
    swordAnimKey = swordAnimKey,
    playerDeck = deck,
    fxAttackIds = if (fxSmall != null && fxBig != null) fxSmall to fxBig else null,
    enemyName = guest.name,
    enemyMaxHp = guest.maxHp.coerceAtLeast(50),
    enemyAtk = guest.atkStat,
    enemyEffAtkMult = guest.effAtkMult,
    enemyChargeRate = guest.chargeRate,
    chargeRate = chargeRate
)

/**
 * Builds our PvP fighter card from the active Digimon's loadout + folder.
 * Returns null when the chip folder isn't battle-ready.
 */
fun buildLocalFighterInfo(
    context: Context,
    ownerId: String,
    monster: PhoneMonsterManager.MonsterState
): PvpFighterInfo? {
    val loadout = NaviCustLoadout.load(context, ownerId)
        .let { if (it.validate().errors.isEmpty()) it else NaviCustLoadout(emptyList()) }
    val folder = ChipFolder.load(context, ownerId)
    if (folder.validate().isNotEmpty()) return null
    val bonuses = loadout.totalBonuses()
    val style = loadout.style()
    val busterOv = loadout.busterOverride()
    val swordOv = loadout.swordOverride()
    val fxOverride = AttackFxOverrides.load(context, ownerId)
    val fxIds = fxOverride?.let {
        try {
            PhoneMonsterManager(context).getAttackIds(it.cardName, it.charId)
        } catch (e: Exception) { null }
    }
    val displayName = monster.nickname?.takeIf { it.isNotBlank() } ?: monster.cardName
    return PvpFighterInfo(
        name = displayName,
        cardName = monster.cardName,
        charId = monster.characterId,
        maxHp = monster.baseHp + monster.healthBonus + bonuses.maxHpBonus - loadout.glitchPenaltyHp(),
        atkStat = monster.baseAp + monster.attackBonus,
        effAtkMult = (1f + bonuses.attackPct / 100f) * (if (style == BattleStyle.BLAZE) 1.1f else 1f),
        chargeRate = 1f + bonuses.chargePct / 100f,
        busterElement = busterOv?.element?.name,
        busterAnimKey = busterOv?.animKey,
        swordAnimKey = swordOv?.animKey,
        fxSmall = fxIds?.first,
        fxBig = fxIds?.second,
        deck = folder.chipIds
    )
}

/**
 * Thin wrapper around Nearby Connections: advertise / discover / connect /
 * send raw string messages. All listener callbacks land on the main thread.
 */
class PvpNetManager(context: Context) {

    interface Listener {
        fun onMessage(fromEndpoint: String, msg: String)
        fun onEndpointFound(endpointId: String, name: String)
        fun onEndpointLost(endpointId: String)
        fun onConnected(endpointId: String)
        fun onDisconnected(endpointId: String)
        /** startAdvertising/startDiscovery failed — usually Bluetooth/WiFi/Location off. */
        fun onRadioError(msg: String) {}
    }

    var listener: Listener? = null

    private val client: ConnectionsClient =
        Nearby.getConnectionsClient(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private val endpoints = mutableSetOf<String>()

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCb)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                endpoints.add(endpointId)
                main.post { listener?.onConnected(endpointId) }
            }
        }

        override fun onDisconnected(endpointId: String) {
            endpoints.remove(endpointId)
            main.post { listener?.onDisconnected(endpointId) }
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            main.post { listener?.onEndpointFound(endpointId, info.endpointName) }
        }

        override fun onEndpointLost(endpointId: String) {
            main.post { listener?.onEndpointLost(endpointId) }
        }
    }

    private val payloadCb = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val msg = bytes.toString(Charsets.UTF_8)
                main.post { listener?.onMessage(endpointId, msg) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    fun startAdvertising(name: String) {
        client.startAdvertising(
            name, PVP_SERVICE_ID, lifecycle,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        ).addOnFailureListener { e ->
            main.post { listener?.onRadioError("Couldn't start hosting: ${e.message}") }
        }
    }

    fun stopAdvertising() = client.stopAdvertising()

    fun startDiscovery() {
        client.startDiscovery(
            PVP_SERVICE_ID, discovery,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        ).addOnFailureListener { e ->
            main.post { listener?.onRadioError("Couldn't start scanning: ${e.message}") }
        }
    }

    fun stopDiscovery() = client.stopDiscovery()

    fun requestConnection(endpointId: String, name: String) {
        client.requestConnection(name, endpointId, lifecycle)
    }

    fun send(msg: String) {
        val p = Payload.fromBytes(msg.toByteArray(Charsets.UTF_8))
        endpoints.toList().forEach { client.sendPayload(it, p) }
    }

    /** Full teardown: stop radio work and drop every connection. */
    fun stopAll() {
        client.stopAdvertising()
        client.stopDiscovery()
        endpoints.toList().forEach { client.disconnectFromEndpoint(it) }
        endpoints.clear()
    }
}
