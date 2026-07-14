package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.ids.ShipsAndWings

/**
 * Author: SafariJohn
 */
class GlitzSwitch : BaseHullMod() {
    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        val argosBonusSwap = ship != null && ship.hullSpec.baseHullId == ShipsAndWings.ARGOS
        if (argosBonusSwap) {
            tooltip?.addPara(ExternalStrings.GLITZ_SWITCH_ARGOS, Helper.PAD)
        }
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        val hasGlitzSwitch = ship?.variant?.hasHullMod(RoiderHullmods.TRACKER_SWAP) == true
        return if (hasGlitzSwitch) ExternalStrings.GLITZ_SWITCH_SWAPPED else ExternalStrings.BREAKERS_REQ
    }

    override fun isApplicableToShip(ship: ShipAPI?): Boolean {
        return if (ship?.variant?.hasHullMod(RoiderHullmods.TRACKER_SWAP) == true) false
        else TrackerSwap.WINGS_PER_SHIP.containsKey(ship?.hullSpec?.baseHullId)
    }
}