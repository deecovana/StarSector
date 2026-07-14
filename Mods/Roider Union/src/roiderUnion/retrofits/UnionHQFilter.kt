package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster
import retroLib.impl.BaseRetrofitFilter
import roiderUnion.ids.Fitters
import roiderUnion.ids.RoiderFactions

class UnionHQFilter(market: MarketAPI?, adjuster: RetrofitAdjuster) : BaseRetrofitFilter(market, adjuster) {
    override fun allow(data: RetrofitData?): Boolean {
        if (data == null) return false
        if (!super.allow(data)) return false
        if (retroLib.Helper.isFrameHull(data.targetSpec) && data.tags.contains(RoiderFactions.ROIDER_UNION)) return true
        if (data.tags.contains(Fitters.LIGHT)) return true
        if (data.tags.contains(Fitters.FULL) && retroLib.Helper.marketHasHeavyIndustry(market)) return true
        return false
    }
}