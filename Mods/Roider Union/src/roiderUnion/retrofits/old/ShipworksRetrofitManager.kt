package roiderUnion.retrofits.old

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.thoughtworks.xstream.XStream
import roiderUnion.econ.Shipworks
import roiderUnion.ids.MemoryKeys
import roiderUnion.retrofits.old.base.BaseRetrofitManager
import roiderUnion.retrofits.old.base.RetrofitData
import roiderUnion.retrofits.old.base.RetrofitsKeeper

/**
 * Author: SafariJohn
 */
class ShipworksRetrofitManager(fitter: String, entity: SectorEntityToken?, faction: FactionAPI) :
    BaseRetrofitManager(fitter, entity, faction) {
    companion object {
        fun aliasAttributes(x: XStream?) {}
    }

    override fun verifyData(
        id: String?,
        fitter: String?, source: String?, target: String?,
        cost: Double, time: Double, rep: RepLevel?,
        commission: Boolean
    ): RetrofitData? {
        // Recalculate cost if there's a market
        var cost = cost
        if (entity?.market != null && !entity.market.isPlanetConditionMarketOnly) {
            cost = RetrofitsKeeper
                .calculateCost(source, target, entity.market)
        }

        // Discount from having alpha core
        if (entity?.market?.memoryWithoutUpdate?.getBoolean(MemoryKeys.SHIPWORKS_ALPHA) == true) {
            cost *= ((100f - Shipworks.ALPHA_DISCOUNT) / 100f).toDouble()
        }
        return super.verifyData(
            id, fitter, source, target, cost,
            time, RepLevel.VENGEFUL, false
        )
    }
}