package roiderUnion.retrofits.old

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.RoiderIds
import roiderUnion.ids.Fitters
import roiderUnion.ids.RoiderIndustries
import roiderUnion.retrofits.old.base.BaseRetrofitManager

/**
 * Author: SafariJohn
 */
class UnionHQRetrofitManager : BaseRetrofitManager {
    constructor(fitter: String, entity: SectorEntityToken, faction: FactionAPI) : super(fitter, entity, faction)
    constructor(fitter: String, entity: SectorEntityToken, faction: FactionAPI, withIntel: Boolean) : super(
        fitter,
        entity,
        faction,
        withIntel
    )

    companion object {
        fun aliasAttributes(x: XStream?) {}
    }

    override val fitter: String
        get() {
            if (entity?.market == null) return super.fitter
            return if (entity.market.hasIndustry(Industries.HEAVYINDUSTRY)
                || entity.market.hasIndustry(Industries.ORBITALWORKS)
                || entity.market.hasIndustry(RoiderIndustries.SHIPWORKS)
            ) {
                Fitters.FULL
            } else Fitters.LIGHT
        }

    override fun advanceImpl(amount: Float) {
        if (entity?.market == null || entity.market.isPlanetConditionMarketOnly) {
            endImmediately()
            return
        }
        if (queued.isEmpty()) return
        if (Global.getSector().isPaused) return
        if (faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS)) {
            queued[0].pause("your reputation is too low") // extern
            return
        }
        val unionHQ: SubmarketAPI = entity.market.getSubmarket(RoiderIds.Roider_Submarkets.UNION_MARKET) ?: return
        val retrofitsEnabled: Boolean = unionHQ.plugin.isEnabled(null)
        if (!retrofitsEnabled) {
            if (queued.isNotEmpty()) {
                queued[0].pause("the Union HQ is disrupted")
            }
            return
        } else {
            queued[0].unpause()
        }
        super.advanceImpl(amount)
    }
}