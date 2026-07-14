package roiderUnion.combat.transducer

import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.CombatHelper
import roiderUnion.helpers.Helper

class BaskerHowlScript : BaseShipSystemScript() {
    companion object {
        const val RANGE = 1000f
        fun getMaxRange(ship: ShipAPI): Float = ship.mutableStats.systemRangeBonus.computeEffective(RANGE)
    }

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {
        if (Helper.anyNull(stats, id)) return
        val source = stats!!.entity as? ShipAPI ?: return
        if (state == ShipSystemStatsScript.State.IN && BaskerHowlEffectManager.queue.none { it.first == source }) {
            val target = pickTarget(source) as? ShipAPI ?: return
            BaskerHowlEffectManager.queue.addLast(Pair(source, target))
        }
    }

    override fun isUsable(system: ShipSystemAPI?, ship: ShipAPI?): Boolean {
        if (ship == null || system == null) return false
        if (ship.fluxTracker?.isOverloadedOrVenting == true) return false

        val target = pickTarget(ship) ?: return false
        if (!isTargetInRange(ship, target)) return false
        return system.state == ShipSystemAPI.SystemState.IDLE
    }

    private fun isTargetInRange(source: CombatEntityAPI, target: CombatEntityAPI?): Boolean {
        val ship = source as? ShipAPI ?: return false
        val range = getMaxRange(ship)
        return CombatHelper.isTargetInRange(ship, target, range)
    }

    private fun pickTarget(ship: ShipAPI): CombatEntityAPI? {
        var pick: CombatEntityAPI? =
            if (ship === Helper.combatEngine?.playerShip && ship.ai == null) {
                ship.shipTarget
            } else {
                ship.customData?.get(BaskerHowlAI.CUSTOM_TARGET_ID) as? CombatEntityAPI
            } ?: getClosestTargetToPoint(ship.mouseTarget) ?: return null
        if (pick?.isExpired == true) return null
        if (pick is ShipAPI) {
            if (pick.isFighter) return null
            if (!pick.isAlive) return null
            val tShip: ShipAPI = pick
            if (tShip.isStation) {
                pick = null
            } else if (tShip.isStationModule) {
                val parent: ShipAPI = tShip.parentStation
                pick = if (parent !== ship && !parent.isStation && tShip.stationSlot != null) {
                    parent
                } else {
                    null
                }
            }
        }
        if (pick === ship) pick = null
        return pick
    }

    private fun getClosestTargetToPoint(point: Vector2f?): CombatEntityAPI? {
        if (point == null) return null
        val entities = mutableListOf<CombatEntityAPI>()
        entities.addAll(Helper.combatEngine?.ships ?: listOf())
        var closest: CombatEntityAPI? = null
        var dist = Short.MAX_VALUE.toFloat()
        for (entity in entities) {
            val prox: Float = Misc.getDistance(entity.location, point)
            if (prox <= entity.collisionRadius * 3 && prox < dist) {
                closest = entity
                dist = prox
            }
        }
        return closest
    }
}

