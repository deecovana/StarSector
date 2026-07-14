package roiderUnion.fleets.nomads

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.command.WarSimScript
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.CargoQuantityData
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.EconomyRouteData
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.*
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.*
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import java.util.*
import kotlin.math.max
import kotlin.math.min

class NomadTradeRouteManager : EconomyFleetRouteManager() {
    companion object {
        private const val MAX_STABILITY = 10
        val GO_TO_MINE = ROUTE_TRAVEL_WS
        val MINE = ROUTE_RESUPPLY_WS
    }

    override fun getMaxFleets(): Int {
        return if (canAddNomadRoute()) super.getMaxFleets() else 0
    }

    private fun canAddNomadRoute(): Boolean {
        val routes = getInstance().getRoutesForSource(routeSourceId)
        val numNomads = routes.mapNotNull { it.custom as? EconomyRouteData }
            .count { Memory.isFlag(MemoryKeys.NOMAD_BASE, it.to) || Memory.isFlag(MemoryKeys.NOMAD_BASE, it.from) }
        return Settings.MAX_NOMAD_TRADE_FLEETS > numNomads
    }

    override fun addRouteFleetIfPossible() {
        val from = pickSourceMarket()
        val to = pickDestMarket(from)
        if (from != null && to != null) {
            val data = createData(from, to) ?: return
            log.info("Added roider nomad trade fleet route from " + from.name + " to " + to.name)
            val seed = Misc.genRandomSeed()
            val id = routeSourceId
            val extra = OptionalFleetData(from)
            val tier = data.size
            val stability = from.stabilityValue
            var factionId = from.factionId
            if (!from.faction.isHostileTo(Factions.INDEPENDENT) && !to.faction.isHostileTo(Factions.INDEPENDENT)) {
                if (Helper.random.nextFloat() * MAX_STABILITY > stability + tier) {
                    factionId = Factions.INDEPENDENT
                }
            }
            if (data.smuggling) {
                factionId = Factions.INDEPENDENT
            }
            extra.factionId = factionId
            val route = getInstance().addRoute(id, from, seed, extra, this) ?: return
            val random = route.random ?: Helper.random
            route.custom = data
            val sysFrom = data.from.starSystem
            val sysTo = data.to.starSystem
            val dFrom = WarSimScript.getDangerFor(factionId, sysFrom)
            val dTo = WarSimScript.getDangerFor(factionId, sysTo)
            val danger = if (dFrom.ordinal > dTo.ordinal) dFrom else dTo
            var pLoss = DANGER_LOSS_PROB[danger]!!
            if (data.smuggling) pLoss *= 0.5f
            if (random.nextFloat() < pLoss) {
                val returning = Math.random().toFloat() < 0.5f
                applyLostShipping(data, returning, true, true, true)
                getInstance().removeRoute(route)
                return
            }
            val orbitDays = data.size * (0.75f + Math.random().toFloat() * 0.5f)
            route.addSegment(RouteSegment(ROUTE_SRC_LOAD, orbitDays, from.primaryEntity))
            possiblyAddMiningSegments(route, data)
            route.addSegment(RouteSegment(ROUTE_TRAVEL_DST, from.primaryEntity, to.primaryEntity))
            route.addSegment(RouteSegment(ROUTE_DST_UNLOAD, orbitDays * 0.5f, to.primaryEntity))
            setDelayAndSendMessage(route)
            recentlySentTradeFleet.add(from.id, Settings.TRADE_FLEET_INTERVAL)
        }
    }

    private fun possiblyAddMiningSegments(route: RouteData, data: EconomyRouteData) {
        val random = route.random ?: Helper.random
        if (random.nextBoolean()) return
        val comingFromNomads = Memory.isFlag(MemoryKeys.NOMAD_BASE, route.market)
        if (!comingFromNomads) return
        val dest = pickDest(route.market) ?: return
        MiningHelper.addMiningStop(route, route.market, dest, GO_TO_MINE)
    }

    private fun pickDest(source: MarketAPI): StarSystemAPI? {
        val systems = Helper.sector?.starSystems
            ?.filter { MiningHelper.inMiningRange(it, source.primaryEntity) }
            ?.filter { MiningHelper.canMine(it) }
            ?.filterNot { it === source.starSystem } ?: return null
        if (systems.isEmpty()) return null
        return systems[Helper.random.nextInt(systems.size)]
    }

    override fun pickSourceMarket(): MarketAPI? {
        val markets = WeightedRandomPicker<MarketAPI>()
        for (market in Helper.sector?.economy?.marketsCopy ?: emptyList()) {
            if (market.isHidden && !Memory.isFlag(MemoryKeys.NOMAD_BASE, market)) continue
            if (!market.hasSpaceport()) continue
            if (!Helper.hasRoiders(market)) continue
            if (SharedData.getData().marketsWithoutTradeFleetSpawn.contains(market.id)) continue
            if (recentlySentTradeFleet.contains(market.id)) continue
            markets.add(market, market.size.toFloat())
        }
        return markets.pick()
    }

