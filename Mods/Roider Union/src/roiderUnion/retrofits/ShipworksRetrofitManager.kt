package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.RetrofitData
import retroLib.api.RetrofitFilter
import retroLib.impl.BaseRetrofitManager
import roiderUnion.helpers.Helper

class ShipworksRetrofitManager(market: MarketAPI, filter: RetrofitFilter) : BaseRetrofitManager(market, market.faction, filter) {
    override fun isTargetAllowed(targetId: String?): Boolean {
        return Helper.sector?.playerFaction?.knowsShip(targetId) == true
    }

    override fun isSourceAllowed(data: RetrofitData): Boolean {
        return Helper.sector?.playerFaction?.knowsShip(data.target) == true
    }
}