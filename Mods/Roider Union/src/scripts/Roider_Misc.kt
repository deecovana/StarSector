package scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*
import kotlin.Comparator
import kotlin.math.max
import kotlin.math.min

/**
 * Author: SafariJohn
 */
object Roider_Misc {
    fun createPatrolParams(route: RouteData, size: FleetSize?): FleetParamsV3 {
        val random = Random()
        val market = route.market
        var fleetType = FleetTypes.PATROL_SMALL
        var combat = 0f
        var tanker = 0f
        var freighter = 0f
        when (size) {
            FleetSize.SMALL -> {
                combat = Math.round(1f + random.nextFloat() * 2f) * 5f
                freighter = Math.round(random.nextFloat()) * 5f
                fleetType = FleetTypes.PATROL_SMALL
            }

            FleetSize.MEDIUM -> {
                combat = Math.round(3f + random.nextFloat() * 3f) * 5f
                tanker = Math.round(random.nextFloat()) * 5f
                freighter = Math.round(random.nextFloat()) * 15f
                fleetType = FleetTypes.PATROL_MEDIUM
            }

            FleetSize.LARGE -> {
                combat = Math.round(5f + random.nextFloat() * 5f) * 5f
                tanker = Math.round(random.nextFloat()) * 10f
                freighter = Math.round(random.nextFloat()) * 25f
                fleetType = FleetTypes.PATROL_LARGE
            }

            else -> {}
        }
        val params = FleetParamsV3(
            market,
            null,  // loc in hyper; don't need if have market
            route.factionId,
            null,  // quality override
            fleetType,
            combat,  // combatPts
            freighter,  // freighterPts
            tanker,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod
        )
        params.timestamp = Global.getSector().clock.timestamp
        params.random = random
        params.modeOverride = Misc.getShipPickMode(market)
        return params
    }

    fun createPatrolFleet(
        size: FleetSize?,
        sourceMarket: MarketAPI?, extra: OptionalFleetData?
    ): CampaignFleetAPI? {
        val route = RouteData(
            Misc.genUID(), sourceMarket,
            Random().nextLong(), extra
        )
        val params = createPatrolParams(route, size)
        return createPatrolFleet(route, params, size)
    }

    fun createPatrolFleet(
        route: RouteData,
        params: FleetParamsV3, size: FleetSize?
    ): CampaignFleetAPI? {
        val market = route.market
        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return null

//		fleet.setFaction(market.getFactionId(), true);
        fleet.name = market.faction.getFleetTypeName(params.fleetType)
        val postId = Ranks.POST_FLEET_COMMANDER
        var rankId = Ranks.SPACE_COMMANDER
        when (size) {
            FleetSize.SMALL -> rankId = Ranks.SPACE_COMMANDER
            FleetSize.MEDIUM -> rankId = Ranks.SPACE_CAPTAIN
            FleetSize.LARGE -> rankId = Ranks.SPACE_ADMIRAL
            else -> {}
        }
        fleet.commander.postId = postId
        fleet.commander.rankId = rankId

//		market.getContainingLocation().addEntity(fleet);
//		fleet.setFacing((float) Math.random() * 360f);
//		fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);
        return fleet
    }

    fun highlight(
        label: LabelAPI, color: Color?,
        vararg highlights: String
    ) {
        label.setHighlight(*highlights)
        label.setHighlightColor(color)
    }

    fun sortFleetByShipSize(fleet: CampaignFleetAPI) {
        fleet.fleetData.sort()
        if (true) return
        val sortedMembers = fleet.fleetData.membersListCopy
        Collections.sort(sortedMembers, Comparator { o1, o2 ->
            if (o1.isFighterWing || o2.isFighterWing) return@Comparator 0
            var o1Size = when (o1.hullSpec.hullSize) {
                HullSize.CAPITAL_SHIP -> 4
                HullSize.CRUISER -> 3
                HullSize.DESTROYER ->  2
                HullSize.FRIGATE -> 1
                else -> 0
            }
            var o2Size = when (o2.hullSpec.hullSize) {
                HullSize.CAPITAL_SHIP -> 4
                HullSize.CRUISER -> 3
                HullSize.DESTROYER -> 2
                HullSize.FRIGATE -> 1
                else -> 0
            }
            if (o1.isCivilian) o1Size -= 4
            if (o2.isCivilian) o2Size -= 4
            if (o2Size == o1Size) {
                o1.hullSpec.nameWithDesignationWithDashClass.compareTo(
                    o2.hullSpec.nameWithDesignationWithDashClass,
                    ignoreCase = true
                )
            } else o2Size - o1Size
        })
        for (m in sortedMembers) {
            fleet.fleetData.removeFleetMember(m)
        }
        for (m in sortedMembers) {
            fleet.fleetData.addFleetMember(m)
        }
    }

    fun getDistanceSquared(from: SectorEntityToken, to: SectorEntityToken): Float {
        return getDistanceSquared(from.location, to.location)
    }

    fun getDistanceSquared(v1: Vector2f, v2: Vector2f): Float {
        return (v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y)
    }

    enum class FleetSize {
        SMALL, MEDIUM, LARGE
    }
}