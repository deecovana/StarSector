package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class EVAExpertise : BaseHullMod() {
    companion object {
        const val REPAIR_BONUS = 20f
        const val CASUALTY_REDUCTION = 20f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.combatEngineRepairTimeMult.modifyMult(id, Helper.percentReductionAsMult(REPAIR_BONUS))
        stats.combatWeaponRepairTimeMult.modifyMult(id, Helper.percentReductionAsMult(REPAIR_BONUS))
        stats.crewLossMult.modifyMult(id, Helper.percentReductionAsMult(CASUALTY_REDUCTION))
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String {
        if (index == 0) return Helper.floatToPercentString(REPAIR_BONUS)
        return if (index == 1) Helper.floatToPercentString(CASUALTY_REDUCTION)
        else ExternalStrings.DEBUG_NULL
    }
}