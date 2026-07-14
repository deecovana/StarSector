package data.missions.roider_eyeofthestorm

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.BattleCreationContext
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.mission.MissionDefinitionPlugin
import roiderUnion.helpers.DModHelper
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.Variants
import java.util.*

// Eye of the Storm
class MissionDefinition : MissionDefinitionPlugin {
    companion object {
        const val CLARKE_SEED = 22345L
    }

    override fun defineMission(api: MissionDefinitionAPI) {
        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, ExternalStrings.EYE_PLAYER_SIDE, FleetGoal.ESCAPE, false)
        api.initFleet(FleetSide.ENEMY, ExternalStrings.EYE_ENEMY_SIDE, FleetGoal.ATTACK, true)

        // Set a blurb for each fleet
        api.setFleetTagline(FleetSide.PLAYER, ExternalStrings.EYE_PLAYER_TAG)
        api.setFleetTagline(FleetSide.ENEMY, ExternalStrings.EYE_ENEMY_TAG)

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(ExternalStrings.EYE_BRIEFING)

        // Set up the player's fleet
        DModHelper.addDShipToFleet(
            2,
            FleetSide.PLAYER,
            Variants.CYCLOPS_E_OUTDATED,
            ExternalStrings.SHIP_CLARKE,
            true,
            Random(CLARKE_SEED),
            api
        )


        // Set up the enemy fleet
        api.addToFleet(FleetSide.ENEMY, Variants.TEMPEST_PURSUIT, FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, Variants.WOLF_PURSUIT, FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, Variants.WOLF_PURSUIT, FleetMemberType.SHIP, false)


        // Set up the map.
        val width = 4000f
        val height = 24000f
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f)

//		float minX = -width/2;
//		float minY = -height/2;
        for (i in 0..14) {
            val x = Math.random().toFloat() * width - width / 2
            val y = Math.random().toFloat() * height - height / 2
            val radius = 100f + Math.random().toFloat() * 900f
            api.addNebula(x, y, radius)
        }
        api.addAsteroidField(10f, 0f, -88f, width, 400f, 600f, 150)
        api.addAsteroidField(-10f, 0f, -92f, width, 400f, 600f, 150)
        api.addAsteroidField(0f, 0f, 0f, height - 1000, 40f, 60f, 100)
        api.addPlugin(SpawnShift(width, height))
        val context = BattleCreationContext(null, null, null, null)
        context.initialEscapeRange = 10000f
        api.addPlugin(EscapeRevealPlugin(context))
    }

    // Credit: Cycerin, Dark Revenant
    inner class SpawnShift(private val mapX: Float, private val mapY: Float) : BaseEveryFrameCombatPlugin() {
        var hasDeployed = false
        override fun advance(amount: Float, events: List<InputEventAPI>) {
            if (!hasDeployed) {
                val enemyFleet = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY)
                for (member in enemyFleet.reservesCopy) {
                    if (!enemyFleet.deployedCopy.contains(member)) {
                        enemyFleet.spawnFleetMember(member, getSafeSpawn(FleetSide.ENEMY, mapX, mapY), 90f, 1f)
                    }
                }
                hasDeployed = true
            }
        }

        fun getSafeSpawn(side: FleetSide?, mapX: Float, mapY: Float): Vector2f {
            val spawnLocation = Vector2f()
            spawnLocation.x = MathUtils.getRandomNumberInRange(-mapX / 2, mapX / 2)
            spawnLocation.y = -mapY / 2f + 1000
            return spawnLocation
        }
    }
}