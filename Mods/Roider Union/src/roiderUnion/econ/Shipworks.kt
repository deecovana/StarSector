package roiderUnion.econ

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.MarketHelper
import roiderUnion.helpers.Memory
import roiderUnion.ids.Aliases
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIndustries

/**
 * Author: SafariJohn
 */
class Shipworks : BaseIndustry() {
    companion object {
        fun alias(x: XStream) {
            x.alias(Aliases.SW, Shipworks::class.java)
        }

        const val ALPHA_DISCOUNT = 10f
        const val BASE_SIZE = 3
        const val MAX_DEFICIT = 1
    }

    override fun apply() {
        demand(Commodities.SHIPS, BASE_SIZE)
        demand(Commodities.HEAVY_MACHINERY, BASE_SIZE - 2)
        supply(Commodities.SUPPLIES, BASE_SIZE - 2)
        supply(Commodities.METALS, BASE_SIZE)
        val deficit: Pair<String, Int> = getMaxDeficit(Commodities.SHIPS, Commodities.HEAVY_MACHINERY)
        if (deficit.two > MAX_DEFICIT) deficit.two = MAX_DEFICIT
        applyDeficitToProduction(
            2, deficit,
            Commodities.SUPPLIES,
            Commodities.METALS
        )
        super.apply(true)
        if (!isFunctional) {
            supply.clear()
            unapply()
        }
    }

    override fun applyAlphaCoreModifiers() {
        super.applyAlphaCoreModifiers()
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, true, market)
    }

    override fun addAlphaCoreDescription(tooltip: TooltipMakerAPI, mode: Industry.AICoreDescriptionMode) {
        super.addAlphaCoreDescription(tooltip, mode)
        tooltip.addPara(
            ExternalStrings.SW_ALPHA,
            0f,
            Misc.getHighlightColor(),
            Helper.floatToPercentString(ALPHA_DISCOUNT)
        )
    }

    override fun canImprove(): Boolean = false

    override fun isAvailableToBuild(): Boolean {
        val bpKnown = MarketHelper.playerGovernorOrMarketFactionKnowsIndustry(RoiderIndustries.SHIPWORKS, market)
        val bypass = Memory.contains(MemoryKeys.SHIPWORKS_BP_BYPASS, market)
        return if (bypass || bpKnown) super.isAvailableToBuild()
        else false
    }

    override fun getUnavailableReason(): String {
        return if (market.faction?.knowsIndustry(RoiderIndustries.SHIPWORKS) == false) {
            ExternalStrings.SW_UNAVAIL
        } else super.getUnavailableReason()
    }

    override fun showWhenUnavailable(): Boolean {
        return if (market.hasTag(Tags.MARKET_NO_INDUSTRIES_ALLOWED)) false
        else if (market.faction?.knowsIndustry(RoiderIndustries.SHIPWORKS) == true) true
        else if (Helper.hasCommission(RoiderFactions.ROIDER_UNION)) true
        else Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.SHIPWORKS) == true
    }

    //<editor-fold defaultstate="collapsed" desc="Cleanup code in various methods">
    override fun unapply() {
        super.unapply()
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, false, market)
    }

    override fun notifyDisrupted() {
        super.notifyDisrupted()
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, false, market)
    }

    override fun disruptionFinished() {
        super.disruptionFinished()
        isFunctional
        val alphaCore = if (getAICoreId() == null) false else getAICoreId() == Commodities.ALPHA_CORE
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, alphaCore, market)
    }

    override fun notifyBeingRemoved(mode: MarketAPI.MarketInteractionMode?, forUpgrade: Boolean) {
        super.notifyBeingRemoved(mode, forUpgrade)
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, false, market)
    }

    override fun applyGammaCoreModifiers() {
        super.applyGammaCoreModifiers()
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, false, market)
    }

    override fun applyBetaCoreModifiers() {
        super.applyBetaCoreModifiers()
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, false, market)
    }

    override fun applyNoAICoreModifiers() {
        super.applyNoAICoreModifiers()
        Memory.set(MemoryKeys.SHIPWORKS_ALPHA, false, market)
    } //</editor-fold>
}