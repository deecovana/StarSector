package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.hullmods.OtherHullmods

/**
 * Author: SafariJohn
 */
class BombardDTC : BaseHullMod() {
    companion object {
        private const val RANGE_BONUS = 35f
        private val BLOCKED_HULLMODS = setOf(
            HullMods.DEDICATED_TARGETING_CORE,
            HullMods.INTEGRATED_TARGETING_UNIT,
            OtherHullmods.ILK_SENSOR_SUITE
        )
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String) {
        BLOCKED_HULLMODS.filter { ship?.variant?.hullMods?.contains(it) == true }
            .forEach { ship?.variant?.removeMod(it) }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        stats?.ballisticWeaponRangeBonus?.modifyPercent(id, RANGE_BONUS)
        stats?.energyWeaponRangeBonus?.modifyPercent(id, RANGE_BONUS)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String {
        return if (index == 0) RANGE_BONUS.toInt().toString()
        else ExternalStrings.DEBUG_NULL
    }
}