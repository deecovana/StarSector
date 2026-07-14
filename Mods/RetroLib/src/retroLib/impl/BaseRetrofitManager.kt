package retroLib.impl

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import retroLib.*
import retroLib.api.RetrofitFilter
import retroLib.api.RetrofitManager

open class BaseRetrofitManager(protected val market: MarketAPI?, protected val faction: FactionAPI, filter: RetrofitFilter) : RetrofitManager {
    override val retrofits: List<RetrofitData> = RetrofitsKeeper.getRetrofits(filter)

    override fun getAllTargets(): List<FleetMemberAPI> {
        val targets = getAvailableTargets() + getUnavailableTargets() + getIllegalTargets() + getNotAllowedTargets()
        return targets.distinctBy { it.specId }.sortedBy { it.hullSpec.hullName }.sortedByDescending { it.hullSpec.hullSize }
    }

    override fun getAvailableTargets(): List<FleetMemberAPI> {
        val result = mutableListOf<FleetMemberAPI>()
        val playerFleet = Helper.sector?.playerFleet?.membersWithFightersCopy ?: listOf()
        val playerWingIds = playerFleet.filter { it.isFighterWing }.map { it.specId }
        result.addAll(retrofits.asSequence()
            .filter { isTargetShipAvailable(it, playerFleet) }
            .filter { isTargetAllowed(it.target) && isTargetLegal(it.target) }
            .mapNotNull { Helper.createShip(it.target) }
        )
        result.addAll(retrofits
            .filter { playerWingIds.contains(it.source) }
            .filter { isTargetAllowed(it.target) && isTargetLegal(it.target) }
            .mapNotNull { Helper.createWing(it.target) }
        )
        return result
            .distinctBy { it.specId }
            .sortedBy { it.hullSpec.hullName }
            .sortedByDescending { it.hullSpec.hullSize }
    }

    protected fun isTargetShipAvailable(data: RetrofitData, playerFleet: List<FleetMemberAPI>): Boolean {
        val playerHullIds = playerFleet.map { it.hullId } + playerFleet.mapNotNull { it.hullSpec?.baseHullId }
        val playerShipSizes = playerFleet.mapNotNull { it.hullSpec?.hullSize }.toSet()
        val largestFrame = getLargestFrameSize(playerFleet)
        return playerHullIds.contains(data.source)
                || (!data.tags.contains(RetroLib_Tags.FIGHTER_WING) && data.targetSpec.hullSize <= largestFrame)
                || (Helper.isFrameHull(data.targetSpec) && playerShipSizes.contains(data.targetSpec.hullSize))
    }

    protected fun getLargestFrameSize(fleet: List<FleetMemberAPI>): HullSize {
        var result = HullSize.FIGHTER
        fleet.filter { Helper.isFrameHull(it) }.forEach {
            if (it.hullSpec.hullSize > result) result = it.hullSpec.hullSize
            if (result == HullSize.CAPITAL_SHIP) return result
        }
        return result
    }

    override fun getUnavailableTargets(): List<FleetMemberAPI> {
        val result = mutableListOf<FleetMemberAPI>()
        val ships =  retrofits.asSequence()
            .filter { isTargetAllowed(it.target) && isTargetLegal(it.target) }
            .filterNot { it.tags.contains(RetroLib_Tags.FIGHTER_WING) }
            .map { it.targetSpec }
            .distinct()
            .toList()
            .mapNotNull { Helper.createShip(it.hullId) }
        val wings = retrofits.asSequence()
            .filter { isTargetAllowed(it.target) && isTargetLegal(it.target) }
            .filter { it.tags.contains(RetroLib_Tags.FIGHTER_WING) }
            .mapNotNull { it.targetWingSpec }
            .distinct()
            .toList()
            .mapNotNull { Helper.createWing(it.id) }
        result += ships + wings
        val available = getAvailableTargets()
        result.removeAll { available.map { m -> m.specId }.contains(it.specId) }
        return result.sortedBy { it.hullSpec.hullName }.sortedByDescending { it.hullSpec.hullSize }
    }

