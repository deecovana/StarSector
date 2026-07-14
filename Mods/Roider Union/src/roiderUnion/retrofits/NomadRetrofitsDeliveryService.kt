package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.impl.BaseRetrofitDeliveryService

class NomadRetrofitsDeliveryService(market: MarketAPI) : BaseRetrofitDeliveryService(market) {
    override val isInstantOnly: Boolean = true
}