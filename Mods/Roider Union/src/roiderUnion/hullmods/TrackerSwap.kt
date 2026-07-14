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
class TrackerSwap : BaseHullMod() {
    companion object {
        @JvmField
        val WINGS_PER_SHIP: MutableMap<String, Int> = HashMap()

        init {
            WINGS_PER_SHIP[ShipsAndWings.JANE] = 1
//            WINGS_PER_SHIP[ShipsAndWings.TRAILBOSS] = 1
            WINGS_PER_SHIP[ShipsAndWings.CALIDOR] = 1
//            WINGS_PER_SHIP[ShipsAndWings.ARGOS] = Helper.settings?.getHullSpec(ShipsAndWings.ARGOS)?.builtInWings?.size ?: 3
            WINGS_PER_SHIP[ShipsAndWings.ARGOS] = 1
            WINGS_PER_SHIP[ShipsAndWings.RANCH] = 2
//            WINGS_PER_SHIP[ShipsAndWings.RANCH_DAMPER] = 2
        }
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        val argosBonusSwap = ship != null && ship.hullSpec.baseHullId == ShipsAndWings.ARGOS
        if (argosBonusSwap) {
            tooltip?.addPara(ExternalStrings.TRACKER_SWAP_ARGOS, Helper.PAD)
        }
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        val hasGlitzSwitch = ship?.variant?.hasHullMod(RoiderHullmods.GLITZ_SWITCH) == true
        return if (hasGlitzSwitch) ExternalStrings.TRACKER_SWAP_SWITCHED else ExternalStrings.BREAKERS_REQ
    }

    override fun isApplicableToShip(ship: ShipAPI?): Boolean {
        return if (ship?.variant?.hasHullMod(RoiderHullmods.GLITZ_SWITCH) == true) false
        else WINGS_PER_SHIP.containsKey(ship?.hullSpec?.baseHullId)
    }
}