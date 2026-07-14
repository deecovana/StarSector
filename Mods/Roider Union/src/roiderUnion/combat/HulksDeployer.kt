package roiderUnion.combat

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatFleetManagerAPI
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import roiderUnion.skills.roaming.Hulks.Companion.DummyListener

class HulksDeployer : BaseEveryFrameCombatPlugin() {
    private var delay = 10f
    private val interval = IntervalUtil(1f, 2f)

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        delay -= amount
        interval.advance(amount)
        if (delay > 0) return
        if (!interval.intervalElapsed()) return
        val isEnemyPursuing = Helper.combatEngine?.getFleetManager(FleetSide.ENEMY)?.goal == FleetGoal.ATTACK
                && Helper.combatEngine?.getFleetManager(FleetSide.PLAYER)?.goal == FleetGoal.ESCAPE
        val isPlayerPursuing = Helper.combatEngine?.getFleetManager(FleetSide.PLAYER)?.goal == FleetGoal.ATTACK
                && Helper.combatEngine?.getFleetManager(FleetSide.ENEMY)?.goal == FleetGoal.ESCAPE
        deployEnemies(isEnemyPursuing)
        deployAllies(isPlayerPursuing)
    }

    private fun deployEnemies(isPursuitBattle: Boolean) {
        val side = Helper.combatEngine?.getFleetManager(FleetSide.ENEMY)
        if (side?.goal == FleetGoal.ESCAPE) return
        val spawnAngle = if (isPursuitBattle) 90f else 270f
        val burnDuration = if (isPursuitBattle) 1f else 3f
//        side?.isSuppressDeploymentMessages = true

        side?.reservesCopy
            ?.filter { it.stats?.hasListenerOfClass(DummyListener::class.java) == true }
            ?.filter { canDeploy(it, side) }
            ?.forEach {
                side.spawnFleetMember(it, getSpawnLocation(FleetSide.ENEMY), spawnAngle, burnDuration)
                side.removeFromReserves(it)
            }
//        side?.isSuppressDeploymentMessages = false
    }

    private fun deployAllies(isPursuitBattle: Boolean) {
        val side = Helper.combatEngine?.getFleetManager(FleetSide.PLAYER)
        if (side?.goal == FleetGoal.ESCAPE) return
        val burnDuration = if (isPursuitBattle) 1f else 3f
//        side?.isSuppressDeploymentMessages = true

        side?.reservesCopy
            ?.filter { it.isAlly }
            ?.filter { it.stats?.hasListenerOfClass(DummyListener::class.java) == true }
            ?.filter { canDeploy(it, side) }
            ?.forEach {
                side.spawnFleetMember(it, getSpawnLocation(FleetSide.PLAYER), 90f, burnDuration)
                side.removeFromReserves(it)
            }
//        side?.isSuppressDeploymentMessages = false
    }

    private fun canDeploy(m: FleetMemberAPI?, side: CombatFleetManagerAPI): Boolean {
        if (m == null) return false
        return side.maxStrength >= (side.currStrength + m.deploymentPointsCost)
    }

    private fun getSpawnLocation(side: FleetSide): Vector2f {
        val spawnLocation = Vector2f()
        spawnLocation.x = MathUtils.getRandomNumberInRange(-(Helper.combatEngine?.mapWidth ?: 0f) / 2, (Helper.combatEngine?.mapWidth ?: 0f) / 2)
        val y = (Helper.combatEngine?.mapHeight ?: 0f) / 2f + 1000
        spawnLocation.y = if (side == FleetSide.PLAYER) -y else y
        return spawnLocation
    }
}