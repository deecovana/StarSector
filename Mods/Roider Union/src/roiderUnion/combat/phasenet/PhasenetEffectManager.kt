package roiderUnion.combat.phasenet

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Settings

/**
 * Author: SafariJohn
 */
class PhasenetEffectManager : BaseEveryFrameCombatPlugin() {
    companion object {
        const val KEY = "roider_phasenetTarget"
        const val EFFECT_KEY = "roider_phasenetEffect_"
        const val ONE_STEP = 5f

        const val TOKEN_NUM = "\$numPhasenets"
        const val TOKEN_STRENGTH = "\$pullStrength"

        fun isNetted(target: ShipAPI): Boolean {
            return target.customData.keys.contains(KEY)
        }
    }

    private val trueVelocities = mutableMapOf<String, Vector2f>()
    private val prevVelocities = mutableMapOf<String, Vector2f>()
    private val prevAccelerations = mutableMapOf<String, Vector2f>()
    private val baseMaxSpeeds = mutableMapOf<String, Float>()

    @Deprecated("Deprecated in Java")
    override fun init(engine: CombatEngineAPI) {
        trueVelocities.clear()
        prevVelocities.clear()
        prevAccelerations.clear()
        baseMaxSpeeds.clear()
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (Helper.combatEngine?.isCombatOver == true) return

        for (ship in Helper.combatEngine?.ships ?: listOf()) {
            if (ship == null) continue
            val targetVectors = getTargetVectors(ship)
            val targetVectorSum = calculateTargetVectorSum(targetVectors)
            if (targetVectorSum == null) { clearData(ship); continue }
            moveTarget(ship, targetVectorSum)
            displayPlayerStatus(ship, targetVectorSum.length().toInt(), targetVectors.size)
        }
    }

    private fun moveTarget(ship: ShipAPI, targetVectorSum: Vector2f) {
        val baseMaxSpeed = getBaseMaxSpeed(ship)
        val velocity = if (ship.velocity != null) Vector2f(ship.velocity) else return
        val trueVelocity = calculateTrueVelocity(ship, VectorUtils.getFacing(targetVectorSum), baseMaxSpeed)
        val targetVelocity = getTargetVelocity(trueVelocity, targetVectorSum)
        val acceleration = calcPhasenetAcceleration(targetVelocity, velocity)
        accelerateTarget(ship, acceleration)
        val newBaseMaxSpeed = adjustTargetTopSpeed(ship, targetVelocity.length())
        updateSavedValues(ship, velocity, acceleration, trueVelocity, newBaseMaxSpeed)
    }

    private fun displayPlayerStatus(ship: ShipAPI, pullStrength: Int, numPhasenets: Int) {
        if (ship === Helper.combatEngine?.playerShip) {
            val sprite = Settings.PHASENET_ICON_NAME
            if (numPhasenets > 1) {
                val text = ExternalStrings.PHASENET_PULL_MULTI
                    .replace(TOKEN_STRENGTH, pullStrength.toString())
                    .replace(TOKEN_NUM, numPhasenets.toString())
                Helper.combatEngine?.maintainStatusForPlayerShip(
                    KEY,
                    sprite,
                    ExternalStrings.PHASENET_TITLE,
                    text,
                    true
                )
            } else {
                val text = ExternalStrings.PHASENET_PULL_SINGLE
                    .replace(TOKEN_STRENGTH, pullStrength.toString())
                Helper.combatEngine?.maintainStatusForPlayerShip(
                    KEY,
                    sprite,
                    ExternalStrings.PHASENET_TITLE,
                    text,
                    true
                )
            }
        }
    }

    private fun getTargetVelocity(trueVelocity: Vector2f, targetVectorSum: Vector2f): Vector2f {
        return Vector2f.add(trueVelocity, targetVectorSum, null)
    }

    private fun calculateTrueVelocity(ship: ShipAPI, targetVectorFacing: Float, baseMaxSpeed: Float): Vector2f {
        var result = getTrueVelocity(ship)
        val acceleration = getAcceleration(ship)
        val prevPhasenetAccel = getPrevPhasenetAcceleration(ship)
        result += acceleration - prevPhasenetAccel

        val notAccelerating = Vector2f.sub(acceleration, prevPhasenetAccel, null).length() == 0f && result.length() < baseMaxSpeed
        if (notAccelerating) {
            val oneStep = Misc.getUnitVectorAtDegreeAngle(targetVectorFacing)
            oneStep.scale(ONE_STEP)
            result += oneStep
        }

        val noControl = (ship.engineController.isFlamedOut || !ship.isAlive) && result.length() < baseMaxSpeed
        if (noControl) {
            val oneStep = Misc.getUnitVectorAtDegreeAngle(targetVectorFacing)
            oneStep.scale(ONE_STEP)
            result += oneStep
        }

        if (result.length() > baseMaxSpeed) {
            result.scale(baseMaxSpeed / result.length())
        }
        return result
    }

