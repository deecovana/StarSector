package roiderUnion.combat

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.resize
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper

class RoiderFlaresScript(private val flare: MissileAPI) : BaseEveryFrameCombatPlugin() {
    companion object {
        const val ORBIT_DIST_MULT = 1.1f
        const val PURSUIT_DIST = 200f
        const val DEFEND_DIST = 500f
        const val DEFEND_DIST_MULT = 1.1f
        const val ORBIT_DEGREES_PER_INTERVAL = 10f
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (flare.isFading || flare.isExpired || flare.source == null) Helper.combatEngine?.removePlugin(this)
        if (Helper.combatEngine?.missiles?.contains(flare) == false) Helper.combatEngine?.removePlugin(this)
        val fLoc = flare.location
        val sLoc = flare.source.location
        val radius = flare.source.collisionRadius
        if (Helper.combatEngine?.missiles?.any { isMissileInRange(it, fLoc, radius) } == true) return

        val angle = Misc.getAngleInDegrees(fLoc, sLoc) + ORBIT_DEGREES_PER_INTERVAL - 180f
        val orbitLoc = Misc.getUnitVectorAtDegreeAngle(angle).resize(radius * ORBIT_DIST_MULT).plus(sLoc)
        val toOrbitAngle = Misc.getAngleInDegrees(fLoc, orbitLoc)
        Helper.combatEngine?.headInDirectionWithoutTurning(flare, toOrbitAngle, 10000f)
    }

    private fun isMissileInRange(missile: MissileAPI, fLoc: Vector2f, radius: Float): Boolean {
        return flare.owner != missile.owner
                && (Misc.getDistance(fLoc, missile.location) < PURSUIT_DIST
                || Misc.getDistance(fLoc, missile.location) < DEFEND_DIST + radius * DEFEND_DIST_MULT)
    }
}