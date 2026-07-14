package roiderUnion.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.GuidedMissileAI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ids.RoiderIds
import kotlin.math.abs

/**
 * Author: SafariJohn
 */
class HammerDirector : BaseEveryFrameCombatPlugin() {
    private val hammers: MutableSet<MissileAPI> = HashSet()
    override fun advance(amount: Float, events: List<InputEventAPI>) {
        val engine = Global.getCombatEngine() ?: return
        if (engine.isPaused) return
        collectHammers(engine)
        directHammers()
    }

    private fun collectHammers(engine: CombatEngineAPI) {
        val missing: MutableSet<MissileAPI> = HashSet(hammers)
        for (mis in engine.missiles) {
            if (hammers.contains(mis)) {
                missing.remove(mis)
                continue
            }
            if (mis.projectileSpecId == RoiderIds.Projectiles.TRACKER_HAMMER) {
                hammers.add(mis)
            }
        }
        for (mis in missing) {
            hammers.remove(mis)
        }
    }

    private fun directHammers() {
        for (ham in hammers) {
            if (ham.isExpired) continue
            if (ham.engineController?.isFlamedOut == true) continue
            if (ham.engineController?.isAccelerating == false) {
                ham.giveCommand(ShipCommand.ACCELERATE)
            }
            if (ham.ai is GuidedMissileAI) {
                val target = (ham.ai as GuidedMissileAI).target ?: continue
                val hamPos = ham.location ?: continue
                val tarPos = target.location ?: continue
                if (hasMissed(hamPos, tarPos)) {
                    ham.missileAI = DummyMissileAI()
                }
            }
        }
    }

    private fun hasMissed(hamPos: Vector2f, tarPos: Vector2f): Boolean {
        val difFace = abs(Misc.getAngleInDegrees(hamPos, tarPos))
        return difFace > 90f
    }
}