    private fun accelerateTarget(ship: ShipAPI, acceleration: Vector2f) {
        ship.velocity.x += acceleration.x
        ship.velocity.y += acceleration.y
    }

    private fun adjustTargetTopSpeed(ship: ShipAPI, targetSpeed: Float): Float {
        val baseMaxSpeed = getBaseMaxSpeed(ship)
        return if (ship.velocity.length() > baseMaxSpeed
            && targetSpeed > baseMaxSpeed
        ) {
            ship.mutableStats.maxSpeed.unmodify(KEY)
            val newBaseMaxSpeed = ship.mutableStats.maxSpeed.modifiedValue
//            ship.mutableStats.maxSpeed.modifyMult(KEY, (targetSpeed / newBaseMaxSpeed) * 2f)
//            val mod = targetSpeed / newBaseMaxSpeed
            val mod = (targetSpeed / newBaseMaxSpeed) * 2
            ship.mutableStats.maxSpeed.modifyMult(KEY, mod)
            newBaseMaxSpeed
        } else {
            ship.mutableStats.maxSpeed.unmodify(KEY)
            ship.mutableStats.maxSpeed.modifiedValue
        }
    }

    private fun calcPhasenetAcceleration(targetVelocity: Vector2f, currentVelocity: Vector2f): Vector2f {
        return Vector2f.sub(targetVelocity, currentVelocity, null)
    }

    private fun getBaseMaxSpeed(ship: ShipAPI): Float {
        var result = baseMaxSpeeds[ship.id]
        if (result == null) {
            result = ship.maxSpeed
            baseMaxSpeeds[ship.id] = result
        }
        if (ship.engineController?.isFlamedOut == true || !ship.isAlive) {
            result = ship.hullSpec.engineSpec.maxSpeed
        }
        return result
    }

    private fun calculateTargetVectorSum(targetVectors: Set<Vector2f>): Vector2f? {
        if (targetVectors.isEmpty()) return null
        val result = Vector2f()
        for (v in targetVectors) {
            result.x += v.x
            result.y += v.y
        }
        return result
    }

    private fun updateSavedValues(
        ship: ShipAPI,
        velocity: Vector2f,
        acceleration: Vector2f,
        trueVelocity: Vector2f,
        newBaseMaxSpeed: Float
    ) {
        prevVelocities[ship.id] = velocity
        prevAccelerations[ship.id] = acceleration
        trueVelocities[ship.id] = trueVelocity
        baseMaxSpeeds[ship.id] = newBaseMaxSpeed
    }

    private fun clearData(ship: ShipAPI) {
        ship.customData?.remove(KEY)
        trueVelocities.remove(ship.id)
        prevVelocities.remove(ship.id)
        prevAccelerations.remove(ship.id)
        baseMaxSpeeds.remove(ship.id)
        ship.mutableStats?.maxSpeed?.unmodify(KEY)
    }

    private fun getTargetVectors(ship: ShipAPI): Set<Vector2f> {
        val customCopy: Map<String, Any> = ship.customData.toMap()
        if (!customCopy.containsKey(KEY)) return emptySet()

        val result = customCopy
            .filter { it.key.startsWith(EFFECT_KEY) && it.value is Vector2f }
            .map { it.value as Vector2f }
            .toSet()
        customCopy.keys.filter { it.startsWith(EFFECT_KEY) }.forEach { ship.removeCustomData(it) }
        return result
    }

    private fun getAcceleration(ship: ShipAPI): Vector2f {
        var prevVelocity: Vector2f? = prevVelocities[ship.id]
        if (prevVelocity == null) {
            prevVelocity = Vector2f(ship.velocity)
            prevVelocities[ship.id] = prevVelocity
        }
        return Vector2f.sub(ship.velocity, prevVelocity, null)
    }

    private fun getTrueVelocity(ship: ShipAPI): Vector2f {
        var trueVelocity: Vector2f? = trueVelocities[ship.id]
        if (trueVelocity == null) {
            trueVelocity = Vector2f(ship.velocity)
            trueVelocities[ship.id] = trueVelocity
        }
        return trueVelocity
    }

    private fun getPrevPhasenetAcceleration(ship: ShipAPI): Vector2f = prevAccelerations[ship.id] ?: Vector2f()
}