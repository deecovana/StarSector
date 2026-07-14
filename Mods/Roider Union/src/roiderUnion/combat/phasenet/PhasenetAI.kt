package roiderUnion.combat.phasenet

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import kotlin.math.log10

/**
 * Author: SafariJohn
 */
class PhasenetAI : ShipSystemAIScript {
    companion object {
        const val DO_NOTHING_WEIGHT = 0f
        const val COLLIDER_WEIGHT_MULT = 1f
        const val WRECKING_WEIGHT_MULT = 1f
        const val SLOW_WEIGHT_MULT = 1f
        const val SHIP_SHIELD_WEIGHT_MULT = 1f
        const val SHIELD_WEIGHT_MULT = 1f
        const val ASSIST_WEIGHT_MULT = 2f
        fun estimatePullSpeed(target: CombatEntityAPI): Float {
            if (true) return if (target is ShipAPI) {
                val maxSpeed = target.hullSpec?.engineSpec?.maxSpeed?.coerceAtLeast(PhasenetInteractor.getMinSpeed(target.hullSize))
                (maxSpeed ?: PhasenetInteractor.getMinSpeed(target.hullSize)) * (log10(1650f) / log10(target.mass + 1f))
            } else {
                PhasenetInteractor.getMinSpeed(null) * (log10(1650f) / log10(target.mass))
            }
            var speed: Float =
                PhasenetInteractor.MAX_FORCE / PhasenetInteractor.getNormalizedMass(target.mass)
            var noEngines = target !is ShipAPI
            if (!noEngines) {
                val tShip = target as ShipAPI
                noEngines = (tShip.isFighter || tShip.engineController.isFlamedOut
                        || !tShip.isAlive)
                if (noEngines && speed > PhasenetInteractor.MAX_PULL_SPEED) {
                    speed = PhasenetInteractor.MAX_PULL_SPEED
                } else if (speed > tShip.maxSpeed + PhasenetInteractor.MAX_PULL_SPEED) {
                    speed = tShip.maxSpeed + PhasenetInteractor.MAX_PULL_SPEED
                }
            }
            return speed
        }
    }

    private var ship: ShipAPI? = null
    private var engine: CombatEngineAPI? = null
    private var flags: ShipwideAIFlags? = null
    private var system: ShipSystemAPI? = null
    private var stats: PhasenetController? = null
    private val tracker = IntervalUtil(0.5f, 1f)

    override fun init(
        ship: ShipAPI, system: ShipSystemAPI,
        flags: ShipwideAIFlags, engine: CombatEngineAPI
    ) {
        this.ship = ship
        this.flags = flags
        this.engine = engine
        this.system = system
        stats = system.specAPI.statsScript as PhasenetController
    }

    override fun advance(
        amount: Float, missileDangerDir: Vector2f?,
        collisionDangerDir: Vector2f?, target: ShipAPI?
    ) {
        tracker.advance(amount)
        if (tracker.intervalElapsed()) {
            // Check if should activate system
            if (system!!.state == SystemState.IDLE) {
                // Fail fast if nothing in range
                if (!isUsable) return
                pickResultIdle(isRetreating)

                // Retreating logic
                // Choices:
                // use weak enemy as shield
                // assist allied Phasenet
                // use hulk or asteroid as shield
                // do nothing


                // Attacking logic
                // Choices:
                // pull small enemy into another enemy
                // grab enemy to beat up
                // assist allied Phasenet
                // slow down retreating enemy
                // do nothing
            } else if (system!!.state == SystemState.ACTIVE) {
                pickResultActive(isRetreating)
            }
        }
    }

    private fun pickResultActive(retreating: Boolean) {
        if (retreating) {
            ship!!.system?.deactivate()
        }
    }

    private val isRetreating: Boolean
        get() = isRetreating(ship)

    private fun isRetreating(s: ShipAPI?): Boolean {
        return s!!.aiFlags.hasFlag(AIFlags.RUN_QUICKLY)
    }

    private val isUsable: Boolean
        get() {
            if (ship!!.fluxTracker.isOverloadedOrVenting) return false
            var pick: CombatEntityAPI? = ship!!.shipTarget
            if (!PhasenetInteractor.isTargetInRange(ship!!, pick)) pick = null
            if (pick == null) {
                val entities: MutableList<CombatEntityAPI> = ArrayList()
                entities.addAll(engine!!.asteroids)
                entities.addAll(engine!!.ships)
                for (entity in entities) {
                    if (entity === ship) continue
                    if (PhasenetInteractor.isTargetInRange(entity, ship)) {
                        pick = entity
                        if (pick is ShipAPI) {
                            val tShip = pick
                            if (tShip.isStation) {
                                pick = null
                            } else if (tShip.isStationModule) {
                                val parent = tShip.parentStation
                                pick = if (parent !== ship && !parent.isStation && tShip.stationSlot != null) {
                                    parent
                                } else {
                                    null
                                }
                            }
                        }
                        if (pick != null) break
                    }
                }
            }
            if (pick === ship) pick = null
            return pick != null
        }

