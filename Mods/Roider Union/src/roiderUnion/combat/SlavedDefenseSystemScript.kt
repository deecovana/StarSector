package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 * Activates ship defense system when parent is using its defense.
 */
class SlavedDefenseSystemScript : ShipSystemAIScript {
    private var delegate: Delegate? = null

    private class Delegate(val ship: ShipAPI) {
        fun advance() {
            if (!ship.isAlive) return
            if (parentDefenseActive() && !defenseActive()) activate()
            if (!parentDefenseActive() && defenseActive()) deactivate()

        }

        private fun parentDefenseActive(): Boolean {
            val parent = ship.parentStation ?: return false
            if (!parent.isAlive) return false
            if (parent.isDefenseDisabled) return false
            return parent.phaseCloak?.isActive == true
        }

        private fun defenseActive(): Boolean {
            return ship.phaseCloak?.isActive == true
        }

        private fun activate() {
            ship.phaseCloak?.forceState(ShipSystemAPI.SystemState.IN, 0f)
        }

        private fun deactivate() {
            ship.phaseCloak?.deactivate()
        }
    }

    override fun init(ship: ShipAPI?, system: ShipSystemAPI?, flags: ShipwideAIFlags?, engine: CombatEngineAPI?) {
        if (Helper.anyNull(ship)) return
        delegate = Delegate(ship!!)
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        delegate?.advance()
    }
}