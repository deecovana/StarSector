package roiderUnion.nomads.old.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class NomadBaseLevelTracker(val base: SectorEntityToken) : EveryFrameScript {

    companion object {
        const val DISPOSABLE_MINER_WEIGHT = 5f

        fun getBaseLevel(base: SectorEntityToken): NomadBaseLevel {
            return Memory.get(MemoryKeys.NOMAD_BASE_LEVEL, base, { it is NomadBaseLevel }, { NomadBaseLevel.BASIC }) as NomadBaseLevel
        }

        fun setBaseLevel(base: SectorEntityToken, level: NomadBaseLevel) {
            Memory.set(MemoryKeys.NOMAD_BASE_LEVEL, level, base)
        }

        fun getNextLevel(level: NomadBaseLevel): NomadBaseLevel {
            return when (level) {
                NomadBaseLevel.BASIC -> NomadBaseLevel.ORBITAL
                NomadBaseLevel.ORBITAL -> NomadBaseLevel.BATTLESTATION
                NomadBaseLevel.BATTLESTATION -> NomadBaseLevel.STAR_FORT
                else -> NomadBaseLevel.STAR_FORT
            }
        }
    }

    private var done = false
    private var level: NomadBaseLevel
        get() = getBaseLevel(base)
        set(value) { setBaseLevel(base, value) }

    private var matchedLevel: NomadBaseLevel? = null
    private val market = base.market

    private val sourceKey = MemoryKeys.MINER_SOURCE + base.id
    private val factionsKey = MemoryKeys.MINER_FACTION_WEIGHTS + base.id

    init {
        if (!base.memoryWithoutUpdate.contains(MemoryKeys.NOMAD_BASE_LEVEL)) level = NomadBaseLevel.BASIC
    }

    override fun advance(amount: Float) {
        if (Helper.isSectorPaused) return
        if (done || base.isExpired || level == NomadBaseLevel.STAR_FORT) {
            done = true
            removeMiningFleets()
            return
        }

        updateStationIfNeeded()
    }

    private fun updateStationIfNeeded() {
        if (matchedLevel == level) return
        matchedLevel = level

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

        updateMiningFleets()
    }

    private fun updateMiningFleets() {
        Memory.setFlag(MemoryKeys.MINER_SOURCE, MemoryKeys.NOMAD_BASE, market)
        Memory.set(sourceKey, DISPOSABLE_MINER_WEIGHT, base.starSystem)
        val factionWeights = mutableMapOf<String, Float>()
        factionWeights[Factions.INDEPENDENT] = 1f
        factionWeights[Factions.PIRATES] = 1f
        factionWeights[base.faction.id] = DISPOSABLE_MINER_WEIGHT
        Memory.set(factionsKey, factionWeights, base.starSystem)
    }

    private fun removeMiningFleets() {
        Helper.sector?.starSystems?.forEach {
            Memory.unset(sourceKey, it)
            Memory.unset(factionsKey, it)
        }
        Memory.unsetFlag(MemoryKeys.MINER_SOURCE, MemoryKeys.NOMAD_BASE, market)
    }

    private fun removeStationModules() {
        if (level != NomadBaseLevel.BASIC) return

        val fleet: CampaignFleetAPI = Misc.getStationFleet(base) ?: return
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
        val removeMult = if (level == NomadBaseLevel.BASIC) 0.33f else 0f
        var remove = (picker.items.size * removeMult).roundToInt()
        if (removeMult > 0) remove = max(remove, 1)
        remove = min(remove, picker.items.size - 2)
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
        picker.add(Industries.ORBITALSTATION_MID, 2f)
        picker.add(Industries.ORBITALSTATION_HIGH, 1f)

        return picker.pick()
    }

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false
}