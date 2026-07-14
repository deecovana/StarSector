package scripts.campaign.fleets.expeditions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import scripts.Roider_Misc
import java.util.*

/**
 * Author: SafariJohn
 */
object Roider_ExpeditionFleetFactory {
    fun createExpedition(
        type: String,
        locInHyper: Vector2f?,
        source: MarketAPI,
        pirate: Boolean,
        random: Random?
    ): CampaignFleetAPI? {
        return createExpedition(type, locInHyper, null, source, pirate, random)
    }

    fun createExpedition(
        type: String?,
        locInHyper: Vector2f?,
        route: RouteData?,
        source: MarketAPI,
        pirate: Boolean,
        random: Random?
    ): CampaignFleetAPI? {
        var expType = type
        var expRoute = route
        var expRandom = random
        if (expRandom == null) expRandom = Random()
        if (expType == null) {
            val picker = WeightedRandomPicker<String>(expRandom)
            picker.add(RoiderFleetTypes.EXPEDITION, 25f)
            //			picker.add(Roider_FleetTypes.MAJOR_EXPEDITION, 0f);
            expType = picker.pick()
        }
        if (expRoute == null) {
            expRoute = RouteData(
                "temp", source, expRandom.nextLong(),
                OptionalFleetData()
            )
        }
        val points = generateFleetPoints(expType!!, expRandom)

//		int combat = 0;
//		int freighter = 0;
//		int tanker = 0;
//		int transport = 0;
//		int utility = 0;
//
//        switch (type) {
//            case Roider_Ids.Roider_FleetTypes.MOTHER_EXPEDITION:
//            case Roider_Ids.Roider_FleetTypes.MAJOR_EXPEDITION:
//                combat = 7 + random.nextInt(8);
//                freighter = 6 + random.nextInt(7);
//                tanker = 5 + random.nextInt(6);
//                transport = 3 + random.nextInt(8);
//                utility = 4 + random.nextInt(5);
//                break;
//            case Roider_Ids.Roider_FleetTypes.EXPEDITION:
//                combat = random.nextInt(2) + 1;
//                tanker = random.nextInt(2) + 1;
//                utility = random.nextInt(2) + 1;
//                break;
//            default:
//                combat = 4 + random.nextInt(5);
//                freighter = 4 + random.nextInt(5);
//                tanker = 3 + random.nextInt(4);
//                transport = random.nextInt(2);
//                utility = 2 + random.nextInt(3);
//                break;
//        }
        if (pirate) {
//			combat += transport;
//			combat += utility;
//			transport = 0;
//            utility = 0;
        }

//		combat *= 5f;
//		freighter *= 3f;
//		tanker *= 3f;
//		transport *= 1.5f;
        val fleet: CampaignFleetAPI?
        if (expRoute.factionId != RoiderFactions.ROIDER_UNION) {
            // Get half the ships from Roider Union's choices
            val params = FleetParamsV3(
                source,
                null,  // loc in hyper; don't need if have market
                RoiderFactions.ROIDER_UNION,
                expRoute.qualityOverride,  // quality override
                expType,
                points.combat / 2,  // combatPts
                points.freighter / 2,  // freighterPts
                points.tanker / 2,  // tankerPts
                points.transport / 2,  // transportPts
                points.liner / 2,  // linerPts
                points.utility / 2,  // utilityPts
                0f // qualityMod
            )
            params.timestamp = expRoute.timestamp
            params.random = expRandom
            //		params.modeOverride = Misc.getShipPickMode(market);
            params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
            fleet = FleetFactoryV3.createFleet(params)
            if (fleet == null || fleet.isEmpty) return null


            // Get the other half from the market's faction
            val params2 = FleetParamsV3(
                source,
                null,  // loc in hyper; don't need if have market
                null,
                null,  // quality override
                expType,
                points.combat / 2,  // combatPts
                points.freighter / 2,  // freighterPts
                points.tanker / 2,  // tankerPts
                points.transport / 2,  // transportPts
                points.liner / 2,  // linerPts
                points.utility / 2,  // utilityPts
                0f // qualityMod
            )
            params2.timestamp = Global.getSector().clock.timestamp
            params2.random = expRandom
            //		params.modeOverride = Misc.getShipPickMode(market);
            params2.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
            val fleet2: CampaignFleetAPI? = FleetFactoryV3.createFleet(params2)
            if (fleet2 != null && !fleet2.isEmpty) {
                for (member in fleet2.membersWithFightersCopy) {
                    if (member.isFighterWing) continue
                    member.captain = null
                    if (member.isFlagship) member.isFlagship = false
                    fleet.fleetData.addFleetMember(member)
                }
            }
            FleetFactoryV3.addCommanderAndOfficers(fleet, params, expRandom)
            Roider_Misc.sortFleetByShipSize(fleet)
        } else {
            val params = FleetParamsV3(
                source,
                null,  // loc in hyper; don't need if have market
                RoiderFactions.ROIDER_UNION,
                expRoute.qualityOverride,  // quality override
                expType,
                points.combat,  // combatPts
                points.freighter,  // freighterPts
                points.tanker,  // tankerPts
                points.transport,  // transportPts
                points.liner,  // linerPts
                points.utility,  // utilityPts
                0f // qualityMod
            )
            params.timestamp = expRoute.timestamp
            params.random = expRandom
            //		params.modeOverride = Misc.getShipPickMode(market);
            params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
            fleet = FleetFactoryV3.createFleet(params)
            if (fleet == null || fleet.isEmpty) return null
        }

//        fleet.setFaction(source.getFactionId(), true);
        if (expType == RoiderFleetTypes.MAJOR_EXPEDITION) fleet.setFaction(
            RoiderFactions.ROIDER_UNION,
            true
        ) else fleet.setFaction(source.factionId, true)
        fleet.name = fleet.faction.getFleetTypeName(expType)
        return fleet
    }

