package roiderUnion.retrofits

import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster
import retroLib.impl.BaseRetrofitFilter
import roiderUnion.ids.Fitters

class RoiderAllFilter(adjuster: RetrofitAdjuster) : BaseRetrofitFilter(null, adjuster) {
    override fun allow(data: RetrofitData?): Boolean {
        if (data == null) return false
        if (data.tags.contains(Fitters.ALL)) return true
        return super.allow(data)
    }
}