package roiderUnion.nomads.bases

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.MarketHelper
import roiderUnion.helpers.Memory
import roiderUnion.ids.*
import roiderUnion.nomads.NomadsHelper
import roiderUnion.nomads.NomadsNameKeeper

object NomadBaseBuilder {
    fun build(system: StarSystemAPI, faction: String, level: NomadBaseLevel): SectorEntityToken? {
        if (Helper.anyNull(Helper.sector?.economy, Helper.sector?.intelManager)) return null
        val loc = BaseThemeGenerator.pickHiddenLocationNotNearStar(null, system, NomadsHelper.STATION_BASE_GAP, null) ?: return null
        val entity = Helper.createMakeshiftStation(system, loc, faction) ?: return null
        val name = NomadsNameKeeper.generateName(NomadsNameKeeper.Type.BASE)
        entity.name = name
        entity.customDescriptionId = RoiderIds.Entities.NOMAD_MARKET_ID + RoiderIds.DESC

        val market = Helper.factory?.createMarket(
            RoiderIds.Entities.NOMAD_MARKET_ID + Misc.genUID(),
            name,
            2
        ) ?: return null
        market.isHidden = true
        market.factionId = faction
        market.econGroup = market.id
        MarketHelper.setPrimaryEntity(market, entity)
        entity.sensorProfile = 1f
        entity.isDiscoverable = true
        Memory.set(MemoryKeys.NOMAD_BASE, true, entity)
        Helper.addStationSensorProfile(entity)
        entity.addTag(RoiderTags.NOMAD_BASE)
        market.addIndustry(Industries.POPULATION)
        market.addIndustry(Industries.SPACEPORT)
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE)
        entity.addScript(NomadBaseLevelTracker(entity, level))
        market.reapplyIndustries()
        market.tariff.modifyFlat(MemoryKeys.DEFAULT_TARIFF, market.faction.tariffFraction)
        entity.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.ORBITAL)
        market.surveyLevel = MarketAPI.SurveyLevel.FULL
        market.conditions.forEach { it.isSurveyed = true }

        Helper.sector?.addScript(NomadWarningTaggingScript(entity))
        Helper.sector?.economy?.addUpdateListener(RoiderSupplyScript(entity))
        Helper.sector?.economy?.addMarket(market, true)

        val intel = NomadBaseIntelPlugin(entity)
        Memory.set(MemoryKeys.NOMAD_BASE_INTEL, intel, entity)
        Helper.sector?.intelManager?.addIntel(intel, true)
        intel.playerVisibleTimestamp = null
        Helper.sector?.addScript(intel)

        return entity
    }
}