    private fun generateFleetPoints(type: String, random: Random): Roider_FleetPointParams {
        val points = Roider_FleetPointParams()

        // Expedition has floor size and ceiling size, and tries to match player fleet inbetween
        // Major expedition has floor size and tries to exceed player fleet
        // Mothership expedition has floor size and tries to exceed player fleet
        val expFloor = 20
        val expCeiling = 100
        val majorExpFloor = 60
        val motherExpFloor = 120
        val playerFleet = Global.getSector().playerFleet.fleetPoints
        var expeditionPoints = playerFleet - random.nextInt(5) + random.nextInt(10)
        if (expeditionPoints <= 5) expeditionPoints = 5
        expeditionPoints *= (1f + random.nextFloat()).toInt()
        if (type == RoiderFleetTypes.EXPEDITION) {
            expeditionPoints = Math.max(expeditionPoints, expFloor)
            expeditionPoints = Math.min(expeditionPoints, expCeiling)
            points.combat = expeditionPoints * 0.6f
            points.freighter = expeditionPoints * 0.1f
            points.tanker = expeditionPoints * 0.1f
            points.transport = expeditionPoints * 0.1f
            points.liner = 0f
            points.utility = expeditionPoints * 0.1f
        }
        if (type == RoiderFleetTypes.MAJOR_EXPEDITION) {
            expeditionPoints = Math.max(expeditionPoints, majorExpFloor)
            expeditionPoints *= (1.2f + random.nextFloat() * 0.3).toInt()
            points.combat = expeditionPoints * 0.5f
            points.freighter = expeditionPoints * 0.1f
            points.tanker = expeditionPoints * 0.2f
            points.transport = expeditionPoints * 0.1f
            points.liner = 0f
            points.utility = expeditionPoints * 0.1f
        }
        if (type == RoiderFleetTypes.MOTHER_EXPEDITION) {
            expeditionPoints = Math.max(expeditionPoints, motherExpFloor)
            expeditionPoints *= (1.4f + random.nextFloat() * 0.4).toInt()
            points.combat = expeditionPoints * 0.5f
            points.freighter = expeditionPoints * 0.1f
            points.tanker = expeditionPoints * 0.1f
            points.transport = expeditionPoints * 0.1f
            points.liner = expeditionPoints * 0.1f
            points.utility = expeditionPoints * 0.1f
        }
        points.combat += Math.max(0f, expeditionPoints - points.sum())
        return points
    }

    class Roider_FleetPointParams {
        var combat = 0f
        var freighter = 0f
        var tanker = 0f
        var transport = 0f
        var liner = 0f
        var utility = 0f
        fun sum(): Float {
            return combat + freighter + tanker + transport + liner + utility
        }
    }
}