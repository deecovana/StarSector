package retroLib.impl

import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.Helper
import retroLib.api.RetrofitAdjuster
import retroLib.RetrofitData

open class BaseRetrofitAdjuster(protected val market: MarketAPI?) : RetrofitAdjuster {
    override fun getFilterTestData(data: RetrofitData): RetrofitData {
        return RetrofitData(data, cost = data.cost ?: Helper.calculateCost(data.source, data.target, market))
    }

    override fun getAdjustedData(data: RetrofitData): RetrofitData {
        return getFilterTestData(data)
    }
}