    private fun pickResultIdle(retreating: Boolean) {
        var count = 6f
        if (retreating) count = 4f

        // Attacking choices
        val bestCollideOdds = 0f
        var bestCollider: CombatEntityAPI? = null
        var bestWreckingOdds = 0f
        var bestWreckingTarget: ShipAPI? = null
        var bestSlowOdds = 0f
        var bestSlowTarget: ShipAPI? = null
        var bestRescueOdds = 0f
        var bestRescueTarget: ShipAPI? = null
        // Retreating choices
        var bestShipShieldOdds = 0f
        var bestShipShieldTarget: ShipAPI? = null
        var bestShieldOdds = 0f
        var bestShieldTarget: ShipAPI? = null
        // Common choices
        var bestAssistOdds = 0f
        var bestAssistTarget: ShipAPI? = null
        var doNothingOdds = DO_NOTHING_WEIGHT
        val entities: MutableList<CombatEntityAPI> = ArrayList()
        entities.addAll(engine!!.asteroids)
        entities.addAll(engine!!.ships)
        for (entity in entities) {
            if (entity === ship) continue
            if (!PhasenetInteractor.isTargetInRange(entity, ship)) continue
            if (entity is ShipAPI) {
                var target: ShipAPI = entity
                if (target.isStation) {
                    continue
                } else if (target.isStationModule) {
                    val parent = target.parentStation
                    if (parent === ship || parent.isStation
                        || target.stationSlot == null
                    ) {
                        continue
                    }
                    target = parent
                }
                if (target.isFighter) {
                    if (target.wing?.spec == null) continue
                    if (target.mutableStats.engineDamageTakenMult.modifiedValue <= 0) continue
                }
                var odds: Float
                odds = getAssistOdds(target)
                if (odds > bestAssistOdds) {
                    bestAssistOdds = odds
                    bestAssistTarget = target
                }
                if (retreating) {
                    odds = getShipShieldOdds(target)
                    if (odds > bestShipShieldOdds) {
                        bestShipShieldOdds = odds
                        bestShipShieldTarget = target
                    }
                    odds = getShieldOdds(target)
                    if (odds > bestShieldOdds) {
                        bestShieldOdds = odds
                        bestShieldTarget = target
                    }
                } else {
                    odds = getWreckingOdds(target)
                    if (odds > bestWreckingOdds) {
                        bestWreckingOdds = odds
                        bestWreckingTarget = target
                    }
                    odds = getSlowOdds(target)
                    if (odds > bestSlowOdds) {
                        bestSlowOdds = odds
                        bestSlowTarget = target
                    }
                    odds = getRescueOdds(target)
                    if (odds > bestRescueOdds) {
                        bestRescueOdds = odds
                        bestRescueTarget = target
                    }
                }
            }
        }

        // Doctor the results to improve consistency
        // by removing unlikely choices
        var avg = (bestCollideOdds + bestWreckingOdds
                + bestAssistOdds + bestRescueOdds
                + bestShipShieldOdds + bestShieldOdds
                + doNothingOdds)
        avg /= count
        if (bestCollideOdds < avg) {
            bestCollider = null
        }
        if (bestWreckingOdds < avg) {
            bestWreckingTarget = null
        }
        if (bestSlowOdds < avg) {
            bestSlowTarget = null
        }
        if (bestRescueOdds < avg) {
            bestRescueTarget = null
        }
        if (bestShipShieldOdds < avg) {
            bestShipShieldTarget = null
        }
        if (bestShieldOdds < avg) {
            bestShieldTarget = null
        }
        if (bestAssistOdds < avg) {
            bestAssistTarget = null
        }
        if (doNothingOdds < avg) {
            doNothingOdds = Float.MIN_VALUE
        }

        // Randomly pick one of the results
        val picker = WeightedRandomPicker<CombatEntityAPI?>()
        if (bestCollider != null) picker.add(bestCollider, bestCollideOdds)
        if (bestWreckingTarget != null) picker.add(bestWreckingTarget, bestWreckingOdds)
        if (bestSlowTarget != null) picker.add(bestSlowTarget, bestSlowOdds)
        if (bestRescueTarget != null) picker.add(bestRescueTarget, bestRescueOdds)
        if (bestShipShieldTarget != null) picker.add(bestShipShieldTarget, bestShipShieldOdds)
        if (bestShieldTarget != null) picker.add(bestShieldTarget, bestShieldOdds)
        if (bestAssistTarget != null) picker.add(bestAssistTarget, bestAssistOdds)
        picker.add(null, doNothingOdds)
        val pick = picker.pick()
        if (pick != null) {
            ship!!.aiFlags.setFlag(AIFlags.SYSTEM_TARGET_COORDS, 0.1f, pick.location)
            if (stats!!.isUsable(system, ship!!)) {
                ship!!.useSystem()
            }
        }
    }

