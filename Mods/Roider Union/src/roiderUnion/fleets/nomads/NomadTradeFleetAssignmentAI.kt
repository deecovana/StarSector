package roiderUnion.fleets.nomads

import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import roiderUnion.fleets.mining.RoiderMinerAssignmentAI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.helpers.MiningHelper
import roiderUnion.ids.MemoryKeys

class NomadTradeFleetAssignmentAI(fleet: CampaignFleetAPI, route: RouteData) : EconomyFleetAssignmentAI(fleet, route) {
    companion object {
        const val ORE_FILL_PERCENT = 0.1f
    }

    private var hyperTransition = route.segments.size > EconomyFleetRouteManager.ROUTE_RESUPPLY_WS

    override fun advance(amount: Float) {
        super.advance(amount)
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

    override fun getTravelActionText(segment: RouteManager.RouteSegment?): String {
        if (segment == null) return super.getTravelActionText(null)
        if (MiningHelper.isMiningSpot(segment.to)) {
            return if (fleet.containingLocation != segment.to.containingLocation) {
                ExternalStrings.MINING_TRAVEL.replace(RoiderMinerAssignmentAI.TOKEN_PLACE, segment.to.starSystem.nameWithLowercaseTypeShort)
            } else {
                val name = Memory.getNullable(MemoryKeys.TOKEN_NAME, segment.to, { it is String }, { null }) as? String
                    ?: segment.to.name
                ExternalStrings.MINING_TRAVEL.replace(RoiderMinerAssignmentAI.TOKEN_PLACE, name)
            }
        }
        return super.getTravelActionText(segment)
    }

    override fun addLocalAssignment(current: RouteManager.RouteSegment?, justSpawned: Boolean) {
        if (current == null) {
            super.addLocalAssignment(null, justSpawned)
            return
        }
        if (MiningHelper.isMiningSpot(current.from)) {
            fleet.addAssignment(
                FleetAssignment.ORBIT_AGGRESSIVE, current.from,
                current.daysMax - current.elapsed, getInSystemActionText(current),
                addOreAndGoNextScript(current)
            )
            return
        }
        super.addLocalAssignment(current, justSpawned)
    }

    override fun getInSystemActionText(segment: RouteManager.RouteSegment?): String {
        if (segment == null) {
            return super.getInSystemActionText(null)
        }
        if (MiningHelper.isMiningSpot(segment.from)) {
            val locType = segment.custom as? BaseThemeGenerator.LocationType ?: return super.getInSystemActionText(segment)
            val resources = mutableListOf<String>()
            val text = if (locType == BaseThemeGenerator.LocationType.IN_SMALL_NEBULA) {
                resources += Commodities.VOLATILES
                ExternalStrings.MINING_COLLECT
            } else {
                val locRez = MiningHelper.getMiningResources(segment.from.starSystem)
                if (locRez.contains(Commodities.ORE)) resources += Commodities.ORE
                if (locRez.contains(Commodities.RARE_ORE)) resources += Commodities.RARE_ORE
                if (locRez.contains(Commodities.ORGANICS)) resources += Commodities.ORGANICS
                ExternalStrings.MINING_MINE
            }
            return text.replace(RoiderMinerAssignmentAI.TOKEN_RESOURCES, Helper.getAndJoinedCommodities(resources))
        }
        return super.getInSystemActionText(segment)
    }

    fun addOreAndGoNextScript(current: RouteManager.RouteSegment): Script {
        return Script {
            val locType = current.custom as? BaseThemeGenerator.LocationType ?: BaseThemeGenerator.LocationType.IN_ASTEROID_BELT
            val added = if (locType == BaseThemeGenerator.LocationType.IN_SMALL_NEBULA) {
                listOf(Commodities.VOLATILES)
            } else {
                listOf(Commodities.ORE, Commodities.RARE_ORE, Commodities.ORGANICS)
            }
            MiningHelper.fillCargo(fleet, current.from.starSystem, added, ORE_FILL_PERCENT)
            goNextScript(current).run()
        }
    }
}