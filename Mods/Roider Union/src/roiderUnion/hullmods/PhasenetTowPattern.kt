package roiderUnion.hullmods

import com.fs.starfarer.api.campaign.BuffManagerAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.ids.hullmods.RoiderHullmods

/**
 * Author: SafariJohn
 */
class PhasenetTowPattern : BaseHullMod() {
    override fun getDescriptionParam(index: Int, hullSize: HullSize): String {
        val size = when (index) {
            0 -> HullSize.FRIGATE
            1 -> HullSize.DESTROYER
            2 -> HullSize.CRUISER
            else -> HullSize.CAPITAL_SHIP
        }
        return ExternalStrings.NUMBER_PLUS.replaceNumberToken(PhasenetTowScript.getMaxBonusForSize(hullSize, size))
    }
}