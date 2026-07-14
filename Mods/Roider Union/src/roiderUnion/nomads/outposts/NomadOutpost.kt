package roiderUnion.nomads.outposts

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.*
import java.util.LinkedHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class NomadOutpost(system: StarSystemAPI, faction: String) : EveryFrameScript {
    companion object {
        const val SITE_CHANCE = 0.6
        const val OUTPOST_CHANCE = 0.2

        fun getOutpostLevel(outpost: SectorEntityToken): NomadOutpostLevel {
            return Memory.get(MemoryKeys.NOMAD_OUTPOST_LEVEL, outpost, { it is NomadOutpostLevel }, { NomadOutpostLevel.CLAIM }) as NomadOutpostLevel
        }

        fun setOutpostLevel(outpost: SectorEntityToken, level: NomadOutpostLevel) {
            Memory.set(MemoryKeys.NOMAD_OUTPOST_LEVEL, level, outpost)
        }

        fun getNextLevel(level: NomadOutpostLevel): NomadOutpostLevel {
            return when (level) {
                NomadOutpostLevel.CLAIM -> NomadOutpostLevel.SITE
                NomadOutpostLevel.SITE -> NomadOutpostLevel.OUTPOST
                NomadOutpostLevel.OUTPOST -> NomadOutpostLevel.OUTPOST
            }
        }
    }
    
    val outpost: SectorEntityToken

    private var done = false
    private var level: NomadOutpostLevel
        get() = getOutpostLevel(outpost)
        set(value) { setOutpostLevel(outpost, value) }

    private var matchedLevel: NomadOutpostLevel? = null

    init {
        val temp = createOutpost(system, faction)
        if (temp == null) done = true
        outpost = temp ?: system.createToken(0f, 0f)
        if (temp != null) system.addEntity(temp)
        val chance = Helper.random.nextFloat()
        level = if (chance < OUTPOST_CHANCE) NomadOutpostLevel.OUTPOST
        else if (chance < SITE_CHANCE) NomadOutpostLevel.SITE
        else NomadOutpostLevel.CLAIM
    }

    private fun createOutpost(system: StarSystemAPI, faction: String): SectorEntityToken? {
        val market = Global.getFactory().createMarket(
            RoiderIds.Entities.NOMAD_MARKET_ID + Misc.genUID(),
            ExternalStrings.NOMAD_BASE_ENTITY_NAME,
            3
        ) ?: return null
        market.isHidden = true
        market.surveyLevel = MarketAPI.SurveyLevel.FULL
        market.factionId = faction
        market.addCondition(Conditions.POPULATION_2)
        market.isPlanetConditionMarketOnly = false

        val weights = LinkedHashMap<BaseThemeGenerator.LocationType, Float>()
        weights[BaseThemeGenerator.LocationType.IN_ASTEROID_BELT] = 30f
        weights[BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD] = 30f
        weights[BaseThemeGenerator.LocationType.IN_RING] = 20f
        weights[BaseThemeGenerator.LocationType.IN_SMALL_NEBULA] = 20f
        weights[BaseThemeGenerator.LocationType.GAS_GIANT_ORBIT] = 10f
        weights[BaseThemeGenerator.LocationType.PLANET_ORBIT] = 10f
        weights[BaseThemeGenerator.LocationType.STAR_ORBIT] = 1f
        weights[BaseThemeGenerator.LocationType.NEAR_STAR] = 1f
        weights[BaseThemeGenerator.LocationType.OUTER_SYSTEM] = 1f
        val locs: WeightedRandomPicker<BaseThemeGenerator.EntityLocation> = BaseThemeGenerator.getLocations(
            null,
            system,
            null,
            100f,
            weights
        )
        val loc = locs.pick() ?: return null

        val added = BaseThemeGenerator.addNonSalvageEntity(
            system,
            loc,
            RoiderIds.Entities.NOMAD_OUTPOST,
            faction
        )
        if (added?.entity == null) return null

        val entity = added.entity

        BaseThemeGenerator.convertOrbitWithSpin(entity, -5f)
        market.primaryEntity = entity
        entity.market = market
        entity.sensorProfile = 1f
        entity.isDiscoverable = true
        entity.memoryWithoutUpdate.set(MemoryKeys.NOMAD_OUTPOST, true)
        Helper.addStationSensorProfile(entity)
        entity.addTag(RoiderTags.NOMAD_OUTPOST)
        entity.memoryWithoutUpdate["\$tradeMode"] = CampaignUIAPI.CoreUITradeMode.NONE
        market.econGroup = market.id
        market.memoryWithoutUpdate.set(DecivTracker.NO_DECIV_KEY, true)
        market.reapplyIndustries()

        entity.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.ORBITAL_CONSTRUCTION)

        return entity
    }
    
    override fun advance(amount: Float) {
        if (Helper.isSectorPaused) return
        if (done || outpost.isExpired || level == NomadOutpostLevel.OUTPOST) {
            done = true
            return
        }

        updateStationIfNeeded()
    }

    private fun updateStationIfNeeded() {
        if (matchedLevel == level) return
        matchedLevel = level

        val market = outpost.market
        var stationInd: Industry? = getStationIndustry(market)
        val currIndId = if (stationInd == null) {
            pickStationType()
        } else {
            market.removeIndustry(stationInd.id, null, false)
            stationInd.id
        }

        market.addIndustry(currIndId)
        stationInd = getStationIndustry(market) ?: return
        stationInd.finishBuildingOrUpgrading()

        removeStationModules()
    }

    private fun removeStationModules() {
        if (level == NomadOutpostLevel.OUTPOST) return

        val fleet: CampaignFleetAPI = Misc.getStationFleet(outpost) ?: return
        val members = fleet.fleetData.membersListCopy
        if (members.size < 1) return
        fleet.inflateIfNeeded()
        val station = members[0]
        val picker = WeightedRandomPicker<Int>(Helper.random)
        var index = 1 // index 0 is station body
        for (slotId in station.variant.moduleSlots) {
            val mv = station.variant.getModuleVariant(slotId)
            if (Misc.isActiveModule(mv)) {
                picker.add(index, 1f)
            }
            index++
        }
        val removeMult = when (level) {
            NomadOutpostLevel.CLAIM -> 0.67f
            NomadOutpostLevel.SITE -> 0.33f
            else -> 0f
        }
        var remove = (picker.items.size * removeMult).roundToInt()
        if (removeMult > 0) remove = max(remove, 1)
        while (remove > 0) {
            remove--
            val pick = picker.pickAndRemove()
            if (pick != null) {
                station.status.setHullFraction(pick, 0f)
                station.status.setDetached(pick, true)
                station.status.setPermaDetached(pick, true)
            }
        }
    }

    private fun getStationIndustry(market: MarketAPI): Industry? {
        for (curr in market.industries) {
            if (curr.spec.hasTag(Industries.TAG_STATION)) {
                return curr
            }
        }
        return null
    }

    private fun pickStationType(): String {
        val picker = WeightedRandomPicker<String>(Helper.random)
//        picker.add(RoiderIndustries.ORBITAL_STATION, 50f)
        picker.add(Industries.ORBITALSTATION, 10f)
//        picker.add(Industries.ORBITALSTATION_MID, 2f)
//        picker.add(Industries.ORBITALSTATION_HIGH, 1f)

        return picker.pick()
    }

    override fun isDone(): Boolean {
        return done
    }

    override fun runWhilePaused(): Boolean = false
}