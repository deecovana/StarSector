package roiderUnion.helpers

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.cleanup.TokenCleaner
import roiderUnion.fleets.mining.RoiderMiningRouteManager
import roiderUnion.ids.MemoryKeys

object MiningHelper {
    val MINING_COMMODITIES = listOf(
        Commodities.ORE,
        Commodities.RARE_ORE,
        Commodities.ORGANICS,
        Commodities.VOLATILES
    )

    const val WEIGHT_EXTREME = 15f
    const val WEIGHT_HIGH = 8f
    const val WEIGHT_MID = 4f
    const val WEIGHT_LOW = 2f
    const val WEIGHT_MIN = 1f

    const val RANK_GRAND = 5f
    const val RANK_HIGH = 4f
    const val RANK_MID = 3f
    const val RANK_LOW = 2f
    const val RANK_MIN = 1f

    val UNION_HQ_RANK = RANK_HIGH
    val UNION_HQ_REMOTE_RANK = RANK_LOW
    val DIVES_RANK = RANK_LOW
    val DIVES_REMOTE_RANK = RANK_MIN

    const val MINING_STOP_BASE_DAYS = 10f

    class MiningData(
        val system: StarSystemAPI,
        val resources: List<String>
    )

    fun canMine(system: StarSystemAPI): Boolean {
        if (Memory.isFlag(MemoryKeys.NO_MINING, system)) return false
        return true
    }

    fun isMiningSpot(entity: SectorEntityToken?): Boolean = Memory.isFlag(MemoryKeys.MINING_SPOT, entity)

    fun getMiningResources(system: LocationAPI): List<String> {
        val result = mutableListOf<String>()
        for (com in MINING_COMMODITIES) {
            if (Memory.contains(MemoryKeys.VOID_RESOURCE + com, system)) result += com
        }
        return result
    }

    fun getMiningResourceConditions(system: LocationAPI): List<String> {
        val result = mutableListOf<String>()
        for (com in MINING_COMMODITIES) {
            result += Memory.getNullable(MemoryKeys.VOID_RESOURCE + com, system, { it is String? }, { null }) as? String ?: continue
        }
        return result
    }

    fun pickMiningFaction(weights: Map<String, Float>): String? {
        val picker = WeightedRandomPicker<String>()
        for (faction in weights.keys) {
            picker.add(faction, weights[faction] ?: 0f)
        }
        return if (picker.isEmpty) null else picker.pick()
    }

    fun setMiningRank(id: String, loc: LocationAPI?, rank: Float) {
        setMiningRank(id, loc?.memoryWithoutUpdate, rank)
    }

    fun setMiningRank(id: String, memory: MemoryAPI?, rank: Float) {
        Memory.set(MemoryKeys.MINER_SOURCE + id, rank, memory)
    }

    fun getMiningRank(loc: LocationAPI): Float = getMiningRank(loc.memoryWithoutUpdate)

    fun getMiningRank(memory: MemoryAPI): Float {
        var result = 0f
        for (memKey in memory.keys) {
            if (memKey.startsWith(MemoryKeys.MINER_SOURCE)) {
                val r = Memory.get(memKey, memory, { true }, { 0 }) as? Float ?: 0f
                if (r > result) result = r
            }
        }
        return result
    }

    fun unsetMiningRank(id: String, loc: LocationAPI?) {
        unsetMiningRank(id, loc?.memoryWithoutUpdate)
    }

    fun unsetMiningRank(id: String, memory: MemoryAPI?) {
        Memory.unset(MemoryKeys.MINER_SOURCE + id, memory)
    }

    fun setMiningWeights(id: String, loc: LocationAPI?, vararg weights: Pair<String, Float>) {
        setMiningWeights(id, loc?.memoryWithoutUpdate, *weights)
    }

    fun setMiningWeights(id: String, memory: MemoryAPI?, vararg weights: Pair<String, Float>) {
        val factionWeights = mutableMapOf<String, Float>()
        factionWeights.putAll(weights)
        Memory.set(MemoryKeys.MINER_FACTION_WEIGHTS + id, factionWeights, memory)
    }

    fun getMiningWeights(loc: LocationAPI?): Map<String, Float> = getMiningWeights(loc?.memoryWithoutUpdate)

