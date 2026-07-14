package data.missions.roider_crushing

import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.mission.MissionDefinitionPlugin
import roiderUnion.helpers.DModHelper
import java.util.*

// Crushing Responsibility
class MissionDefinition : MissionDefinitionPlugin {
    override fun defineMission(api: MissionDefinitionAPI) {
        val random = Random(64323676)

        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false)
        api.initFleet(FleetSide.ENEMY, "THI", FleetGoal.ATTACK, true)

        // Set a blurb for each fleet
        api.setFleetTagline(FleetSide.PLAYER, "Roider Union Experimental Detachment") // extern
        api.setFleetTagline(FleetSide.ENEMY, "Scavs with Tiandong Mercenary Escort")
        val flagship = "ISS Roach"

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem("Defeat Tiandong's thugs")
        api.addBriefingItem("The experimental $flagship must survive")

        // Set up the player's fleet
        DModHelper.addDShipToFleet(4, FleetSide.PLAYER, "venture_Outdated", null, false, random, api)
        //        addDShipToFleet(2, FleetSide.PLAYER, "roider_firestorm_Assault", null, false, random, api);
//        addDShipToFleet(3, FleetSide.PLAYER, "roider_cowboy_Interceptor", null, false, random, api);
//        addDShipToFleet(2, FleetSide.PLAYER, "roider_onager_Assault", null, false, random, api);
//        addDShipToFleet(1, FleetSide.PLAYER, "roider_aurochs_Elite", null, false, random, api);
//        addDShipToFleet(1, FleetSide.PLAYER, "roider_roach_Assault", flagship, true, random, api);
//		api.defeatOnShipLoss(flagship);
//        addDShipToFleet(2, FleetSide.PLAYER, "roider_cyclops_Assault", null, false, random, api);
//        addDShipToFleet(1, FleetSide.PLAYER, "roider_cyclops_Balanced", null, false, random, api);
//        addDShipToFleet(2, FleetSide.PLAYER, "roider_pepperbox_Support", null, false, random, api);

        // Set up the enemy fleet
//        addDShipToFleet(2, FleetSide.ENEMY, "tiandong_wuzhang_Standard", null, false, random, api);
//        addDShipToFleet(2, FleetSide.ENEMY, "tiandong_qianzi_Standard", null, false, random, api);
//        addDShipToFleet(1, FleetSide.ENEMY, "tiandong_hanzhong_Closesupport", null, false, random, api);
//        addDShipToFleet(2, FleetSide.ENEMY, "tiandong_hujing_Brawler", null, false, random, api);
////        addDShipToFleet(2, FleetSide.ENEMY, "tiandong_guan_du_Balanced", null, false, random, api);
//        addDShipToFleet(2, FleetSide.ENEMY, "mule_Standard", null, false, random, api);
//        addDShipToFleet(3, FleetSide.ENEMY, "tiandong_chengdu_Industrial", null, false, random, api);
////        addDShipToFleet(2, FleetSide.ENEMY, "tiandong_luo_yang_Auxiliary", null, false, random, api);
//        addDShipToFleet(1, FleetSide.ENEMY, "tiandong_luo_yang_Auxiliary", null, false, random, api);
//        addDShipToFleet(2, FleetSide.ENEMY, "hound_Standard", null, false, random, api);
////        addDShipToFleet(1, FleetSide.ENEMY, "hound_Standard", null, false, random, api);
//        addDShipToFleet(3, FleetSide.ENEMY, "tiandong_nanzhong_Standard", null, false, random, api);
////        addDShipToFleet(2, FleetSide.ENEMY, "tiandong_wujun_Support", null, false, random, api);
        DModHelper.addDShipToFleet(2, FleetSide.ENEMY, "vigilance_Standard", null, false, random, api)


        // Set up the map.
        val width = 18000f
        val height = 16000f
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f)
        val minX = -width / 2
        val minY = -height / 2

        // Spawn Roach in front of TT ships
        for (i in 0..14) {
            val x = Math.random().toFloat() * width - width / 2
            val y = Math.random().toFloat() * height - height / 2
            val radius = 100f + Math.random().toFloat() * 900f
            api.addNebula(x, y, radius)
        }
        api.addAsteroidField(
            0f, 0f, 0f, height,
            10f, 20f, 200
        )
        api.addAsteroidField(
            0f, 0f, 150f, height,
            20f, 40f, 50
        )

//		api.addPlugin(new BaseEveryFrameCombatPlugin() {
//            @Override
//			public void init(CombatEngineAPI engine) {
//				engine.getContext().setStandoffRange(10000f);
//			}
//		});
    }
}