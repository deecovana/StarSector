package roiderUnion.combat.interdictor

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipEngineControllerAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.Misc
import kotlin.math.min

class InterdictorInteractor(private val model: InterdictorModel) {
    companion object {
        const val WING_EFFECT_RANGE = 200f
        const val RANGE = 800f
        const val INTERDICT_SPEED_MULT = 0.1f
        const val ID = "roider_interdictor"
        private const val ENGINE_MULT_ID = "engine_damage_penalty"
    }

    fun applyInterdiction(ship: ShipAPI, state: ShipSystemStatsScript.State, effectLevel: Float) {
        var targetData = model.mainTarget

        if (state == ShipSystemStatsScript.State.IDLE && targetData != null) {
            model.mainTarget = null
            model.targets.clear()
            return
        }

        if (state == ShipSystemStatsScript.State.IN && targetData == null) {
            val target = findTarget(ship)
            model.mainTarget = target
            targetData = target
        }

        if (effectLevel == 0f || targetData == null) return

        model.targets.clear()
        if (targetData.isFighter || targetData.isDrone) {
            val engine = Global.getCombatEngine()
            for (other in engine.ships) {
                if (other.isShuttlePod) continue
                if (other.isHulk) continue
                if (!other.isDrone && !other.isFighter) continue
                if (other.originalOwner != targetData.originalOwner) continue
                val dist = Misc.getDistance(other.location, targetData.location)
                if (dist > WING_EFFECT_RANGE) continue
                model.targets.add(other)
            }
        } else {
            model.targets.add(targetData)
        }

        var first = true
        for (target in model.targets) {
            if (state == ShipSystemStatsScript.State.IN && effectLevel < 1) {
                target.mutableStats.maxSpeed.modifyMult(ID, min(INTERDICT_SPEED_MULT / effectLevel, 1f))
            }
            if (effectLevel >= 1) {
                target.mutableStats.maxSpeed.unmodifyMult(ID)
                if (first) {
                    first = false
                    model.canShowFloaty = true
                    val ec = target.engineController
                    var limit = ec.flameoutFraction
                    if (ship.isDrone || ship.isFighter) {
                        limit = 1f
                    }
                    disableEngines(ship, ec, limit)
                } else {
                    val ec = target.engineController
                    val limit = 1f
                    disableEngines(ship, ec, limit)
                }
            }
            if (state == ShipSystemStatsScript.State.OUT && effectLevel < 1) {
                val targetMult = 1f - effectLevel
                if (getEngineMult(target) <= targetMult) reenableEngine(target.engineController)
                val speedMult = getSpeedMult(getEngineMult(target), targetMult)
                target.mutableStats.maxSpeed.modifyMult(ID, speedMult)
                target.mutableStats.acceleration.modifyMult(ID, speedMult)
            }
            if (state == ShipSystemStatsScript.State.COOLDOWN) {
                target.mutableStats.maxSpeed.unmodifyMult(ID)
                target.mutableStats.acceleration.unmodifyMult(ID)
            }
        }
    }

    private fun findTarget(ship: ShipAPI): ShipAPI? {
        val filter = Misc.FindShipFilter { s -> !isFlamedOut(s) }
        val range = getMaxRange(ship)
        val player = ship === Global.getCombatEngine().playerShip
        var target: ShipAPI? = ship.shipTarget
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
                    ship.mouseTarget,
                    ShipAPI.HullSize.FRIGATE,
                    range,
                    true,
                    filter
                )
            }
        }

        if (target == null) {
            target = Misc.findClosestShipEnemyOf(
                ship,
                ship.location,
                ShipAPI.HullSize.FRIGATE,
                range,
                true,
                filter)
        }

        if (target === ship) return null

        target = checkTarget(ship, target, range)

        return target
    }

    private fun checkTarget(ship: ShipAPI, target: ShipAPI?, range: Float) : ShipAPI? {
        if (target == null) return null
        if (ship.owner == target.owner) return null
        val dist = Misc.getDistance(ship.location, target.location)
        val radSum = ship.collisionRadius + target.collisionRadius
        if (dist > range + radSum) return null
        if (isFlamedOut(target)) return null
        return target
    }

    private fun isFlamedOut(ship: ShipAPI?): Boolean = ship?.engineController?.isFlamedOut == true
            || ship?.engineController?.isFlamingOut == true

    private fun getMaxRange(ship: ShipAPI): Float = ship.mutableStats.systemRangeBonus.computeEffective(RANGE)

    private fun disableEngines(ship: ShipAPI, ec: ShipEngineControllerAPI, limit: Float) {
        var disabledSoFar = 0f
        var disabledAnEngine = false
        val engines = ec.shipEngines?.toMutableList() ?: return
        engines.shuffle()
        for (engine in engines) {
            if (engine == null) continue
            if (engine.isDisabled) continue
            val contrib = engine.contribution
            if (disabledSoFar + contrib <= limit) {
                engine.disable()
                disabledSoFar += contrib
                disabledAnEngine = true
            }
        }
        if (!disabledAnEngine) {
            for (engine in engines) {
                if (engine == null) continue
                if (engine.isDisabled) continue
                engine.disable()
                break
            }
        }
        ec.computeEffectiveStats(ship === Global.getCombatEngine().playerShip)
    }

    private fun reenableEngine(ec: ShipEngineControllerAPI) {
        if (ec.isFlamingOut) return
        val engines = ec.shipEngines?.filter { it.isDisabled }?.toMutableList() ?: return
        if (engines.isEmpty()) return
        engines.shuffle()
        engines.first().repair()
    }

    private fun getEngineMult(ship: ShipAPI?): Float {
        return ship?.mutableStats?.acceleration?.multMods?.get(ENGINE_MULT_ID)?.value ?: 1f
    }

    private fun getSpeedMult(engineMult: Float, targetMult: Float): Float {
        if (engineMult == 0f) return 1f
        return targetMult / engineMult
    }

    fun isUsable(system: ShipSystemAPI, ship: ShipAPI): Boolean {
        if (system.isActive) return false
        val target = findTarget(ship)
        return target != null && target !== ship

    }

    fun confirmTargetInfo(system: ShipSystemAPI, ship: ShipAPI) {
        model.infoState = if (system.isOutOfAmmo || system.state != ShipSystemAPI.SystemState.IDLE) InterdictorInfoState.DEFAULT
            else if (findTarget(ship) != null) InterdictorInfoState.READY
            else if (ship.shipTarget != null) InterdictorInfoState.OUT_OF_RANGE
            else InterdictorInfoState.NO_TARGET
    }
}