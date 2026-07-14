package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.impl.BaseRetrofitDeliveryService
import roiderUnion.ids.RoiderIndustries

class ShipworksDeliveryService(market: MarketAPI) : BaseRetrofitDeliveryService(market) {
    companion object {
        val DISRUPTED_REASON = "your Rockpiper Shipworks is disrupted"
    }

    override val isInstantOnly: Boolean = true

    override fun advance(amount: Float) {
        if (market?.isPlanetConditionMarketOnly == true) {
            done = true
            return
        }
        val disrupted = market?.hasFunctionalIndustry(RoiderIndustries.SHIPWORKS) == false
        if (disrupted) {
            queued.forEach { it.pause(DISRUPTED_REASON) }
            return
        }
        queued.forEach { it.unpause(DISRUPTED_REASON) }
        super.advance(amount)
    }
}