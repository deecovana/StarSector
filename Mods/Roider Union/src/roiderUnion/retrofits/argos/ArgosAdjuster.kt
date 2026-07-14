package roiderUnion.retrofits.argos

import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.Helper
import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster
import retroLib.impl.BaseRetrofitAdjuster

class ArgosAdjuster(market: MarketAPI?) : BaseRetrofitAdjuster(market) {
    override fun getFilterTestData(data: RetrofitData): RetrofitData {
        return RetrofitData(data, reputation = RepLevel.VENGEFUL, commission = false, cost = Helper.calculateCost(data.source, data.target, market))
    }
}