package scripts.campaign.econ

import exerelin.world.ExerelinProcGen
import exerelin.world.NexMarketBuilder
import exerelin.world.industry.IndustryClassGen
import roiderUnion.helpers.MarketHelper
import roiderUnion.ids.RoiderIndustries

/**
 * Author: SafariJohn
 */
class Roider_DivesNexClassGen : IndustryClassGen(RoiderIndustries.DIVES, RoiderIndustries.UNION_HQ) {
    override fun canApply(entity: ExerelinProcGen.ProcGenEntity): Boolean {
        return if (entity.market.industries.size >= 12) false else super.canApply(entity)
    }

    override fun apply(entity: ExerelinProcGen.ProcGenEntity, instant: Boolean) {
        // If already have dives, upgrade to Union HQ
        val upgrading = MarketHelper.upgradeIndustry(entity.market, RoiderIndustries.DIVES, instant)
        // build Union HQ directly
        if (!upgrading) {
            NexMarketBuilder.addIndustry(entity.market, RoiderIndustries.UNION_HQ, this.id, instant)
            entity.numProductiveIndustries += 1
        }
    }

    override fun canAutogen(): Boolean {
        return false
    }
}