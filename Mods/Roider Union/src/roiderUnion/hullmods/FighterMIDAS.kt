package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.hullmods.MIDAS.Companion.EMP_REDUCTION
import roiderUnion.hullmods.MIDAS.Companion.MASS_BONUS

/**
 * Author: SafariJohn
 */
class FighterMIDAS : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        stats?.empDamageTakenMult?.modifyMult(MIDAS.MOD_ID, Helper.percentReductionAsMult(EMP_REDUCTION))
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String) {
        if (Helper.anyNull(ship)) return

        ship!!.mass = ship.mass * (1f + MASS_BONUS / 100f)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String {
        if (index == 0) return Helper.floatToPercentString(EMP_REDUCTION)
        return if (index == 1) Helper.floatToPercentString(MASS_BONUS)
        else ExternalStrings.DEBUG_NULL
    }
}