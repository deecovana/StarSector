package roiderUnion.combat.entropyAmp

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper

class EntropyAmpLRInteractor(private val model: EntropyAmpLRModel) {
    companion object {
        const val ENTROPY_ID = "entropyamplifier effect"
        const val ENTROPY_TARGET = "_entropy_target_data"
        const val DAM_MULT = 1.5f
        private const val RANGE = 3000f
    }

    fun applyEntropy(
        id: String,
        state: State,
        effectLevel: Float,
        ship: ShipAPI,
        buildEntropyPlugin: (EntropyAmpLRStats.TargetData) -> EveryFrameCombatPlugin
    ) {
        var targetData = model.targetData

        if (state == State.IDLE && targetData != null) {
            clearTarget(ship)
            return
        }

        if (state == State.IN && targetData == null) {
            targetData = pickTarget(ship)
            model.canShowFloatyText = true
        }

        if (targetData == null) return

        targetData.currDamMult = 1f + (DAM_MULT - 1f) * effectLevel
        if (targetData.targetEffectPlugin == null) {
            targetData.targetEffectPlugin = buildEntropyPlugin(targetData)

            Helper.combatEngine?.addPlugin(targetData.targetEffectPlugin)
            Helper.combatEngine?.customData?.put(getTargetDataKey(ship), targetData)
        }
    }

    private fun clearTarget(ship: ShipAPI) {
        Global.getCombatEngine().customData.remove(getTargetDataKey(ship))
        model.targetData = null
    }

    private fun pickTarget(ship: ShipAPI): EntropyAmpLRStats.TargetData {
        val result = EntropyAmpLRStats.TargetData(ship, findTarget(ship))
        model.targetData = result
        return result
    }

    private fun findTarget(ship: ShipAPI): ShipAPI? {
        var target = ship.shipTarget
        val filter = Misc.FindShipFilter { s -> !isEntropied(s) }
        val range = getMaxRange(ship)
        val player = ship === Helper.combatEngine?.playerShip
        if (target != null) {
            target = checkTarget(ship, target, range)
        } else {
            if (player) {
                target = Misc.findClosestShipEnemyOf(
                    ship,
                    ship.mouseTarget,
                    ShipAPI.HullSize.FRIGATE,
                    range,
                    true,
                    filter
                )
            } else {
                val test = ship.aiFlags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET)
                if (test is ShipAPI) {
                    target = checkTarget(ship, test, range)
                }
            }

            if (target == null) {
                target = Misc.findClosestShipEnemyOf(
                    ship,
                    ship.location,
                    ShipAPI.HullSize.FRIGATE,
                    range,
                    true,
                    filter
                )
            }
        }

        if (target === ship) return null

        return checkTarget(ship, target, range)
    }

    private fun isEntropied(ship: ShipAPI?): Boolean {
        if (ship == null) return true
        return ship.mutableStats?.hullDamageTakenMult?.multMods?.containsKey(ENTROPY_ID) == true
//        return Helper.combatEngine?.customData?.containsKey(getTargetDataKey(ship)) == true
    }

    private fun getTargetDataKey(ship: ShipAPI): String = ship.id + ENTROPY_TARGET

    private fun checkTarget(ship: ShipAPI, target: ShipAPI?, range: Float) : ShipAPI? {
        if (target == null) return null
        if (isEntropied(target)) return null
        val dist = Misc.getDistance(ship.location, target.location)
        val radSum = ship.collisionRadius + target.collisionRadius
        if (dist > range + radSum) return null
        return target
    }

    private fun getMaxRange(ship: ShipAPI): Float {
        return ship.mutableStats.systemRangeBonus.computeEffective(RANGE)
    }

    fun entropyAdvance(targetData: EntropyAmpLRStats.TargetData) {
        val target = targetData.target ?: return
//        target.customData[ENTROPY_ID] = true

        if (targetData.currDamMult <= 1f || !targetData.ship.isAlive) {
            target.mutableStats.hullDamageTakenMult.unmodify(ENTROPY_ID)
            target.mutableStats.armorDamageTakenMult.unmodify(ENTROPY_ID)
            target.mutableStats.shieldDamageTakenMult.unmodify(ENTROPY_ID)
            target.mutableStats.empDamageTakenMult.unmodify(ENTROPY_ID)
            Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin)
        } else {
            target.mutableStats.hullDamageTakenMult.modifyMult(ENTROPY_ID, targetData.currDamMult)
            target.mutableStats.armorDamageTakenMult.modifyMult(ENTROPY_ID, targetData.currDamMult)
            target.mutableStats.shieldDamageTakenMult.modifyMult(ENTROPY_ID, targetData.currDamMult)
            target.mutableStats.empDamageTakenMult.modifyMult(ENTROPY_ID, targetData.currDamMult)
        }
    }

    fun confirmTargetInfo(ship: ShipAPI) {
        val target = findTarget(ship)
        model.isTargetInRange = target != null
        model.isTargetOutOfRange = target == null && ship.shipTarget != null
    }

    fun isSystemUsable(ship: ShipAPI): Boolean {
        val target = findTarget(ship)
        return target != null
    }

}
