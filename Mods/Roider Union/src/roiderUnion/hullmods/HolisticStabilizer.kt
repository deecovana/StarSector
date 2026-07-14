package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.hullmods.BallisticRangefinder.RangefinderRangeModifier
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class HolisticStabilizer : BaseHullMod() {
    companion object {
        const val MEDIUM_RANGE_BONUS = 100f
        const val MEDIUM_RANGE_MAX = 900f
        const val TURRET_SPEED_BONUS = 50f
        const val RECOIL_BONUS = 10f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats, id)) return

        stats!!.weaponTurnRateBonus.modifyPercent(id, TURRET_SPEED_BONUS)
        stats.beamWeaponTurnRateBonus.modifyPercent(id, TURRET_SPEED_BONUS)
        stats.maxRecoilMult.modifyMult(id, 1f - 0.01f * RECOIL_BONUS)
        stats.recoilPerShotMult.modifyMult(id, 1f - 0.01f * RECOIL_BONUS)
        // slower recoil recovery, also, to match the reduced recoil-per-shot
        stats.recoilDecayMult.modifyMult(id, 1f - 0.01f * RECOIL_BONUS)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.addListener(RangefinderRangeModifier(0f, MEDIUM_RANGE_BONUS, MEDIUM_RANGE_MAX))
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String {
        if (index == 0) return MEDIUM_RANGE_BONUS.toInt().toString()
        if (index == 1) return (MEDIUM_RANGE_BONUS * 2f).toInt().toString()
        if (index == 2) return MEDIUM_RANGE_MAX.toInt().toString()
        if (index == 3) return Helper.floatToPercentString(TURRET_SPEED_BONUS)
        return if (index == 4) Helper.floatToPercentString(RECOIL_BONUS)
        else ExternalStrings.DEBUG_NULL
    }
}