    override fun getNotAllowedTargets(): List<FleetMemberAPI> {
        val ships =  retrofits.asSequence()
            .filter { !isTargetAllowed(it.target) }
            .filterNot { it.tags.contains(RetroLib_Tags.FIGHTER_WING) }
            .map { it.targetSpec }
            .distinct()
            .toList()
            .mapNotNull { Helper.createShip(it.hullId) }
        val wings = retrofits.asSequence()
            .filter { !isTargetAllowed(it.target) }
            .filter { it.tags.contains(RetroLib_Tags.FIGHTER_WING) }
            .mapNotNull { it.targetWingSpec }
            .distinct()
            .toList()
            .mapNotNull { Helper.createWing(it.id) }
        return (ships + wings).sortedBy { it.hullSpec.hullName }.sortedByDescending { it.hullSpec.hullSize }
    }

    override fun getIllegalTargets(): List<FleetMemberAPI> {
        val ships =  retrofits.asSequence()
            .filter { isTargetAllowed(it.target) && !isTargetLegal(it.target) }
            .filterNot { it.tags.contains(RetroLib_Tags.FIGHTER_WING) }
            .map { it.targetSpec }
            .distinct()
            .toList()
            .mapNotNull { Helper.createShip(it.hullId) }
        val wings = retrofits.asSequence()
            .filter { isTargetAllowed(it.target) && !isTargetLegal(it.target) }
            .filter { it.tags.contains(RetroLib_Tags.FIGHTER_WING) }
            .mapNotNull { it.targetWingSpec }
            .distinct()
            .toList()
            .mapNotNull { Helper.createWing(it.id) }
        return (ships + wings).sortedBy { it.hullSpec.hullName }.sortedByDescending { it.hullSpec.hullSize }
    }

    override fun getQueuedData(queue: List<FleetMemberAPI>, target: FleetMemberAPI?): List<RetrofitData> {
        val result = mutableListOf<RetrofitData>()
        val sources = getSourcesData(target)
        for (ship in queue) {
            result += sources.firstOrNull { ship.hullId == it.source || ship.hullSpec.baseHullId == it.source } ?: continue
        }
        return result
    }

    override fun getSourcesData(target: FleetMemberAPI?): List<RetrofitData> {
        if (target == null) return emptyList()
        val result = mutableListOf<RetrofitData>()
        val matched = mutableSetOf("")
        retrofits.forEach {
            if (it.target == target.hullId && !isMatched(it.sourceSpec, matched)) {
                result += it
                matched += it.source
                if (it.sourceSpec != null) matched += it.sourceSpec!!.hullName
            }
        }
        if (Helper.isFrameHull(target)) result.addAll(getFrameSourcesData(matched, target.hullId, target.hullSpec.hullSize))
        else if (target.isFighterWing)  result.addAll(getFighterSourcesData(matched, target.specId))
        else result.addAll(getFramesData(matched, target.hullSpec))
        val playerShips = Helper.sector?.playerFleet?.membersWithFightersCopy?.map { it.hullSpec } ?: listOf()
        for (data in retrofits) {
            if (data.target != target.specId) continue
            for (spec in playerShips) {
                if (isMatched(spec, matched)) continue
                if (isSourceHull(spec, data.source)) {
                    result += RetrofitData(
                        data,
                        id = data.id + spec.hullId,
                        source = spec.hullId,
                        tags = data.tags + RetroLib_Tags.GENERATED,
                        cost = Helper.calculateCost(spec.hullId, data.target, market)
                    )
                    matched += spec.hullId
                    matched += spec.hullName
                }
            }
        }
        return result
    }

