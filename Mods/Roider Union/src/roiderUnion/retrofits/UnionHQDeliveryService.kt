package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import retroLib.impl.BaseRetrofitDeliveryService
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIndustries

class UnionHQDeliveryService(market: MarketAPI) : BaseRetrofitDeliveryService(market) {
    companion object {
        val REP_REASON = "your reputation is too low" // extern
        val DISRUPTED_REASON = "the Union HQ is disrupted"
    }

    override val isInstantOnly: Boolean = true

    override fun advance(amount: Float) {
        if (market?.isInEconomy == false) {
            done = true
            return
        }
        val lowRep = Helper.roiders?.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS) == true
        if (lowRep) {
            queued.forEach { it.pause(REP_REASON) }
            return
        }
        val disrupted = market?.hasFunctionalIndustry(RoiderIndustries.UNION_HQ) == false
        if (disrupted) {
            queued.forEach { it.pause(DISRUPTED_REASON) }
            return
        }
        queued.forEach { it.unpause(REP_REASON); it.unpause(DISRUPTED_REASON) }
        super.advance(amount)
    }
}