    private fun getWreckingOdds(target: ShipAPI?): Float {
        if (target == null) return 0f
        if (ship!!.owner == target.owner) return 0f
        if (!target.isAlive) return 0f
        if (PhasenetEffectManager.isNetted(target)) return 0f

        // Only target fighters that are worth killing
        if (target.isFighter) {
            val spec = target.wing.spec
            val cooldown = system!!.specAPI.getCooldown(ship!!.mutableStats)
            if (spec.refitTime / 2f < cooldown) return 0f
        }
        var odds = 1f / estimatePullTime(target)
        if (target.isFighter) odds /= 2f
        if (isRetreating(target)) odds *= 2f
        if (target === ship!!.shipTarget) odds *= 2f
        return odds * WRECKING_WEIGHT_MULT
    }

    private fun getSlowOdds(target: ShipAPI?): Float {
        if (true) return 0f
        if (target == null) return 0f
        if (ship!!.owner == target.owner) return 0f
        if (!target.isAlive) return 0f
        if (PhasenetEffectManager.isNetted(target)) return 0f
        return 0f
    }

    private fun getRescueOdds(target: ShipAPI?): Float {
        if (true) return 0f
        if (target == null) return 0f
        if (ship!!.owner != target.owner) return 0f
        if (!target.isAlive) return 0f
        if (PhasenetEffectManager.isNetted(target)) return 0f
        return 0f
    }

    private fun getShipShieldOdds(target: ShipAPI?): Float {
        if (true) return 0f
        if (target == null) return 0f
        if (ship!!.owner == target.owner) return 0f
        if (!target.isAlive) return 0f
        if (PhasenetEffectManager.isNetted(target)) return 0f
        if (estimatePullTime(target) > (system?.chargeActiveDur ?: 0f) / 2f) return 0f

        // Only target fighters that are worth killing
        if (target.isFighter) {
            val spec = target.wing.spec
            if (spec.refitTime / 2f < system!!.specAPI.getCooldown(ship!!.mutableStats)) return 0f
        }
        return 0f
    }

    private fun getShieldOdds(target: ShipAPI?): Float {
        if (true) return 0f
        if (target == null) return 0f
        if (ship!!.owner == target.owner) return 0f
        if (!target.isAlive) return 0f
        if (PhasenetEffectManager.isNetted(target)) return 0f
        if (estimatePullTime(target) > (system?.chargeActiveDur ?: 0f) / 2f) return 0f

        // Only target fighters that are worth killing
        if (target.isFighter) {
            val spec = target.wing.spec
            if (spec.refitTime / 2f < system!!.specAPI.getCooldown(ship!!.mutableStats)) return 0f
        }
        return 0f
    }

    private fun getAssistOdds(target: ShipAPI?): Float {
        if (true) return 0f
        if (target == null) return 0f
        if (ship!!.owner == target.owner) return 0f
        if (!target.isAlive) return 0f
        val custom = target.customData
        var phasenetsActive = 0
        var avgPullAngle = 0f
        for (key in custom.keys) {
            if (key.startsWith(PhasenetInteractor.ASSIST_KEY)) {
                phasenetsActive++
                avgPullAngle += custom[key] as Float
            }
        }

        // No one to assist
        if (phasenetsActive == 0) return 0f

        // Target is already overwhelmed
        if (estimatePullSpeed(target) * phasenetsActive / getTargetsSpeed(target) > 1f) return 0f

        // Confirm angle is +- 60 degrees
        avgPullAngle /= phasenetsActive.toFloat()
        var currentPullAngle = Misc.getAngleInDegrees(PhasenetInteractor.getFocusPoint(ship!!, target), target.location)
        if (currentPullAngle < 0) currentPullAngle += 360f
        if (currentPullAngle > avgPullAngle + 60f) return 0f
        if (currentPullAngle < avgPullAngle - 60f) return 0f

        // Only target fighters that are worth killing
        if (target.isFighter) {
            val spec = target.wing.spec
            if (spec.refitTime / 2f < system!!.specAPI.getCooldown(ship!!.mutableStats)) return 0f
        }
        var odds = 1f / estimatePullTime(target)
        if (target.isFighter) odds /= 2f
        if (isRetreating(target)) odds *= 2f
        if (target === ship!!.shipTarget) odds *= 2f
        return odds * ASSIST_WEIGHT_MULT
    }

    private fun getTargetsSpeed(target: CombatEntityAPI): Float {
        if (target !is ShipAPI) return 0f
        val invalid = target.isFighter || target.engineController.isFlamedOut || !target.isAlive
        return if (invalid) 0f
        else target.maxSpeed
    }

    private fun estimatePullTime(target: CombatEntityAPI): Float {
        val speed = estimatePullSpeed(target)
        return Misc.getDistance(target.location, PhasenetInteractor.getFocusPoint(ship!!, target)) / speed
    }
}