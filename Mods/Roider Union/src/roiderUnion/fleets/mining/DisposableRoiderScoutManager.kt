package roiderUnion.fleets.mining

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.fleets.DisposableFleetManager
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.*
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes

class DisposableRoiderScoutManager : DisposableFleetManager() {
    companion object {
        const val ROUTE_ID = "roider_miningScouts"
        const val BASE_FLEET_FP = 5f
        const val MAX_BONUS_FLEET_FP = 10f
        const val MIN_FLEETS = 0
        const val MAX_FLEETS = 4
        const val TOKEN_PLACE = "\$place"
        const val CARGO_PERCENT = 0.1f
    }

    override fun getSpawnId(): String = ROUTE_ID

    override fun getDesiredNumFleetsForSpawnLocation(): Int {
        val loc: StarSystemAPI = getCurrSpawnLoc() ?: return 0
        if (Helper.isSpecialSystem(loc)) return 0
        var desiredNumFleets = MIN_FLEETS
        desiredNumFleets += MiningHelper.getMiningRank(loc).toInt()
        desiredNumFleets += MiningHelper.getMiningWeights(loc).size
        return desiredNumFleets.coerceAtMost(MAX_FLEETS)
    }

    override fun spawnFleetImpl(): CampaignFleetAPI? {
        val loc: StarSystemAPI = getCurrSpawnLoc() ?: return null
        val weightBonuses = MiningHelper.getMiningWeights(loc)
        val faction = MiningHelper.pickMiningFaction(weightBonuses) ?: return null
        val weightBonus = weightBonuses[faction] ?: 0f
        val fp = BASE_FLEET_FP + Misc.random.nextFloat() * MAX_BONUS_FLEET_FP + weightBonus
        val weights = mapOf(
                Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID),
                Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW)
            )
        val type = RoiderFleetTypes.MINER
        val params = FleetsHelper.createFleetParams(
            Helper.random,
            weights,
            fp,
            0f,
            null,
            null,
            loc.location,
            faction,
            type
        )
        params.ignoreMarketFleetSizeMult = true
        val factionParams = FleetsHelper.getStandardFactionParams(faction)
        val fleet = FleetsHelper.createMultifactionFleet(
            params,
            faction,
            *factionParams
        )
        fleet.setFaction(faction, true)
        fleet.name = Helper.sector?.getFaction(faction)?.getFleetTypeName(type) ?: ExternalStrings.DEBUG_NULL
        fleet.commander.postId = Ranks.POST_FLEET_COMMANDER
        fleet.commander.rankId = Ranks.SPACE_CAPTAIN
        if (Misc.isPirateFaction(fleet.faction)) {
            Memory.setFlag(MemFlags.MEMORY_KEY_PIRATE, fleet.id, fleet)
            Memory.setFlag(MemFlags.MEMORY_KEY_NO_REP_IMPACT, fleet.id, fleet)
        } else if (faction != RoiderFactions.ROIDER_UNION) {
            Memory.setFlag(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, fleet.id, fleet)
        }
        Memory.setFlag(MemFlags.FLEET_NO_MILITARY_RESPONSE, fleet.id, fleet)
        if (desiredNumFleetsForSpawnLocation == 1) {
            setLocationAndOrders(fleet, 1f, 0f)
        } else {
            setLocationAndOrders(fleet, 0.1f, 0f)
        }
        MiningHelper.fillCargo(
            fleet,
            loc,
            listOf(Commodities.ORE, Commodities.RARE_ORE, Commodities.ORGANICS, Commodities.VOLATILES),
            CARGO_PERCENT
        )
        return fleet
    }

    override fun getActionOutsideText(system: StarSystemAPI?, fleet: CampaignFleetAPI?): String = ExternalStrings.MINING_SCOUT_HYPER.replace(
        TOKEN_PLACE, system?.nameWithLowercaseType ?: ExternalStrings.DEBUG_NULL)

    override fun getActionInsideText(system: StarSystemAPI?, fleet: CampaignFleetAPI?): String = ExternalStrings.MINING_SCOUT_LOCAL.replace(
        TOKEN_PLACE, system?.nameWithLowercaseType ?: ExternalStrings.DEBUG_NULL)
}