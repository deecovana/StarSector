package roiderUnion.fleets

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase.PatrolFleetData
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.FleetsHelper
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.helpers.Settings
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderFactions
import java.util.*
import kotlin.math.max

class UnionHQPatrolManager(private val market: MarketAPI) : RouteManager.RouteFleetSpawner, FleetEventListener {
    companion object {
        private fun getPatrolDays() = 35f + Helper.random.nextFloat() * 10f
        
        fun alias(x: XStream) {
            val jClass = UnionHQPatrolManager::class.java
            x.alias(Aliases.HQPMAN, jClass)
            x.aliasAttribute(jClass, "tracker", "t")
            x.aliasAttribute(jClass, "market", "m")
            x.aliasAttribute(jClass, "returningPatrolValue", "r")
        }
    }

    val tracker = IntervalUtil(
        Settings.AVG_PATROL_INTERVAL * 0.7f,
        Settings.AVG_PATROL_INTERVAL * 1.3f
    )

    private var returningPatrolValue = 0f

    private val routeSourceId: String
        get() = market.id + "_military"

    private fun getCount(vararg types: PatrolType): Int {
        val routes = RouteManager.getInstance().getRoutesForSource(routeSourceId)
        return routes.count { data -> types.any { it == (data.custom as? PatrolFleetData)?.type } }
    }

    fun advance(amount: Float, functional: Boolean) {
        if (!functional) return
        if (market.isInEconomy == false) return
        val spawnRate = market.stats?.dynamic?.getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT)?.modifiedValue ?: 1f
        val days = Misc.getDays(amount)
        var extraTime = 0f
        if (returningPatrolValue > 0) {
            // apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
            val interval: Float = tracker.intervalDuration
            extraTime = interval * days
            returningPatrolValue -= days
            if (returningPatrolValue < 0) returningPatrolValue = 0f
        }
        tracker.advance(days * spawnRate + extraTime)
        //tracker.advance(days * spawnRate * 100f);
        if (tracker.intervalElapsed()) {
            val picker = WeightedRandomPicker<PatrolType>(Helper.random)

            val maxLight = max(2, market.size / 2)
            val light = getCount(PatrolType.FAST)
            if (maxLight - light > 0) picker.add(PatrolType.FAST, (maxLight - light).toFloat())
            
            val maxMedium = (market.size.toFloat() / 3f + 1).toInt()
            val medium = getCount(PatrolType.COMBAT)
            if (maxMedium - medium > 0) picker.add(PatrolType.COMBAT, (maxMedium - medium).toFloat())
            
            val maxHeavy = maxMedium - 1
            val heavy = getCount(PatrolType.HEAVY)
            if (maxHeavy - heavy > 0) picker.add(PatrolType.HEAVY, (maxHeavy - heavy).toFloat())
            
            if (picker.isEmpty) return
            val type: PatrolType = picker.pick()
            val extra = RouteManager.OptionalFleetData(market)
            extra.fleetType = type.fleetType
            val route: RouteManager.RouteData = RouteManager.getInstance().addRoute(
                routeSourceId,
                market,
                Misc.genRandomSeed(),
                extra,
                this,
                PatrolFleetData(type)
            )
            route.addSegment(RouteManager.RouteSegment(getPatrolDays(), market.primaryEntity))
        }
    }

    override fun spawnFleet(route: RouteManager.RouteData?): CampaignFleetAPI {
        val custom = route?.custom as PatrolFleetData
        val random: Random = route.random
        val type: PatrolType = custom.type
        val weights: Map<FleetsHelper.Category, FleetsHelper.Weight> = when (type) {
            PatrolType.HEAVY -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW)
                )
            }
            PatrolType.COMBAT -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW)
                )
            }
            else -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.MID)
                )
            }
        }
        val fp = when (type) {
            PatrolType.HEAVY -> {
                (10f + random.nextFloat() * 5f) * 5f + random.nextFloat() * 20f
            }
            PatrolType.COMBAT -> {
                (6f + random.nextFloat() * 3f) * 5f + random.nextFloat() * 5f
            }
            else -> {
                (3f + random.nextFloat() * 2f) * 5f
            }
        }
        val params = FleetsHelper.createFleetParams(
            random,
            weights,
            fp,
            0f,
            null,
            market,
            null,
            market.factionId,
            type.fleetType
        )
        val factionParams = FleetsHelper.getStandardFactionParams(market.factionId)
        val fleet = FleetsHelper.createMultifactionFleet(
            params,
            market.factionId,
            *factionParams
        )
        if (!fleet.faction.getCustomBoolean(Factions.CUSTOM_PATROLS_HAVE_NO_PATROL_MEMORY_KEY)) {
            Memory.set(MemFlags.MEMORY_KEY_PATROL_FLEET, true, fleet)
            if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
                Memory.set(MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR, true, fleet)
            }
        } else if (fleet.faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
            Memory.set(MemFlags.MEMORY_KEY_PIRATE, true, fleet)

            // hidden pather and pirate bases
            // make them raid so there's some consequence to just having a colony in a system with one of those
            if (market.isHidden) {
                Memory.set(MemFlags.MEMORY_KEY_RAIDER, true, fleet)
            }
        }
        fleet.setFaction(market.factionId, true)
        fleet.name = Helper.sector?.getFaction(market.factionId)?.getFleetTypeName(type.fleetType) ?: ExternalStrings.DEBUG_NULL
        fleet.commander.postId = Ranks.POST_PATROL_COMMANDER
        fleet.commander.rankId = when (type) {
            PatrolType.FAST -> Ranks.SPACE_LIEUTENANT
            PatrolType.COMBAT -> Ranks.SPACE_COMMANDER
            PatrolType.HEAVY -> Ranks.SPACE_CAPTAIN
        }
        market.containingLocation?.addEntity(fleet)
        fleet.facing = Math.random().toFloat() * 360f
        fleet.setLocation(market.primaryEntity?.location?.x ?: 0f, market.primaryEntity?.location?.y ?: 0f)
        fleet.addScript(PatrolAssignmentAIV4(fleet, route))
        if (custom.spawnFP <= 0) {
            custom.spawnFP = fleet.fleetPoints
        }
        if (fleet.faction.id != RoiderFactions.ROIDER_UNION) {
            Memory.setFlag(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, fleet.id, fleet)
        }
        return fleet
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (Helper.anyNull(fleet, reason)) return
        if (reason != CampaignEventListener.FleetDespawnReason.REACHED_DESTINATION) return

        val route: RouteManager.RouteData = RouteManager.getInstance().getRoute(routeSourceId, fleet)
        if (route.custom is PatrolFleetData) {
            val custom = route.custom as PatrolFleetData
            if (custom.spawnFP > 0) {
                val fraction: Float = fleet!!.fleetPoints.toFloat() / custom.spawnFP.toFloat()
                returningPatrolValue += fraction
            }
        }
    }

    override fun shouldCancelRouteAfterDelayCheck(route: RouteManager.RouteData?): Boolean = false

    override fun shouldRepeat(route: RouteManager.RouteData?): Boolean = false

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteManager.RouteData?) {}

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}
}