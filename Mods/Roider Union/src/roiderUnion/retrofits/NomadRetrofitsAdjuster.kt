package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.RepLevel
import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster

class NomadRetrofitsAdjuster(private val ignoreRep: Boolean) : RetrofitAdjuster {
    override fun getFilterTestData(data: RetrofitData): RetrofitData {
        return RetrofitData(data, commission = false, reputation = if (ignoreRep) RepLevel.VENGEFUL else data.reputation)
    }

    override fun getAdjustedData(data: RetrofitData): RetrofitData = getFilterTestData(data)
}