package roiderUnion.hullmods

import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import roiderUnion.combat.TrackerSpeedBoost
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.ids.hullmods.RoiderHullmods

class TrackerCoreShip : BaseHullMod() {
    companion object {
        const val SPEED_BOOST = TrackerSpeedBoost.SPEED_BOOST
        const val MAX_SPEED = TrackerSpeedBoost.MAX_SPEED.toInt().toString()
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (!MIDAS.hasMIDASStatic(stats?.variant)) {
            stats?.variant?.removeMod(RoiderHullmods.TRACKER_CORE_SHIP)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
        return when (index) {
            0 -> ExternalStrings.NUMBER_PLUS.replaceNumberToken(SPEED_BOOST.toInt())
            1 -> MAX_SPEED
            else -> ExternalStrings.DEBUG_NULL
        }
    }

    override fun canBeAddedOrRemovedNow(
        ship: ShipAPI?,
        marketOrNull: MarketAPI?,
        mode: CampaignUIAPI.CoreUITradeMode?
    ): Boolean {
        return false
    }
}