    protected open fun getFrameSourcesData(matched: MutableSet<String>, targetId: String, targetSize: HullSize): Collection<RetrofitData> {
        val result = mutableListOf<RetrofitData>()
        val frameData = retrofits.first { it.target == targetId }
        val playerShips = Helper.sector?.playerFleet?.membersWithFightersCopy?.map { it.hullSpec } ?: listOf()
        playerShips.forEach {
            if (!isMatched(it, matched)
                && it.hullId != targetId
                && !Settings.FRAME_SOURCES_BLACKLIST.contains(it.hullId)
                && it.hullSize == targetSize
            ) {
                result += RetrofitData(
                    frameData,
                    id = frameData.id + it.hullId,
                    source = it.hullId,
                    tags = frameData.tags + RetroLib_Tags.GENERATED,
                    cost = Helper.calculateCost(it.hullId, targetId, market)
                )
                matched += it.hullId
                matched += it.hullName
            }
        }
        return result
    }

    protected open fun getFighterSourcesData(matched: MutableSet<String>, targetId: String): Collection<RetrofitData> {
        val result = mutableListOf<RetrofitData>()
        retrofits.forEach {
            if (it.target == targetId && !matched.contains(it.source)) {
                result += it
                matched += it.source
            }
        }
        return result
    }

    protected open fun getFramesData(matched: MutableSet<String>, target: ShipHullSpecAPI): Collection<RetrofitData> {
        if (Settings.FRAME_TARGETS_BLACKLIST.contains(target.hullId)) return emptyList()
        val result = mutableListOf<RetrofitData>()
        val playerShips = Helper.sector?.playerFleet?.membersWithFightersCopy?.map { it.hullSpec } ?: listOf()
        playerShips.forEach {
            if (!isMatched(it, matched) && Helper.isFrameHull(it) && Helper.isHullSameOrBigger(it, target)) {
                result += RetrofitData(
                    it.hullId + target.hullId,
                    it.hullId,
                    target.hullId,
                    setOf(RetroLib_Tags.GENERATED),
                    Helper.calculateCost(it.hullId, target.hullId, market),
                    getFrameTime(it, target),
                    getFramesRepReq(target),
                    areFramesComLegal(target)
                )
                matched += it.hullId
            }
        }
        return result
    }

    protected fun isMatched(spec: ShipHullSpecAPI?, matched: MutableSet<String>): Boolean {
        if (spec == null) return false
        return matched.contains(spec.hullId) || matched.contains(spec.hullName)
    }

    protected open fun getFrameTime(frame: ShipHullSpecAPI, target: ShipHullSpecAPI): Double {
        return 0.0
    }

    override fun isSourceAllowed(data: RetrofitData): Boolean {
        if (Helper.isFrameHull(data.sourceSpec) && data.tags.contains(RetroLib_Tags.GENERATED)) return areFramesAllowed(data.targetSpec)
        return true
    }

    protected open fun isSourceLegal(data: RetrofitData): Boolean {
        return isSourceLegalRep(data) && isSourceLegalCom(data)
    }

    override fun isSourceLegalRep(data: RetrofitData): Boolean {
        if (Helper.isFrameHull(data.sourceSpec) && data.tags.contains(RetroLib_Tags.GENERATED)) return areFramesRepLegal(data.targetSpec)
        val level = faction.getRelationshipLevel(Helper.sector?.playerFaction) ?: return false
        return level.isAtWorst(data.reputation)
    }

    override fun isSourceLegalCom(data: RetrofitData): Boolean {
        if (Helper.isFrameHull(data.sourceSpec) && data.tags.contains(RetroLib_Tags.GENERATED)) return areFramesComLegal(data.targetSpec)
        return data.isLegalCom(faction)
    }

    override fun isTargetAllowed(targetId: String?): Boolean {
        return true
    }

    override fun isTargetLegal(targetId: String?): Boolean {
        val level = faction.getRelationshipLevel(Helper.sector?.playerFaction) ?: return false
        return level.isAtWorst(getTargetRep(targetId)) && (Helper.hasCommission(faction.id) || !isTargetCom(targetId))
    }

    override fun getTargetRep(targetId: String?): RepLevel {
        var lowest: RepLevel = RepLevel.COOPERATIVE
        retrofits.filter { it.target == targetId }
            .forEach { if (it.reputation.isAtBest(lowest)) lowest = it.reputation }
        return lowest
    }

    override fun isTargetCom(targetId: String?): Boolean {
        return retrofits.filter { it.target == targetId }.none { !it.commission }
    }