    fun getMiningWeights(memory: MemoryAPI?): Map<String, Float> {
        if (memory == null) return emptyMap()
        val result = mutableMapOf<String, Float>()
        memory.keys
            .filter { it.startsWith(MemoryKeys.MINER_FACTION_WEIGHTS) }
            .forEach {
                val w: Map<String, Float> = memory.get(it) as Map<String, Float>
                for (faction in w.keys) {
                    var weight = result[faction] ?: 0f
                    weight += w[faction]!!
                    result[faction] = weight
                }
            }
        return result
    }

    fun unsetMiningWeights(id: String, loc: LocationAPI?) {
        unsetMiningWeights(id, loc?.memoryWithoutUpdate)
    }

    fun unsetMiningWeights(id: String, memory: MemoryAPI?) {
        Memory.unset(MemoryKeys.MINER_FACTION_WEIGHTS + id, memory)
    }

    fun setMiningDist(id: String, entity: SectorEntityToken?, range: Float) {
        setMiningDist(id, entity?.memoryWithoutUpdate, range)
    }

    fun setMiningDist(id: String, memory: MemoryAPI?, range: Float) {
        Memory.set(MemoryKeys.MINER_RANGE + id, range, memory)
    }

    fun getMiningDist(entity: SectorEntityToken?): Float {
        return getMiningDist(entity?.memory)
    }

    fun getMiningDist(memory: MemoryAPI?): Float {
        if (memory == null) return 0f
        var result = 0f
        memory.keys
            .filter { it.startsWith(MemoryKeys.MINER_RANGE) }
            .forEach {
                val range = memory[it] as? Float ?: 0f
                if (range > result) result = range
            }
        return result
    }

    fun unsetMiningDist(id: String, entity: SectorEntityToken?) {
        unsetMiningDist(id, entity?.memory)
    }

    fun unsetMiningDist(id: String, memory: MemoryAPI?) {
        Memory.unset(MemoryKeys.MINER_RANGE + id, memory)
    }

    fun inMiningRange(loc: LocationAPI, source: SectorEntityToken): Boolean {
        return Misc.getDistanceLY(loc.location, source.locationInHyperspace) <= getMiningDist(source)
    }

    fun addMiningStop(route: RouteManager.RouteData, source: MarketAPI, dest: StarSystemAPI, index: Int): SectorEntityToken? {
        val loc = pickMiningLocation(route, dest) ?: return null
        val token = dest.createToken(loc.location ?: loc.orbit.computeCurrentLocation()) ?: return null
        Helper.setCircularOrbit(token, loc.orbit)
        Memory.set(MemoryKeys.MINING_SPOT, true, token)
        token.addScript(TokenCleaner(token) { route.isExpired })
        val name = if (loc.type == BaseThemeGenerator.LocationType.IN_SMALL_NEBULA) {
            ExternalStrings.MINING_CLOUD
        } else {
            ExternalStrings.MINING_SPOT
        }
        Memory.set(MemoryKeys.TOKEN_NAME, name, token)
        dest.addEntity(token)
        val miningTime = Helper.multHalfToFull(MINING_STOP_BASE_DAYS, route.random)
        route.addSegment(RouteManager.RouteSegment(index, Helper.getTravelDays(source.primaryEntity, token), source.primaryEntity, token, loc.type))
        route.addSegment(RouteManager.RouteSegment(index + 1, miningTime, token, null, loc.type))
        return token
    }

    fun pickMiningLocation(route: RouteManager.RouteData, system: StarSystemAPI): BaseThemeGenerator.EntityLocation? {
        val types = linkedMapOf<BaseThemeGenerator.LocationType, Float>()
        val resources = getMiningResources(system)
        if (resources.contains(Commodities.ORE) || resources.contains(Commodities.RARE_ORE)) {
            types += Pair(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 1f)
            types += Pair(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 1f)
        }
        if (resources.contains(Commodities.ORGANICS)) {
            types += Pair(BaseThemeGenerator.LocationType.IN_RING, 1f)
        }
        if (resources.contains(Commodities.VOLATILES)) {
            types += Pair(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 1f)
        }
        return BaseThemeGenerator.getLocations(route.random, system, RoiderMiningRouteManager.MIN_GAP, types).pick()
    }

    fun fillCargo(fleet: CampaignFleetAPI, system: StarSystemAPI, comIds: List<String>, maxFillPercent: Float) {
        val locRez = getMiningResources(system)
        val added = comIds.filter { locRez.contains(it) }
        added.forEach {
            val amount = fleet.cargo.maxCapacity * maxFillPercent / added.size
            CargoHelper.addCommodity(it, amount, fleet.cargo)
        }
    }

}