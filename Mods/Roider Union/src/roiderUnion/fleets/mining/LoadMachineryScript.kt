package roiderUnion.fleets.mining

import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import roiderUnion.helpers.CargoHelper
import kotlin.reflect.KFunction1

class LoadMachineryScript(
    private val fleet: CampaignFleetAPI,
    private val current: RouteSegment,
    private val goNextScript: KFunction1<RouteSegment, Script>
) : Script {
    override fun run() {
        CargoHelper.addCommodity(
            Commodities.HEAVY_MACHINERY,
            fleet.cargo.maxCapacity * RoiderMiningRouteManager.MACHINERY_CARGO,
            fleet.cargo
        )
        goNextScript(current).run()
    }
}