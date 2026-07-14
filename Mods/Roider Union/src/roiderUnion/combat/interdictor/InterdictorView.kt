package roiderUnion.combat.interdictor

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import java.awt.Color

class InterdictorView(private val model: InterdictorModel) {
    fun showJitter(ship: ShipAPI, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val targetData = model.mainTarget ?: return
        if (model.targets.isEmpty()) return

        var first = true
        for (target in model.targets) {
            if (effectLevel >= 1) {
                first = false
            }

            if (effectLevel > 0) {
                val maxRangeBonus = 20f + target.collisionRadius * 0.25f
                var jitterRangeBonus = effectLevel * maxRangeBonus
                if (state == ShipSystemStatsScript.State.OUT) {
                    jitterRangeBonus = maxRangeBonus + (1f - effectLevel) * maxRangeBonus
                }
                target.setJitter(
                    this,  //target.getSpriteAPI().getAverageColor(),
                    getEffectColor(target),
                    effectLevel, 6, 0f, 0 + jitterRangeBonus
                )
                if (first) {
                    ship.setJitter(
                        this,  //target.getSpriteAPI().getAverageColor(),
                        getEffectColor(targetData),
                        effectLevel, 6, 0f, 0 + jitterRangeBonus
                    )
                }
            }
        }
    }

    fun showFloaty(ship: ShipAPI) {
        if (model.canShowFloaty) model.canShowFloaty = false
        else return
        if (model.targets.isEmpty()) return
        for (target in model.targets) {
            var color: Color? = getEffectColor(target)
            color = Misc.setAlpha(color, 255)

            if (target.fluxTracker.showFloaty() || ship === Global.getCombatEngine().playerShip || target === Global.getCombatEngine().playerShip) {
                target.fluxTracker.showOverloadFloatyIfNeeded(ExternalStrings.INTERDICTOR_FLOATY, color, 4f, true)
            }
        }
    }

    private fun getEffectColor(ship: ShipAPI?): Color {
        return if (ship!!.engineController.shipEngines.isEmpty()) {
            InterdictorStats.EFFECT_COLOR
        } else Misc.setAlpha(
            ship.engineController.shipEngines[0].engineColor,
            InterdictorStats.EFFECT_COLOR.alpha
        )
    }

    fun getInfoText(): String? {
        return when (model.infoState) {
            InterdictorInfoState.DEFAULT -> null
            InterdictorInfoState.READY -> ExternalStrings.SYSTEM_READY
            InterdictorInfoState.OUT_OF_RANGE -> ExternalStrings.SYSTEM_OUT_OF_RANGE
            InterdictorInfoState.NO_TARGET -> ExternalStrings.SYSTEM_NO_TARGET
        }
    }

}
