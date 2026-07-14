package roiderUnion.combat.phasenet

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.CombatHelper
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

class PhasenetInteractor(private val model: PhasenetModel) {
    companion object {
        const val ASSIST_KEY = "roider_phasenetActive_"
        const val AI_TARGET = "\$roider_phasenetTarget"
        
        const val MAX_FORCE = 10000f
        private const val PEAK_FORCE_MULT = 2f

        // Restrict top grab speed for fighters and the like
        const val MAX_PULL_SPEED = 100000f
        private const val MAX_ACCEL = 80f
        private const val MIN_ACCEL = 20f
        private const val RANGE = 2000f

        val MIN_SPEEDS = mapOf(
            Pair(HullSize.CAPITAL_SHIP, 30f),
            Pair(HullSize.CRUISER, 40f),
            Pair(HullSize.DESTROYER, 50f),
            Pair(HullSize.FRIGATE, 100f),
            Pair(HullSize.FIGHTER, 150f)
        )

        fun getMinSpeed(hullSize: HullSize?): Float {
            return MIN_SPEEDS[hullSize] ?: MIN_SPEEDS[HullSize.CRUISER]!!
        }

        fun isTargetInRange(ship: CombatEntityAPI, target: CombatEntityAPI?): Boolean {
            val range = (ship as? ShipAPI)?.mutableStats?.systemRangeBonus?.computeEffective(RANGE) ?: RANGE
            return CombatHelper.isTargetInRange(ship, target, range)
        }

        fun getNormalizedMass(mass: Float): Float {
            if (MAX_FORCE / mass > MAX_PULL_SPEED / 2f) {
                return mass + (MAX_FORCE / (MAX_PULL_SPEED / 2f) - mass) / 2f
            } else if (MAX_FORCE / mass < MAX_PULL_SPEED / 5f) {
                return mass - (mass - MAX_FORCE / (MAX_PULL_SPEED / 5f)) / 2f
            }
            return mass
        }

        fun getFocusPoint(source: CombatEntityAPI, target: CombatEntityAPI): Vector2f {
            var radius: Float = source.collisionRadius + target.collisionRadius
            radius *= 1.1f
            val facing: Float = source.facing
            val rx = cos(Math.toRadians(facing.toDouble())).toFloat() * radius
            val ry = sin(Math.toRadians(facing.toDouble())).toFloat() * radius
            val focus = Vector2f(source.location)
            focus.x += rx
            focus.y += ry
            return focus
        }
    }

    /**
     * Interactor determines target when player is aiming
     * Interactor determines which direction target is being pulled
     */

    private val interval = IntervalUtil(0.01f, 0.01f)

    fun applyEffects(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val amount = Helper.combatEngine?.elapsedInLastFrame ?: return
        if (Helper.combatEngine?.isPaused == true) return
        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        val ship = stats.entity as? ShipAPI ?: return
        var target = model.target

        if (state == ShipSystemStatsScript.State.IN && target == null) {
            target = pickTarget(ship)
            model.target = target
        }
        if (target?.isExpired == true) target = null
        if (target == null) {
            ship.system.deactivate()
            return
        }
        if (!isTargetInRange(ship, target)) {
            ship.system.deactivate()
            return
        }

        val focus = getFocusPoint(ship, target)
        model.focusPoint = focus

        var angle: Float = Misc.getAngleInDegrees(focus, target.location)
        if (angle < 0) angle += 360f
        target.setCustomData(ASSIST_KEY + ship.id, angle)

        // Debug: show where focus point is
//        SpriteAPI sprite1 = Global.getSettings().getSprite(Roider_Categories.GRAPHICS_COMBAT, GLOW_1_SPRITE_ID);
//        MagicRender.singleframe(sprite1, focus, new Vector2f(sprite1.getWidth(), sprite1.getHeight()), ship.getFacing(), GLOW_1_COLOR, true);
        val forceMult = getForceMult(state, effectLevel)
        model.forceMult = forceMult
        applyForce(id, focus, ship, target, forceMult, interval.elapsed)
    }

