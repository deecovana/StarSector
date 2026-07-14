package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.econ.MarketAPI
import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster
import retroLib.impl.BaseRetrofitFilter
import roiderUnion.helpers.Memory
import roiderUnion.ids.Fitters
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.nomads.NomadsData

class NomadRetrofitsFilter(market: MarketAPI?, adjuster: RetrofitAdjuster) : BaseRetrofitFilter(market, adjuster) {
    override fun allow(data: RetrofitData?): Boolean {
        if (data == null) return false
        if (!super.allow(data)) return false
        if (market == null) return false
        val base = market!!.primaryEntity
        val group = Memory.get(MemoryKeys.NOMAD_GROUP, base, { it is NomadsData }, { NomadsData("","")}) as NomadsData
        if (!group.knownBPs.contains(data.target)) return false
        if (retroLib.Helper.isFrameHull(data.targetSpec) && data.tags.contains(RoiderFactions.ROIDER_UNION)) return true
        if (data.tags.contains(Fitters.ARGOS)) return true
        if (data.tags.contains(Fitters.FULL) && retroLib.Helper.marketHasHeavyIndustry(market)) return true
        return false
    }
}