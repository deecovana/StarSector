package roiderUnion.nomads.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.helpers.MarketHelper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderIds
import roiderUnion.ids.RoiderIndustries
import roiderUnion.helpers.Settings
import roiderUnion.ids.Aliases
import roiderUnion.nomads.NomadsData
import roiderUnion.nomads.NomadsHelper
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class NomadBaseLevelTracker(
    private val base: SectorEntityToken,
    level: NomadBaseLevel = NomadBaseLevel.STARTING
) : EveryFrameScript, FleetEventListener {
    companion object {
        fun alias(x: XStream) {
            val jClass = NomadBaseLevelTracker::class.java
            x.alias(Aliases.NLVLER, jClass)
            x.aliasAttribute(jClass, "upgradeTracker", "uT")
            x.aliasAttribute(jClass, "done", "d")
            x.aliasAttribute(jClass, "base", "b")
            x.aliasAttribute(jClass, "matchedLevel", "mL")
            x.aliasAttribute(jClass, "market", "m")
            x.aliasAttribute(jClass, "addedListenerTo", "l")
        }

        const val INTERVAL_MIN = 340f
        const val INTERVAL_MAX = 380f

        const val REMOVE_MULT = 1f / 3f

        const val WEIGHT_ROIDER = 50f
        const val WEIGHT_LOWTECH = 10f
        const val WEIGHT_MIDLINE = 10f
        const val WEIGHT_HIGHTECH = 5f

        val BASE_LEVEL_COUNTS = mapOf(
            Pair(NomadBaseLevel.BATTLESTATION, Settings.NOMAD_BASES_T3),
            Pair(NomadBaseLevel.SHIPWORKS, Settings.NOMAD_BASES_T4),
            Pair(NomadBaseLevel.HQ, Settings.NOMAD_BASES_T5)
        )

        fun getLevel(base: SectorEntityToken): NomadBaseLevel {
            return Memory.get(MemoryKeys.NOMAD_BASE_LEVEL, base, { it is NomadBaseLevel }, { NomadBaseLevel.STARTING }) as NomadBaseLevel
        }

        fun setLevel(base: SectorEntityToken, level: NomadBaseLevel) {
            Memory.set(MemoryKeys.NOMAD_BASE_LEVEL, level, base)
        }

        fun getNextLevel(level: NomadBaseLevel): NomadBaseLevel {
            val next = max(level.ordinal + 1, NomadBaseLevel.values().size - 1)
            return NomadBaseLevel.values()[next]
        }
    }

    private val upgradeTracker = IntervalUtil(INTERVAL_MIN, INTERVAL_MAX)

    private var done = false
    private var level: NomadBaseLevel
        get() = getLevel(base)
        set(value) {
            setLevel(base, value)
        }

    private var matchedLevel: NomadBaseLevel? = null
    private val market = base.market
    private var addedListenerTo: CampaignFleetAPI? = null

    init {
        this.level = level
        updateIfNeeded()
    }

    override fun advance(amount: Float) {
        if (Helper.isSectorPaused) return
        if (done || base.isExpired || matchedLevel == NomadBaseLevel.CAPITAL) {
            val nomads = Memory.get(MemoryKeys.NOMAD_GROUP, base, { it is NomadsData }, { NomadsData("", "") }) as NomadsData
            NomadsHelper.activeGroups.remove(nomads)
            done = true
            return
        }

        val fleet = Misc.getStationFleet(market)
        if (addedListenerTo !== fleet) {
            addedListenerTo?.removeEventListener(this)
            fleet?.addEventListener(this)
            addedListenerTo = fleet
        }

        upgradeTracker.advance(Misc.getDays(amount))
        if (upgradeTracker.intervalElapsed()) {
            val canLevelTo2 = canLevelTo(NomadBaseLevel.ESTABLISHED)
            val canLevelTo3 = canLevelTo(NomadBaseLevel.BATTLESTATION)
            val canLevelTo4 = canLevelTo(NomadBaseLevel.SHIPWORKS)
            val canLevelTo5 = canLevelTo(NomadBaseLevel.HQ)
            val canLevelTo6 = Memory.isFlag(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, base)
            val canLevelUp = canLevelTo2 || canLevelTo3 || canLevelTo4 || canLevelTo5 || canLevelTo6
            if (canLevelUp) level = getNextLevel(level)
        }

        updateIfNeeded()
    }

    private fun canLevelTo(tLevel: NomadBaseLevel): Boolean {
        if (level.ordinal + 1 != tLevel.ordinal || tLevel == NomadBaseLevel.ESTABLISHED) return false
        val count = NomadsHelper.bases.count { entity ->
            Memory.getNullable(MemoryKeys.NOMAD_BASE_LEVEL, entity, { it is NomadBaseLevel }, { null }) == tLevel
        }
        return count < BASE_LEVEL_COUNTS[tLevel]!!
    }

    private fun updateIfNeeded() {
        if (matchedLevel == level) return
        matchedLevel = level
        updateIndustries()
        updateStation()
        market.reapplyIndustries()
    }

    private fun updateIndustries() {
        when (level) {
            NomadBaseLevel.STARTING -> {
                Helper.setPopulation(market, 3)
                MarketHelper.addIndustry(market, Industries.GROUNDDEFENSES)
                MarketHelper.addIndustry(market, RoiderIndustries.DIVES)
                MarketHelper.addIndustry(market, Industries.REFINING)
                addNomadBaseInd()
            }
            NomadBaseLevel.ESTABLISHED -> {
                Helper.setPopulation(market, 3)
                MarketHelper.replaceIndustry(market, Industries.GROUNDDEFENSES, Industries.HEAVYBATTERIES)
                MarketHelper.addIndustry(market, Industries.PATROLHQ)
                MarketHelper.addIndustry(market, RoiderIndustries.DIVES)
                MarketHelper.addIndustry(market, Industries.REFINING)
                addNomadBaseInd()
            }
            NomadBaseLevel.BATTLESTATION -> {
                Helper.setPopulation(market, 3)
                MarketHelper.replaceIndustry(market, Industries.GROUNDDEFENSES, Industries.HEAVYBATTERIES)
                MarketHelper.addIndustry(market, Industries.PATROLHQ)
                MarketHelper.addIndustry(market, RoiderIndustries.DIVES)
                MarketHelper.addIndustry(market, Industries.REFINING)
                addNomadBaseInd()
            }
            NomadBaseLevel.SHIPWORKS -> {
                Helper.setPopulation(market, 3)
                MarketHelper.replaceIndustry(market, Industries.GROUNDDEFENSES, Industries.HEAVYBATTERIES)
                MarketHelper.addIndustry(market, Industries.PATROLHQ)
                MarketHelper.addIndustry(market, RoiderIndustries.DIVES)
                MarketHelper.addIndustry(market, Industries.REFINING)
                MarketHelper.addIndustry(market, RoiderIndustries.SHIPWORKS)
                addNomadBaseInd()
            }
            NomadBaseLevel.HQ -> {
                Helper.setPopulation(market, 4)
                MarketHelper.replaceIndustry(market, Industries.GROUNDDEFENSES, Industries.HEAVYBATTERIES)
                market.removeIndustry(Industries.PATROLHQ, null, false)
                MarketHelper.replaceIndustry(market, RoiderIndustries.DIVES, RoiderIndustries.UNION_HQ)
                MarketHelper.addIndustry(market, Industries.REFINING)
                MarketHelper.addIndustry(market, RoiderIndustries.SHIPWORKS)
                addNomadBaseInd()
//                removeNomadBaseInd()
            }
            NomadBaseLevel.CAPITAL -> {
                Helper.setPopulation(market, 4)
                MarketHelper.replaceIndustry(market, Industries.GROUNDDEFENSES, Industries.HEAVYBATTERIES)
                market.removeIndustry(Industries.PATROLHQ, null, false)
                MarketHelper.replaceIndustry(market, RoiderIndustries.DIVES, RoiderIndustries.UNION_HQ)
                MarketHelper.addIndustry(market, Industries.REFINING)
                if (!market.hasIndustry(Industries.ORBITALWORKS)) {
                    MarketHelper.replaceIndustry(market, RoiderIndustries.SHIPWORKS, Industries.HEAVYINDUSTRY)
                    MarketHelper.upgradeIndustry(market, Industries.HEAVYINDUSTRY, false)
                } else {
                    MarketHelper.addIndustry(market, Industries.ORBITALWORKS)
                }
                removeNomadBaseInd()
            }
        }
    }

    private fun addNomadBaseInd() {
        MarketHelper.addIndustry(market, RoiderIndustries.NOMAD_BASE)
        market.addSubmarket(RoiderIds.Roider_Submarkets.NOMAD_MARKET)
        market.removeSubmarket(Submarkets.SUBMARKET_OPEN)
        market.removeSubmarket(Submarkets.SUBMARKET_BLACK)
    }

    private fun removeNomadBaseInd() {
        market.removeIndustry(RoiderIndustries.NOMAD_BASE, null, false)
        market.removeSubmarket(RoiderIds.Roider_Submarkets.NOMAD_MARKET)
        market.addSubmarket(Submarkets.SUBMARKET_OPEN)
        market.addSubmarket(Submarkets.SUBMARKET_BLACK)
    }

    private fun updateStation() {
        // Need to switch to Roider station if on-track to capital

        var stationInd = Misc.getStationIndustry(market)
        if (stationInd == null) {
            MarketHelper.addIndustry(market, pickStationType())
            stationInd = Misc.getStationIndustry(market)
        }

        val isOrbital = stationInd.spec?.downgrade == null
        when (level) {
            NomadBaseLevel.STARTING -> {
                removeStationModules()
            }
            NomadBaseLevel.ESTABLISHED -> {}
            NomadBaseLevel.BATTLESTATION -> {
                if (isOrbital) MarketHelper.replaceIndustry(market, stationInd.id, stationInd.spec?.upgrade)
            }
            NomadBaseLevel.SHIPWORKS -> {
                if (isOrbital) MarketHelper.replaceIndustry(market, stationInd.id, stationInd.spec?.upgrade)
            }
            NomadBaseLevel.HQ -> {
                if (isOrbital) MarketHelper.replaceIndustry(market, stationInd.id, stationInd.spec?.upgrade)
            }
            NomadBaseLevel.CAPITAL -> {
                if (isOrbital) MarketHelper.replaceIndustry(market, stationInd.id, stationInd.spec?.upgrade)
                MarketHelper.replaceIndustry(market, stationInd.id, stationInd.spec?.upgrade)
            }
        }

    }

    private fun removeStationModules() {
        if (level != NomadBaseLevel.STARTING) return

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
        var remove = (picker.items.size * REMOVE_MULT).roundToInt()
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

    private fun pickStationType(): String {
        val picker = WeightedRandomPicker<String>(Helper.random)
        picker.add(RoiderIndustries.ORBITAL_STATION, WEIGHT_ROIDER)
        picker.add(Industries.ORBITALSTATION, WEIGHT_LOWTECH)
        picker.add(Industries.ORBITALSTATION_MID, WEIGHT_MIDLINE)
        picker.add(Industries.ORBITALSTATION_HIGH, WEIGHT_HIGHTECH)

        return picker.pick()
    }

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        Misc.fadeAndExpire(base)
        Helper.sector?.economy?.removeMarket(base.market)
        NomadsHelper.bases -= base
        PirateBaseManager.markRecentlyUsedForBase(base.starSystem)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}
}