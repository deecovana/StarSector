package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.RepLevel

/**
 * Author: SafariJohn
 */
interface RetrofitVerifier {
    fun verifyData(
        id: String?, fitter: String?,
        source: String?, target: String?, cost: Double,
        time: Double, rep: RepLevel?, commission: Boolean
    ): RetrofitData?

    fun verifyData(
        data: RetrofitData?
    ): RetrofitData? {
        return if (data == null) null
            else verifyData(
                data.id,
                data.fitter,
                data.sourceHull,
                data.targetHull,
                data.cost,
                data.time,
                data.reputation,
                data.commission
            )
    }
}