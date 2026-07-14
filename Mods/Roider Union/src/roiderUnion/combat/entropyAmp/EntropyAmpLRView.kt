package roiderUnion.combat.entropyAmp

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import roiderUnion.helpers.ExternalStrings
import java.awt.Color
import kotlin.math.max

class EntropyAmpLRView(private val model: EntropyAmpLRModel) {
    companion object {
        val KEY_SHIP = Any()
        val KEY_TARGET = Any()
        val TEXT_COLOR = Color(255, 55, 55, 255)
        val JITTER_COLOR = Color(255, 50, 50, 75)
        val JITTER_UNDER_COLOR = Color(255, 100, 100, 155)
    }

    fun showFloatyText(ship: ShipAPI) {
        if (!model.canShowFloatyText) return
        model.canShowFloatyText = false

        val target = getTargetData()?.target ?: return

        if (target.fluxTracker.showFloaty()
            || ship === Global.getCombatEngine().playerShip
            || target === Global.getCombatEngine().playerShip
        ) {
            target.fluxTracker.showOverloadFloatyIfNeeded(
                ExternalStrings.ENTROPY_FLOATY,
                TEXT_COLOR,
                4f,
                true
            )
        }
    }

    fun showEmitterVisuals(
        state: State,
        effectLevel: Float,
        ship: ShipAPI,
    ) {
        val targetData = getTargetData() ?: return

        if (effectLevel > 0) {
            if (state != State.IN) {
                targetData.elapsedAfterInState += Global.getCombatEngine().elapsedInLastFrame
            }
            val shipJitterLevel = if (state == State.IN) {
                effectLevel
            } else {
                val durOut = 0.5f
                max(0f, durOut - targetData.elapsedAfterInState) / durOut
            }
            val maxRangeBonus = 50f
            val jitterRangeBonus = shipJitterLevel * maxRangeBonus
            if (shipJitterLevel > 0) {
                //ship.setJitterUnder(KEY_SHIP, JITTER_UNDER_COLOR, shipJitterLevel, 21, 0f, 3f + jitterRangeBonus);
                ship.setJitter(
                    KEY_SHIP,
                    JITTER_COLOR,
                    shipJitterLevel,
                    4,
                    0f,
                    jitterRangeBonus)
            }
        }
    }

    fun showEntropyVisuals(effectLevel: Float) {
        if (effectLevel > 0) {
            val targetData = getTargetData() ?: return

            //target.setJitterUnder(KEY_TARGET, JITTER_UNDER_COLOR, targetJitterLevel, 5, 0f, 15f);
            targetData.target!!.setJitter(
                KEY_TARGET,
                JITTER_COLOR,
                effectLevel,
                3,
                0f,
                5f
            )
        }
    }

    fun maintainStatusForPlayerShip() {
        if (model.targetData?.target === Global.getCombatEngine().playerShip) {
            val damMultString = ((model.targetData!!.currDamMult - 1f) * 100f).toInt().toString()
            Global.getCombatEngine().maintainStatusForPlayerShip(
                KEY_TARGET,
                model.targetData!!.ship.system.specAPI.iconSpriteName,
                model.targetData!!.ship.system.displayName,
                ExternalStrings.ENTROPY_PLAYER_STATUS.replace("%s", damMultString),
                true
            )
        }
    }

    private fun getTargetData(): EntropyAmpLRStats.TargetData? {
        return if (model.targetData != null) model.targetData
        else null
    }

    fun getStatusData(
        index: Int,
        effectLevel: Float
    ): StatusData? {
        if (effectLevel > 0 && index == 0) {
            val damMult = 1f + (EntropyAmpLRInteractor.DAM_MULT - 1f) * effectLevel
            val damMultString = ((damMult - 1f) * 100f).toInt().toString()
            return StatusData(ExternalStrings.ENTROPY_TARGET_STATUS.replace("%s", damMultString), false)
        }
        return null
    }

    fun getInfoText(system: ShipSystemAPI): String? {
        if (system.isOutOfAmmo) return null
        if (system.state != ShipSystemAPI.SystemState.IDLE) return null
        if (model.isTargetInRange) return ExternalStrings.SYSTEM_READY
        return if (model.isTargetOutOfRange) ExternalStrings.SYSTEM_OUT_OF_RANGE
        else ExternalStrings.SYSTEM_NO_TARGET
    }

}