    private fun pickTarget(ship: ShipAPI): CombatEntityAPI? {
        var pick: CombatEntityAPI? =
            if (ship === Helper.combatEngine?.playerShip && ship.ai == null) {
                ship.shipTarget
            } else {
                getClosestTargetToPoint(ship.aiFlags.getCustom(ShipwideAIFlags.AIFlags.SYSTEM_TARGET_COORDS) as? Vector2f)
            } ?: getClosestTargetToPoint(ship.mouseTarget) ?: return null

        if (pick is ShipAPI) {
            if (pick.isStation) {
                pick = null
            } else if (pick.isStationModule) {
                val parent = pick.parentStation ?: return null
                pick = if (parent !== ship && !parent.isStation && pick.stationSlot != null) {
                    parent
                } else {
                    null
                }
            }
            if (pick != null && PhasenetEffectManager.isNetted(pick as ShipAPI)) return null
        }
        if (pick === ship) pick = null
        return pick
    }

    private fun getClosestTargetToPoint(point: Vector2f?): CombatEntityAPI? {
        if (point == null) return null
        val entities = mutableListOf<CombatEntityAPI>()
        entities.addAll(Helper.combatEngine?.asteroids ?: listOf())
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

    private fun getForceMult(state: ShipSystemStatsScript.State, effectLevel: Float): Float {
        // Force spikes above max during activation
        if (state == ShipSystemStatsScript.State.IN) {
            val peakLevel = 0.666f
            // Rises fast during first 2/3
            return if (effectLevel < peakLevel) {
                val levelMult = 1f / peakLevel
                effectLevel * levelMult * PEAK_FORCE_MULT
            } else {
                // Reduce current level
                // Mult level so it reduces PEAK force from max to none
                val levelMult = (PEAK_FORCE_MULT - 1f) / (1f - peakLevel)
                val level = (effectLevel - peakLevel) * levelMult
                PEAK_FORCE_MULT - level
            }
        }

        // Otherwise it follows effectLevel
        return effectLevel
    }

    private fun applyForce(
        id: String, focus: Vector2f?, ship: ShipAPI,
        target: CombatEntityAPI, forceMult: Float, amount: Float
    ) {
        if (!isTargetInRange(ship, target)) return

        var targetSpeed: Float
//        targetSpeed = if (target.mass < 1) MAX_FORCE else MAX_FORCE / getNormalizedMass(target.mass)
        targetSpeed = if (target is ShipAPI) {
            (target.hullSpec?.engineSpec?.maxSpeed?.coerceAtLeast(getMinSpeed(target.hullSize)) ?: getMinSpeed(target.hullSize)) *
                    (log10(ship.mass + 1f) / log10(target.mass + 1f))
        } else {
            getMinSpeed(null) * (log10(ship.mass + 1f) / log10(target.mass + 1f))
        }
        val loc: Vector2f = target.location
        val focusDist: Float = Misc.getDistance(focus, ship.location)
        val shipDist: Float = Misc.getDistance(ship.location, loc)
        val tooClose = focusDist > shipDist
        val farAway = shipDist > focusDist * 2
        // With a tiny zone in-between
        val angleToSource: Float = Misc.getAngleInDegrees(ship.location, loc)

        // Give some leeway
        val wrongAngleClock: Boolean = angleToSource - 30 > ship.facing
        val wrongAngleCounter: Boolean = angleToSource + 30 < ship.facing
        val wrongAngle = wrongAngleClock || wrongAngleCounter
        var targetAngle: Float = Misc.getAngleInDegrees(loc, focus)
        var wrongAngleDegrees: Float = angleToSource - ship.facing
        if (wrongAngleDegrees < 0) wrongAngleDegrees += 360f

        // Target is far away
        if (farAway) {
//            if (acceleration > shipDist - focusDist) acceleration = shipDist - focusDist;
        } else if (tooClose && wrongAngle) {
            if (wrongAngleDegrees <= 180) targetAngle += 90 * (wrongAngleDegrees / 360) else targetAngle -= 90 * (wrongAngleDegrees / 360)
            //            if (acceleration > focusDist - shipDist) acceleration = focusDist - shipDist;
//            acceleration *= 2;
        } else if (tooClose) {
//            if (acceleration > focusDist - shipDist) acceleration = focusDist - shipDist;

//            acceleration *= 2;
        } else if (wrongAngle) {
            if (wrongAngleDegrees <= 180) targetAngle += 45 * (wrongAngleDegrees / 360) else targetAngle -= 45 * (wrongAngleDegrees / 360)
        }

        if (target is ShipAPI) {
            knockOutFighterEngines(target)
//            val noEngines = target.engineController.isFlamedOut || !target.isAlive
//            if (noEngines && targetSpeed > MAX_PULL_SPEED) {
//                targetSpeed = MAX_PULL_SPEED
//            } else if (targetSpeed > target.maxSpeed + MAX_PULL_SPEED) {
//                targetSpeed = target.maxSpeed + MAX_PULL_SPEED
//            }
        }
        val targetVector = Misc.getUnitVectorAtDegreeAngle(targetAngle)
        targetVector.scale(targetSpeed * forceMult)
        target.setCustomData(PhasenetEffectManager.KEY, true)
        target.setCustomData(PhasenetEffectManager.EFFECT_KEY + ship.id, targetVector)
        model.targetVector = targetVector

        val vel: Vector2f = target.velocity
        if (vel.lengthSquared() == 0f) return

        if (target !is ShipAPI) {
            loc.x += targetVector.x * forceMult * amount
            loc.y += targetVector.y * forceMult * amount
            negateVelocity(vel)
        }
    }

    private fun negateVelocity(vel: Vector2f) {
        if (vel.x > 0) vel.x -= 1f
        if (vel.x < 0) vel.x += 1f
        if (vel.y > 0) vel.y -= 1f
        if (vel.y < 0) vel.y += 1f
    }

    private fun knockOutFighterEngines(target: ShipAPI) {
        if (target.isFighter && target.mutableStats.engineDamageTakenMult.computeMultMod() > 0) {
            target.engineController.forceFlameout(true)
        }
    }

    fun unapply(stats: MutableShipStatsAPI, id: String) {
        val uid = if (stats.entity is ShipAPI) {
            val ship: ShipAPI = stats.entity as ShipAPI
            ASSIST_KEY + ship.id
        } else null
        if (model.target?.customData?.containsKey(uid) == true) model.target?.removeCustomData(id)
        model.target = null
    }

    fun isUsable(ship: ShipAPI): Boolean {
        if (ship.fluxTracker?.isOverloadedOrVenting == true) return false
        if (model.target != null) {
            val inRange = isTargetInRange(ship, model.target!!)
            if (!inRange) model.target = null
            return inRange
        }

        model.target = pickTarget(ship)
        if (model.target == null) return false
        if (!isTargetInRange(ship, model.target!!)) return false

        // Reset target
        model.target = null
        return true
    }

    fun selectInfoText(system: ShipSystemAPI, ship: ShipAPI) {
        val target = if (model.target == null) pickTarget(ship) else model.target
        model.infoState = if (system.isActive) PhasenetInfoState.ACTIVE
        else if (system.isCoolingDown) PhasenetInfoState.COOLDOWN
        else if (target != null && !isTargetInRange(ship, target)) PhasenetInfoState.OUT_OF_RANGE
        else if (isUsable(ship)) PhasenetInfoState.READY
        else PhasenetInfoState.NO_TARGET
    }

    fun setStatusData() {
        model.hullName = (model.target as? ShipAPI)?.hullSpec?.hullName ?: ExternalStrings.DEBUG_NULL
        model.statusSpeed = if (model.target != null) PhasenetAI.estimatePullSpeed(model.target!!).toInt() else 0
    }
}
