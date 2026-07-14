package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import roiderUnion.econ.Shipworks
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster

class ShipworksAdjuster(private val market: MarketAPI) : RetrofitAdjuster {
    override fun getFilterTestData(data: RetrofitData): RetrofitData {
        return RetrofitData(data, cost = calculateCost(data), reputation = RepLevel.VENGEFUL, commission = false)
    }

    private fun calculateCost(data: RetrofitData): Double {
        var result = data.cost ?: retroLib.Helper.calculateCost(data.source, data.target, market)
        val alphaDiscount = Memory.isFlag(MemoryKeys.SHIPWORKS_ALPHA, market)
        if (alphaDiscount) result *= ((100f - Shipworks.ALPHA_DISCOUNT) / 100f).toDouble()
        return result
    }

    override fun getAdjustedData(data: RetrofitData): RetrofitData = getFilterTestData(data)
}