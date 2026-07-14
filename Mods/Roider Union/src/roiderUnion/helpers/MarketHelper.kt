package roiderUnion.helpers

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.population.CoreImmigrationPluginImpl
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import roiderUnion.econ.DivesController
import roiderUnion.ids.AICores
import roiderUnion.ids.RoiderItems
import java.util.*
import kotlin.math.roundToInt

object MarketHelper {
    fun setStorageMarket(market: MarketAPI, primaryEntity: SectorEntityToken) {
        market.isHidden = true
        market.surveyLevel = MarketAPI.SurveyLevel.FULL
        market.isPlanetConditionMarketOnly = false
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE)
        primaryEntity.memoryWithoutUpdate["\$tradeMode"] = CampaignUIAPI.CoreUITradeMode.NONE
        market.econGroup = market.id
        market.memoryWithoutUpdate[DecivTracker.NO_DECIV_KEY] = true
    }

    fun setPrimaryEntity(market: MarketAPI, entity: SectorEntityToken) {
        market.primaryEntity = entity
        entity.market = market
    }

    fun attachEntity(market: MarketAPI, entity: SectorEntityToken) {
        market.connectedEntities += entity
        entity.market = market
    }

    fun getStationFleet(market: MarketAPI): CampaignFleetAPI? {
        return Misc.getStationFleet(market)
    }

    fun addAICoreToIndustry(market: MarketAPI, core: AICores, industry: String) {
        if (!market.hasIndustry(industry)) return
        market.getIndustry(industry)?.aiCoreId = core.id
    }

    fun setPopulation(market: MarketAPI, popSize: Int) {
        val test = MathUtils.clamp(popSize, 3, Misc.MAX_COLONY_SIZE)
        while (market.size > test) CoreImmigrationPluginImpl.reduceMarketSize(market)
        while (market.size < test) CoreImmigrationPluginImpl.increaseMarketSize(market)
    }

    fun addIndustry(market: MarketAPI, id: String) {
        if (market.hasIndustry(id)) return
        market.addIndustry(id)
    }

    /**
     * @return whether the replacement industry has been added
     */
    fun replaceIndustry(market: MarketAPI?, id: String, replaceId: String? = null): Boolean {
        if (market == null) return false
        if (replaceId == null) {
            market.removeIndustry(id, null, false)
            return true
        }
        if (market.hasIndustry(replaceId)) return true
        if (market.hasIndustry(id)) {
            val ind = market.getIndustry(id)
            if (ind?.spec?.upgrade == replaceId) {
                upgradeIndustry(market, ind, true)
            } else {
                market.removeIndustry(id, null, false)
            }
        } else {
            market.removeIndustry(id, null, false)
        }

        return true
    }

    fun upgradeIndustry(market: MarketAPI?, id: String, instant: Boolean): Boolean {
        if (market == null) return false
        val ind = market.getIndustry(id) ?: return false
        return upgradeIndustry(market, ind, instant)
    }

    fun upgradeIndustry(market: MarketAPI?, ind: Industry?, instant: Boolean): Boolean {
        if (market == null) return false
        if (ind == null) return false
        if (!ind.canUpgrade()) return false
        if (ind.spec.upgrade.isNullOrEmpty()) return false
        ind.startUpgrading()
        if (instant) ind.finishBuildingOrUpgrading()
        return true
    }

    fun addItemEffectsToRepo() {
        ItemEffectsRepo.ITEM_EFFECTS[RoiderItems.HISTORICAL_CHARTER] = object : BoostIndustryInstallableItemEffect(
            RoiderItems.HISTORICAL_CHARTER, RoiderItems.CHARTER_PRODUCTION_BONUS, 0
        ) {
            override fun apply(industry: Industry?) {
                super.apply(industry)
                industry?.market?.stats?.dynamic?.getMod(Stats.COMBAT_FLEET_SIZE_MULT)
                    ?.modifyFlat(
                        spec.id,
                        RoiderItems.CHARTER_FLEET_SIZE_BONUS,
                        Misc.ucFirst(spec.name.lowercase(Locale.getDefault()))
                    )
                if (industry is DivesController) {
                    industry.miningRange.modifyFlat(spec.id, RoiderItems.CHARTER_MINING_RANGE_BONUS)
                }
            }

            override fun unapply(industry: Industry?) {
                super.unapply(industry)
                industry?.market?.stats?.dynamic?.getMod(Stats.COMBAT_FLEET_SIZE_MULT)?.unmodifyFlat(spec.id)
                if (industry is DivesController) {
                    industry.miningRange.unmodifyFlat(spec.id)
                }
            }

            override fun addItemDescriptionImpl(
                industry: Industry?, text: TooltipMakerAPI?, data: SpecialItemData?,
                mode: InstallableIndustryItemPlugin.InstallableItemDescriptionMode?, pre: String?, pad: Float
            ) {
                val post = if (mode == InstallableIndustryItemPlugin.InstallableItemDescriptionMode.MANAGE_ITEM_DIALOG_LIST) {
                    ExternalStrings.CHARTER_PRODUCTION1
                } else {
                    ExternalStrings.CHARTER_PRODUCTION2
                }
                text?.addPara(
                    pre + post,
                    pad, Misc.getHighlightColor(),
                    RoiderItems.CHARTER_PRODUCTION_BONUS.toString(),
                    RoiderItems.CHARTER_MINING_RANGE_BONUS.roundToInt().toString(),
                    Helper.multToPercentString(RoiderItems.CHARTER_FLEET_SIZE_BONUS)
                )
            }
        }
    }

    fun isNexerelinGovernorship(market: MarketAPI): Boolean = market.isPlayerOwned && market.faction?.isPlayerFaction == false

    fun playerGovernorOrMarketFactionKnowsIndustry(industry: String, market: MarketAPI): Boolean {
        return (isNexerelinGovernorship(market) && Helper.sector?.playerFaction?.knowsIndustry(industry) == true)
                || (market.faction?.knowsIndustry(industry) == true)
    }
}