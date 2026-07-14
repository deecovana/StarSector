package roiderUnion.fleets.mining

import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI
import roiderUnion.helpers.*
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFleetTypes

class RoiderMinerAssignmentAI(fleet: CampaignFleetAPI, route: RouteData) : RouteFleetAssignmentAI(fleet, route) {
    companion object {
        const val TOKEN_PLACE = "\$place"
        const val TOKEN_RESOURCES = "\$resources"

        const val MAX_FILL = RoiderMiningRouteManager.MAX_CARGO
    }

    private var hyperTransition = route.extra.fleetType == RoiderFleetTypes.MINING_ARMADA

    override fun advance(amount: Float) {
        super.advance(amount)
        destTravelTextOverride()
    }

    private fun destTravelTextOverride() {
        val current = route.current ?: return
        val hasReachedDest = hyperTransition && MiningHelper.isMiningSpot(current.to)
                && fleet.containingLocation == current.to?.containingLocation
        if (hasReachedDest) {
            hyperTransition = false
            fleet.clearAssignments()
            fleet.addAssignment(
                FleetAssignment.GO_TO_LOCATION,
                current.to,
                Short.MAX_VALUE.toFloat(),
                getTravelActionText(current),
                goNextScript(current)
            )
        }
    }

    override fun getStartingActionText(segment: RouteSegment?): String {
        return ExternalStrings.MINING_LOAD
            .replace(TOKEN_RESOURCES, Helper.getAndJoinedCommodities(listOf(Commodities.HEAVY_MACHINERY)))
            .replace(TOKEN_PLACE, segment?.from?.name ?: ExternalStrings.DEBUG_NULL)
    }

    override fun getTravelActionText(segment: RouteSegment?): String {
        if (segment == null) return ExternalStrings.DEBUG_NULL
        if (segment.to == route.market.primaryEntity) {
            return ExternalStrings.MINING_RETURN.replace(TOKEN_PLACE, route.market.name)
        }
        if (fleet.containingLocation != segment.to.containingLocation) {
            return ExternalStrings.MINING_TRAVEL.replace(TOKEN_PLACE, segment.to.starSystem.nameWithLowercaseType)
        }
        val name = Memory.getNullable(MemoryKeys.TOKEN_NAME, segment.to, { it is String }, { null }) as? String
            ?: segment.to.name
        return ExternalStrings.MINING_TRAVEL.replace(TOKEN_PLACE, name)
    }

    override fun addLocalAssignment(current: RouteSegment?, justSpawned: Boolean) {
        if (current == null) return
        if (justSpawned) {
            val progress = current.progress
            RouteLocationCalculator.setLocation(
                fleet, progress,
                current.from, current.destination
            )
        }
        if (MiningHelper.isMiningSpot(current.from)) {
            fleet.addAssignment(
                FleetAssignment.ORBIT_AGGRESSIVE, current.from,
                current.daysMax - current.elapsed, getInSystemActionText(current),
                AddOreScript(fleet, route, current, this::goNextScript)
            )
            return
        }
        if (current.from === route.market.primaryEntity) {
            if (current.id == RoiderMiningRouteManager.PREPARE) {
                fleet.addAssignment(
                    FleetAssignment.ORBIT_PASSIVE, current.from,
                    current.daysMax - current.elapsed, getInSystemActionText(current),
                    LoadMachineryScript(fleet, current, this::goNextScript)
                )
            } else {
                fleet.addAssignment(
                    FleetAssignment.ORBIT_PASSIVE, current.from,
                    current.daysMax - current.elapsed, getInSystemActionText(current),
                    goNextScript(current)
                )
            }
            return
        }
        super.addLocalAssignment(current, justSpawned)
    }

    override fun getInSystemActionText(segment: RouteSegment?): String {
        if (segment == null) return ExternalStrings.DEBUG_NULL
        if (MiningHelper.isMiningSpot(segment.from)) {
            val locType = segment.custom as? LocationType ?: return super.getInSystemActionText(segment)
            val resources = mutableListOf<String>()
            val text = if (locType == LocationType.IN_SMALL_NEBULA) {
                resources += Commodities.VOLATILES
                ExternalStrings.MINING_COLLECT
            } else {
                val locRez = MiningHelper.getMiningResources(segment.from.starSystem)
                if (locRez.contains(Commodities.ORE)) resources += Commodities.ORE
                if (locRez.contains(Commodities.RARE_ORE)) resources += Commodities.RARE_ORE
                if (locRez.contains(Commodities.ORGANICS)) resources += Commodities.ORGANICS
                ExternalStrings.MINING_MINE
            }
            return text.replace(TOKEN_RESOURCES, Helper.getAndJoinedCommodities(resources))
        }
        return super.getInSystemActionText(segment)
    }

    override fun getEndingActionText(segment: RouteSegment?): String {
        if (segment?.from === route.market.primaryEntity) {
            val fleetRez = fleet.cargo.stacksCopy.filter { it.isCommodityStack }
            val resources = mutableListOf<String>()
            if (fleetRez.any { it.commodityId == Commodities.ORE }) resources += Commodities.ORE
            if (fleetRez.any { it.commodityId == Commodities.RARE_ORE }) resources += Commodities.RARE_ORE
            if (fleetRez.any { it.commodityId == Commodities.ORGANICS }) resources += Commodities.ORGANICS
            if (fleetRez.any { it.commodityId == Commodities.VOLATILES }) resources += Commodities.VOLATILES
            return ExternalStrings.MINING_UNLOAD.replace(TOKEN_RESOURCES, Helper.getAndJoinedCommodities(resources))
        }
        return super.getEndingActionText(segment)
    }

    fun loadMachineryAndGoNextScript(current: RouteSegment): Script {
        return Script {
            CargoHelper.addCommodity(Commodities.HEAVY_MACHINERY, fleet.cargo.maxCapacity * RoiderMiningRouteManager.MACHINERY_CARGO, fleet.cargo) // extern
            goNextScript(current).run()
        }
    }

    fun addOreAndGoNextScript(current: RouteSegment): Script {
        return Script {
            val locType = current.custom as? LocationType ?: LocationType.IN_ASTEROID_BELT
            val added = if (locType == LocationType.IN_SMALL_NEBULA) {
                listOf(Commodities.VOLATILES)
            } else {
                listOf(Commodities.ORE, Commodities.RARE_ORE, Commodities.ORGANICS)
            }
            val mult = when (route.extra.fleetType) {
                RoiderFleetTypes.MINING_ARMADA -> MAX_FILL / RoiderMiningRouteManager.HEAVY_STOPS
                RoiderFleetTypes.MINING_FLEET -> MAX_FILL / RoiderMiningRouteManager.MEDIUM_STOPS
                else -> MAX_FILL / RoiderMiningRouteManager.LIGHT_STOPS
            }
            MiningHelper.fillCargo(fleet, current.from.starSystem, added, mult)
            goNextScript(current).run()
        }
    }
}