package data.missions.roider_midway

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.mission.MissionDefinitionPlugin
import roiderUnion.helpers.DModHelper
import data.missions.roider_eyeofthestorm.MissionDefinition
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.RoiderPlanets
import roiderUnion.ids.Variants
import roiderUnion.ids.systems.AtkaIds
import java.awt.Color
import java.util.*

// Midway
class MissionDefinition : MissionDefinitionPlugin {
    companion object {
        private const val SEED = 436233456L
    }

    override fun defineMission(api: MissionDefinitionAPI) {
        val random = Random(SEED)

        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, ExternalStrings.MIDWAY_PLAYER_SIDE, FleetGoal.ATTACK, false)
        api.initFleet(FleetSide.ENEMY, ExternalStrings.MIDWAY_ENEMY_SIDE, FleetGoal.ATTACK, true)

        // Set a blurb for each fleet
        api.setFleetTagline(FleetSide.PLAYER, ExternalStrings.MIDWAY_PLAYER_TAG)
        api.setFleetTagline(FleetSide.ENEMY, ExternalStrings.MIDWAY_ENEMY_TAG)

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(ExternalStrings.MIDWAY_BRIEFING1)
        api.addBriefingItem(ExternalStrings.MIDWAY_BRIEFING2)
        api.addBriefingItem(ExternalStrings.MIDWAY_BRIEFING3)


        // Set up the player's fleet
        // Cyclops, Pepperbox, Gambit, and Colossus are Early
        DModHelper.addDShipToFleet(1, FleetSide.PLAYER, Variants.RANCH_ATTACK, ExternalStrings.SHIP_HORNET, true, random, api)
        api.defeatOnShipLoss(ExternalStrings.SHIP_HORNET)
        DModHelper.addDShipToFleet(1, FleetSide.PLAYER, Variants.GAMBIT_E_SALVAGED, null, false, random, api)
        DModHelper.addDShipToFleet(1, FleetSide.PLAYER, Variants.TRAILBOSS_SUPPORT, null, false, random, api)
        DModHelper.addDShipToFleet(2, FleetSide.PLAYER, Variants.TRAILBOSS_SUPPORT, null, false, random, api)
        DModHelper.addDShipToFleet(2, FleetSide.PLAYER, Variants.CYCLOPS_E_BALANCED, null, false, random, api)
        val cRandom = Random(MissionDefinition.CLARKE_SEED)
        val cName = ExternalStrings.SHIP_CLARKE
        DModHelper.addDShipToFleet(2, FleetSide.PLAYER, Variants.CYCLOPS_E_OUTDATED, cName, false, cRandom, api)
        DModHelper.addDShipToFleet(1, FleetSide.PLAYER, Variants.PEPPERBOX_ESCORT, null, false, random, api)
        DModHelper.addDShipToFleet(1, FleetSide.PLAYER, Variants.LASHER_D_CS, null, false, random, api)


        // Set up the enemy fleet
//        addDShipToFleet(1, FleetSide.ENEMY, "atlas2_Standard", pirateShip, false, random, api);
        DModHelper.addDShipToFleet(1, FleetSide.ENEMY, Variants.FALCON_P_STRIKE, ExternalStrings.SHIP_RED_CASTLE, false, random, api)
        api.defeatOnShipLoss(ExternalStrings.SHIP_RED_CASTLE)
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, Variants.COLOSSUS_3_PIRATE, null, false, random, api)
        DModHelper.addDShipToFleet(3, FleetSide.ENEMY, Variants.COLOSSUS_3_PIRATE, null, false, random, api)
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, Variants.CONDOR_ATTACK, null, false, random, api)
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, Variants.CONDOR_STRIKE, null, false, random, api)
        DModHelper.addDShipToFleet(3, FleetSide.ENEMY, Variants.CONDOR_STRIKE, null, false, random, api)
        DModHelper.addDShipToFleet(4, FleetSide.ENEMY, Variants.BUFFALO_FS, null, false, random, api)
        DModHelper.addDShipToFleet(1, FleetSide.ENEMY, Variants.WOLF_D_P_ATTACK, null, false, random, api)
        DModHelper.addDShipToFleet(1, FleetSide.ENEMY, Variants.LASHER_D_CS, null, false, random, api)
        DModHelper.addDShipToFleet(3, FleetSide.ENEMY, Variants.MUDSKIPPER2_CS, null, false, random, api)
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, Variants.HOUND_D_P_STANDARD, null, false, random, api)


        // Set up the map.
        val width = 16000f
        val height = 14000f
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f)
        val minX = -width / 2
        val minY = -height / 2

        api.addAsteroidField(
            0f, 0f, 0f, height,
            10f, 20f, 200
        )
        api.addAsteroidField(
            0f, 0f, 150f, height,
            20f, 40f, 50
        )

        val fakeSys: StarSystemAPI = Global.getSector().createStarSystem(SEED.toString())
        val kalekhta: PlanetAPI = fakeSys.addPlanet(
            AtkaIds.KALEKHTA.id,
            fakeSys.createToken(0f, 0f), AtkaIds.KALEKHTA.name,
            RoiderPlanets.ICE_GIANT, 130f, 290f, 8000f, 400f
        )
        kalekhta.spec.planetColor = Color(255, 210, 170, 255)
        kalekhta.spec.pitch = 20f
        kalekhta.spec.tilt = 10f
        kalekhta.applySpecChanges()
        api.addPlanet(-55f, 144f, kalekhta.radius, kalekhta, 0f, true)
    }
}