    protected fun isSourceHull(spec: ShipHullSpecAPI, source: String): Boolean {
        if (isFrameSource(source)) return false
        return source == spec.hullId || source == spec.baseHullId
    }

    protected fun isSpeccedSourceHull(hullId: String, source: String): Boolean {
        if (isFrameSource(source)) return false
        return source == hullId
    }

    protected fun isFrameSource(source: String): Boolean = source.isEmpty()

    protected open fun areFramesRepLegal(target: ShipHullSpecAPI): Boolean {
        val level = faction.getRelationshipLevel(Helper.sector?.playerFaction) ?: return false
        return level.isAtWorst(getFramesRepReq(target))
    }

    protected open fun getFramesRepReq(target: ShipHullSpecAPI): RepLevel {
        var highest: RepLevel = RepLevel.VENGEFUL
        retrofits.filter { it.target == target.hullId }
            .filterNot { Helper.getGivenFrameHull(it.tags) != null }
            .forEach { if (it.reputation.isAtWorst(highest)) highest = it.reputation }
        return highest
    }

    protected open fun areFramesComLegal(target: ShipHullSpecAPI): Boolean {
        return retrofits.filter { it.target == target.hullId }
            .filterNot { Helper.getGivenFrameHull(it.tags) != null }
            .none { it.commission }
    }

    protected open fun areFramesAllowed(target: ShipHullSpecAPI): Boolean = true

    override fun getAvailableSourceShips(target: FleetMemberAPI?): List<FleetMemberAPI> {
        if (target == null) return emptyList()
        if (target.isFighterWing) return getAvailableFighterSources(target)
        if (Helper.isFrameHull(target)) return getAvailableFrameSources(target.hullSpec)
        val sourceIds = retrofits.filter {
            isSourceHull(target.hullSpec, it.target) && isSourceLegal(it) && isSourceAllowed(it)
        }.map { it.source }
        val pool = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null)
        Helper.sector?.playerFleet?.fleetData?.membersListCopy
            ?.filterNot { it.hullId == target.hullId }
            ?.filter { sourceIds.any { id -> isSourceHull(it.hullSpec, id) } }
            ?.forEach { pool.fleetData.addFleetMember(it) }
        if (isTargetLegal(target.hullId) && isTargetAllowed(target.hullId) && !Settings.FRAME_TARGETS_BLACKLIST.contains(target.hullId)) {
            Helper.sector?.playerFleet?.fleetData?.membersListCopy
                ?.filter { Helper.isFrameHull(it) && Helper.isHullSameOrBigger(it, target) }
                ?.forEach { pool.fleetData.addFleetMember(it) }
        }
        pool.fleetData.sort()
        return pool.fleetData.membersListCopy
    }

    protected open fun getAvailableFighterSources(target: FleetMemberAPI): List<FleetMemberAPI> {
        val result = mutableListOf<FleetMemberAPI>()
        val fighters = Helper.sector?.playerFleet?.cargo?.fighters ?: emptyList()
        for (data in retrofits) {
            if (data.target != target.specId) continue
            result.addAll(fighters.filter { data.source == it.item }.mapNotNull { Helper.createWing(it.item) })
        }
        return result
    }

    protected open fun getAvailableFrameSources(target: ShipHullSpecAPI): List<FleetMemberAPI> {
        if (Settings.FRAME_TARGETS_BLACKLIST.contains(target.hullId)) return emptyList()
        val results = Helper.sector?.playerFleet?.fleetData?.membersListCopy
            ?.filter { !Helper.isFrameHull(it) && it.hullSpec.hullSize == target.hullSize }?.toMutableList()
            ?: mutableListOf()
        // Maybe reenable but only allow different frame types?
//        results.addAll(Helper.sector?.playerFleet?.fleetData?.membersListCopy
//            ?.filter { Helper.isFrameHull(it) && Helper.isHullSameOrBigger(it, model.currTarget!!) }?.toMutableList()
//            ?: mutableListOf())
        return results
    }
}