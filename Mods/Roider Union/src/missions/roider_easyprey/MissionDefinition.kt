package data.missions.roider_easyprey

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.mission.MissionDefinitionPlugin
import roiderUnion.helpers.DModHelper
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.Variants
import java.util.*

// Easy Prey
class MissionDefinition : MissionDefinitionPlugin {
    companion object {
        private const val SEED = 3534634L
    }

    override fun defineMission(api: MissionDefinitionAPI) {
        val random = Random(SEED)

        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, ExternalStrings.EASY_PREY_PLAYER_SIDE, FleetGoal.ATTACK, false)
        api.initFleet(FleetSide.ENEMY, ExternalStrings.EASY_PREY_ENEMY_SIDE, FleetGoal.ATTACK, true)

        // Set a blurb for each fleet
        api.setFleetTagline(FleetSide.PLAYER, ExternalStrings.EASY_PREY_PLAYER_TAG)
        api.setFleetTagline(FleetSide.ENEMY, ExternalStrings.EASY_PREY_ENEMY_TAG)

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(ExternalStrings.EASY_PREY_BRIEFING)


        // Set up the player's fleet
        DModHelper.addDShipToFleet(
            2,
            FleetSide.PLAYER,
            Variants.HOUND_STARFARER,
            ExternalStrings.SHIP_ARCHIMEDES,
            true,
            random,
            api
        )

        // Mark player flagship as essential
        api.defeatOnShipLoss("ISS Archimedes")

        // Set up the enemy fleet
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, Variants.BUFFALO_FS, null, false, random, api)
        DModHelper.addDShipToFleet(1, FleetSide.ENEMY, Variants.LASHER_D_CS, null, false, random, api)
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, Variants.MUDSKIPPER2_HELL, null, false, random, api)
        //        addDShipToFleet(1, FleetSide.ENEMY, "hound_d_pirates_Standard", null, false, random, api);


        // Set up the map.
        val width = 10000f
        val height = 9000f
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f)
        val minX = -width / 2
        val minY = -height / 2

//		for (int i = 0; i < 15; i++) {
//			float x = (float) Math.random() * width - width/2;
//			float y = (float) Math.random() * height - height/2;
//			float radius = 100f + (float) Math.random() * 900f;
//			api.addNebula(x, y, radius);
//		}
        api.addAsteroidField(
            0f, 0f, 0f, height,
            10f, 20f, 200
        )
        api.addAsteroidField(
            0f, 0f, 150f, height,
            20f, 40f, 50
        )
        api.addPlugin(object : BaseEveryFrameCombatPlugin() {
            override fun init(engine: CombatEngineAPI) {
                engine.context.standoffRange = 8000f
            }
        })
    }
}