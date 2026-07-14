package roiderUnion.combat.transducer

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState
import com.fs.starfarer.api.input.InputEventAPI
import roiderUnion.combat.interdictor.InterdictorStats
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import java.awt.Color
import kotlin.math.log10

class BaskerHowlEffectManager : BaseEveryFrameCombatPlugin() {
    companion object {
        const val SOURCE_ID = "roider_baskerHowl"
        const val SPEED_STEAL_MULT = 0.33f
        const val MAX_SPEED_BOOST = 30f
        const val MIN_SPEED_BOOST = 30f
        const val IN_DURATION = 1f
        const val ACTIVE_DURATION = 3f
        const val OUT_DURATION = 5f

        val ENGINE_SHIFT: Color = Color.RED

        const val START_SOUND = "roider_system_transducer_activate"
        const val LOOP_SOUND = "roider_system_transducer_loop"
        const val OUT_SOUND = "roider_system_transducer_end"

        const val ADD_TOKEN = "[SPEED BOOST]"
        const val AFFLICT_TOKEN = "[SPEED PENALTY]"

        fun getSpeedBoost(target: ShipAPI?): Float {
            if (target == null) return 0f
            if (true) return MAX_SPEED_BOOST
            return (target.maxSpeed * SPEED_STEAL_MULT * log10(target.mass.coerceAtLeast(1f)))
                .coerceAtLeast(MIN_SPEED_BOOST).coerceAtMost(MAX_SPEED_BOOST)
        }

        @JvmField
        val queue = ArrayDeque<Pair<ShipAPI, ShipAPI>>()
    }

    private class Data(val source: ShipAPI, val target: ShipAPI, var effectLevel: Float, var state: SystemState) {
        var activeDuration = ACTIVE_DURATION
        val speedBoost = getSpeedBoost(target)
    }

    private val active = mutableListOf<Data>()

    private fun getSourceId(source: ShipAPI): String = SOURCE_ID + source.id

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        initEffects()
        advanceActiveEffects(amount)
    }

    private fun initEffects() {
        while (queue.isNotEmpty()) {
            val pair = queue.removeFirst()
            val source = pair.first
            val target = pair.second
            active.removeAll { it.source == source }
            active += Data(source, target, 0f, SystemState.IN)
            Helper.soundPlayer?.playSound(START_SOUND, 1f, 0.7f, source.location, source.velocity)
            Helper.soundPlayer?.playSound(START_SOUND, 1f, 0.5f, target.location, target.velocity)
        }
    }

    private fun advanceActiveEffects(amount: Float) {
        if (Helper.combatEngine?.isPaused == true) return
        for (data in active) {
            applyEffects(data.source, data.target, data.speedBoost, data.effectLevel)
            showEffects(data.source, data.effectLevel)
            showEffects(data.target, data.effectLevel)
            showUI(data)
            val advanceAmount = getAdvanceAmount(amount, data.target)
            if (data.state == SystemState.IN) {
                data.effectLevel = (data.effectLevel + advanceAmount / IN_DURATION).coerceAtMost(1f)
                if (data.effectLevel >= 1f) data.state = SystemState.ACTIVE
            } else if (data.state == SystemState.ACTIVE) {
                data.activeDuration -= advanceAmount
                if (data.activeDuration <= 0f) data.state = SystemState.OUT
            } else {
                if (data.effectLevel >= 1f) playOutSounds(data)
                data.effectLevel -= advanceAmount / OUT_DURATION
            }
        }
        active.removeAll { it.effectLevel <= 0f }
    }

    private fun showUI(data: Data) {
        if (Helper.combatEngine?.playerShip === data.source) {
            val speedBoost = MAX_SPEED_BOOST * data.effectLevel
            Helper.combatEngine?.maintainStatusForPlayerShip(
                getSourceId(data.source),
                data.source.system.specAPI.iconSpriteName,
                data.source.system.displayName,
                ExternalStrings.INTERDICTOR_TRANSDUCER_ADD.replace(ADD_TOKEN, "$speedBoost"),
                false
            )
        } else if (Helper.combatEngine?.playerShip === data.target) {
            val penalty = (SPEED_STEAL_MULT * 100f).toInt()
            Helper.combatEngine?.maintainStatusForPlayerShip(
                getSourceId(data.source),
                data.source.system.specAPI.iconSpriteName,
                data.source.system.displayName,
                ExternalStrings.INTERDICTOR_TRANSDUCER_AFFLICT.replace(AFFLICT_TOKEN, "$penalty"),
                true
            )
        }
    }

    private fun showEffects(ship: ShipAPI, effectLevel: Float) {
        ship.setJitter(this, InterdictorStats.EFFECT_COLOR, effectLevel, 3, 0f, 10f * effectLevel)
        ship.setJitterUnder(this, InterdictorStats.EFFECT_COLOR, effectLevel, 25, 0f, 7f + 10f * effectLevel)
        ship.engineController?.fadeToOtherColor(this, ENGINE_SHIFT, null, effectLevel, 0.4f)
        ship.engineController?.extendFlame(this, 0.25f * effectLevel, 0.25f * effectLevel, 0.25f * effectLevel)
        Helper.soundPlayer?.playLoop(LOOP_SOUND, ship, effectLevel, effectLevel, ship.location, ship.velocity)
    }

    private fun playOutSounds(data: Data) {
        Helper.soundPlayer?.playSound(OUT_SOUND, 1f, 1f, data.source.location, data.source.velocity)
        Helper.soundPlayer?.playSound(OUT_SOUND, 1f, 1f, data.target.location, data.target.velocity)
    }

    private fun getAdvanceAmount(amount: Float, target: ShipAPI): Float {
        val targetTime = target.mutableStats?.timeMult?.modified ?: 1f
        return amount.coerceAtMost(targetTime * amount)
    }

    private fun applyEffects(source: ShipAPI, target: ShipAPI, stealSpeed: Float, effectLevel: Float) {
        val id = getSourceId(source)
        val targetStats = target.mutableStats
        if (targetStats != null) {
            val targetMult = 1f - (SPEED_STEAL_MULT * effectLevel)
            targetStats.maxSpeed?.modifyMult(id, targetMult)
            targetStats.acceleration?.modifyMult(id, targetMult)
            targetStats.deceleration?.modifyMult(id, targetMult)
            targetStats.turnAcceleration?.modifyMult(id, targetMult)
            targetStats.maxTurnRate?.modifyMult(id, targetMult)
        }

        val sourceStats = source.mutableStats
        if (sourceStats != null) {
            val sourceEffect = stealSpeed * effectLevel
            val sourceMult = 1f + (SPEED_STEAL_MULT * effectLevel)
            sourceStats.maxSpeed?.modifyFlat(id, sourceEffect)
            sourceStats.acceleration?.modifyMult(id, sourceMult)
            sourceStats.deceleration?.modifyMult(id, sourceMult)
            sourceStats.turnAcceleration?.modifyMult(id, sourceMult)
            sourceStats.maxTurnRate?.modifyMult(id, sourceMult)
        }
    }
}