package roiderUnion.hullmods

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript
import com.fs.starfarer.api.util.FaderUtil
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.TPSHelper
import java.awt.Color

class TemporalPhaseShell_Fighter : BaseHullMod() {
    companion object {
        const val ID = "roider_temporalPhaseShell"
        const val PHASE_COOLDOWN_REDUCTION = 80f
        const val FLUX_THRESHOLD_INCREASE_PERCENT = 50f
        const val TIME_MULT = 50f
        const val DEAD_DIVE_DURATION = 1.5f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        stats?.phaseCloakCooldownBonus?.modifyMult(id, 1f - PHASE_COOLDOWN_REDUCTION / 100f)
        stats?.dynamic?.getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD)
            ?.modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT)
        stats?.dynamic?.getMod(Stats.PHASE_TIME_BONUS_MULT)
            ?.modifyFlat(id, Helper.percentIncreaseAsMult(TIME_MULT))
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String? {
        return when (index) {
            0 -> Helper.floatToPercentString(PHASE_COOLDOWN_REDUCTION)
            1 -> Helper.floatToPercentString(FLUX_THRESHOLD_INCREASE_PERCENT)
            else -> null
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        ship?.addListener(DeadDiveScript(ship))
    }


    class DeadDiveScript(private val ship: ShipAPI) : AdvanceableListener {
        override fun advance(amount: Float) {
            if (!ship.isAlive) {
                TPSHelper.phaseOut(ship)
                ship.removeListener(this)
            }
        }
    }
}