    override fun pickDestMarket(from: MarketAPI?): MarketAPI? {
        if (from == null) return null
        val markets = WeightedRandomPicker<MarketAPI>()
        val relevant: MutableList<CommodityOnMarketAPI> = ArrayList()
        for (com in from.allCommodities) {
            if (com.isNonEcon) continue
            val exported = min(com.available, com.maxSupply)
            val imported = max(0, com.maxDemand - exported)
            if (imported > 0 || exported > 0) {
                relevant.add(com)
            }
        }
//        val comingFromNomads = Memory.isFlag(MemoryKeys.NOMAD_BASE, from)
        val destMarkets = Helper.sector?.economy?.marketsCopy?.filter { Helper.hasRoiders(it) } ?: emptyList()
        for (market in destMarkets) {
            if (!market.hasSpaceport()) continue
            if (!Helper.hasRoiders(market)) continue
//            if (!comingFromNomads && !Memory.isFlag(MemoryKeys.NOMAD_BASE, market)) continue
            if (SharedData.getData().marketsWithoutTradeFleetSpawn.contains(market.id)) continue
            if (market === from) continue
            val shipping = Misc.getShippingCapacity(market, market.faction === from.faction)
            if (shipping <= 0) continue
            var w = 0f
            for (com in relevant) {
                var exported = min(com.available, com.maxSupply)
                exported = min(exported, shipping)
                var imported = max(0, com.maxDemand - exported)
                imported = min(imported, shipping)
                val other = market.getCommodityData(com.id)
                exported = min(exported, other.maxDemand - other.maxSupply)
                if (exported < 0) exported = 0
                imported = min(imported, min(other.available, other.maxSupply))
                w += imported.toFloat()
                w += exported.toFloat()
            }
            if (from.faction.isHostileTo(market.faction)) {
                w *= 0.25f
            }
            markets.add(market, w)
        }
        return markets.pick()
    }

    override fun spawnFleet(route: RouteData?): CampaignFleetAPI? {
        if (route == null) return null
        val random = route.random ?: Random(route.seed ?: Helper.random.nextLong())
        val fleet = createNomadTradeFleet(route, random) ?: return null
        fleet.addEventListener(this)
        fleet.addScript(NomadTradeFleetAssignmentAI(fleet, route))
        return fleet
    }

    protected fun createNomadTradeFleet(route: RouteData?, random: Random): CampaignFleetAPI? {
        if (route == null) return null
        val data = route.custom as? EconomyRouteData ?: return null
        val from = data.from
        var tier = data.size
        if (data.smuggling && tier > 4) {
            tier = 4f
        }
        val factionId = route.factionId
        var total = 0f
        var fuel = 0f
        var cargo = 0f
        var personnel = 0f
        val all: MutableList<CargoQuantityData> = ArrayList()
        all.addAll(data.cargoDeliver)
        all.addAll(data.cargoReturn)
        for (curr in all) {
            val spec = curr.commodity
            if (spec.isMeta) continue
            total += curr.units.toFloat()
            if (spec.hasTag(Commodities.TAG_PERSONNEL)) {
                personnel += curr.units.toFloat()
            } else if (spec.id == Commodities.FUEL) {
                fuel += curr.units.toFloat()
            } else {
                cargo += curr.units.toFloat()
            }
        }
        if (total < 1f) total = 1f
        var fuelFraction = fuel / total
        var personnelFraction = personnel / total
        var cargoFraction = cargo / total
        if (fuelFraction + personnelFraction + cargoFraction > 0) {
            val mult = 1f / (fuelFraction + personnelFraction + cargoFraction)
            fuelFraction *= mult
            personnelFraction *= mult
            cargoFraction *= mult
        }
        log.info("Creating roider nomad trade fleet of tier " + tier + " for market [" + from.name + "]")
        val stabilityFactor = 1f + from.stabilityValue / 20f
        val combat = max(1f, tier * stabilityFactor * 0.5f) * 10f
        var freighter = tier * 2f * cargoFraction * 3f
        var tanker = tier * 2f * fuelFraction * 3f
        var transport = tier * 2f * personnelFraction * 3f
        var liner = 0f
        val utility = 0f
        if (data.smuggling) {
            freighter *= 0.5f
            tanker *= 0.5f
            transport *= 0.5f
            liner *= 0.5f
        }
        val params = FleetParamsV3(
            from,
            null,  // locInHyper
            factionId,
            route.qualityOverride,  // qualityOverride
            RoiderFleetTypes.NOMAD_FLEET,
            combat,  // combatPts
            freighter,  // freighterPts
            tanker,  // tankerPts
            transport,  // transportPts
            liner,  // linerPts
            utility,  // utilityPts
            0f //-0.5f // qualityBonus
        )
        params.timestamp = route.timestamp
        params.onlyApplyFleetSizeToCombatShips = true
        params.maxShipSize = 3
        params.officerLevelBonus = -2
        params.officerNumberMult = 0.5f
        params.random = random
        val factionParams = FleetsHelper.getStandardFactionParams(route.factionId)
        val fleet = FleetsHelper.createMultifactionFleet(
            params,
            route.factionId,
            *factionParams
        )
        if (fleet.isEmpty) return null
        if (Misc.isPirateFaction(fleet.faction)) {
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF] = true
            Misc.makeNoRepImpact(fleet, routeSourceId)
        }
        if (fleet.faction.id != RoiderFactions.ROIDER_UNION) Misc.makeLowRepImpact(fleet, routeSourceId)
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_TRADE_FLEET] = true
        data.cargoCap = fleet.cargo.maxCapacity
        data.fuelCap = fleet.cargo.maxFuel
        data.personnelCap = fleet.cargo.maxPersonnel
        return fleet
    }

    override fun setDelayAndSendMessage(route: RouteData?) {}
}