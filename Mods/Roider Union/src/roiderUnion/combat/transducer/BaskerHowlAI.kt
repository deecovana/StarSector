package roiderUnion.combat.transducer

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import org.lwjgl.util.vector.Vector2f

class BaskerHowlAI : ShipSystemAIScript {
    companion object {
        const val CHECKS_PER_FRAME = 10
        const val IN_RANGE_ID = "in_range"
        const val IN_RANGE_WEIGHT_PER_SECOND = 1f
        const val CUSTOM_TARGET_ID = "\$roider_howlTarget"
        const val FRIEND_ID = "friend"
        const val CLOSE_RANGE_MULT = 1.5f
    }

    lateinit var ship: ShipAPI
    lateinit var system: ShipSystemAPI
    lateinit var flags: ShipwideAIFlags
    lateinit var engine: CombatEngineAPI

    override fun init(ship: ShipAPI?, system: ShipSystemAPI?, flags: ShipwideAIFlags?, engine: CombatEngineAPI?) {
        if (Helper.anyNull(ship, system, flags, engine)) return
        this.ship = ship!!
        this.system = system!!
        this.flags = flags!!
        this.engine = engine!!
    }

    private val interval = IntervalUtil(0.08f, 0.12f)

    private val targets = mutableMapOf<ShipAPI, MutableStat>()
    private var preferredTarget: ShipAPI? = null

    private var index = 0

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        if (!this::ship.isInitialized) return
        val ships = engine.ships?.filterNot { it === ship || !it.isAlive || it.isExpired || it.isFighter } ?: return
        if (ships.isEmpty()) {
            index = 0
            return
        }
        targets.keys.filter { !ships.contains(it) }.forEach { targets.remove(it) }
        for (i in 0 until CHECKS_PER_FRAME.coerceAtMost(ships.size)) {
            if (index >= ships.size) index = 0
            val pTarget = ships[index] ?: continue
            val stat = targets.getOrElse(pTarget) { createNewTrackingStat(pTarget) }
            if (ship.owner == pTarget.owner) stat.modifyMult(FRIEND_ID, 0.5f)
            else stat.unmodifyMult(FRIEND_ID)
            stat.baseValue = pTarget.maxSpeed + BaskerHowlEffectManager.getSpeedBoost(pTarget)
            updateRangeWeight(pTarget, amount)
            if (preferredTarget == null || (targets[preferredTarget]?.modifiedValue ?: 0f) < stat.modifiedValue) preferredTarget = pTarget
            index++
        }
        interval.advance(amount)
        if (interval.intervalElapsed()) {
            if (preferredTarget != null
                && inRange(preferredTarget!!)
                && system.state == ShipSystemAPI.SystemState.IDLE
            ) {
                ship.setCustomData(CUSTOM_TARGET_ID, preferredTarget)
                ship.useSystem()
            }
        }
    }

    private fun createNewTrackingStat(target: ShipAPI): MutableStat {
        val result = MutableStat(target.maxSpeed)
        targets[target] = result
        return result
    }

    private fun updateRangeWeight(target: ShipAPI, amount: Float) {
        val stat = targets[target] ?: return
        val boost = stat.getFlatStatMod(IN_RANGE_ID)?.value ?: 0f
        if (inRange(target)) {
            stat.modifyFlat(IN_RANGE_ID, boost + IN_RANGE_WEIGHT_PER_SECOND * amount)
        } else if (boost > 0) {
            val newBoost = boost - IN_RANGE_WEIGHT_PER_SECOND * amount
            if (newBoost > 0) {
                stat.modifyFlat(IN_RANGE_ID, newBoost)
            } else {
                stat.unmodifyFlat(IN_RANGE_ID)
            }
        }
    }

    private fun inRange(target: ShipAPI): Boolean {
        return Misc.getDistance(ship.location, target.location) <= BaskerHowlScript.getMaxRange(ship) * CLOSE_RANGE_MULT
    }
}