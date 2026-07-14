package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 * Activates ship system when parent is using its system. Is supposed to allow defense sync, too, but that doesn't work.
 */
open class SlavedSystemScript : ShipSystemAIScript {
    private var delegate: Delegate? = null

    private class Delegate(val ship: ShipAPI, val isDefense: Boolean) {
        fun advance() {
            if (!ship.isAlive) return
            if (isDefense) {
                if (parentDefenseActive() && !defenseActive()) activate()
                if (!parentDefenseActive() && defenseActive()) deactivate()
            } else {
                if (parentSystemActive() && !systemActive()) activate()
                if (!parentSystemActive() && systemActive()) deactivate()
            }
        }

        private fun parentSystemActive(): Boolean {
            val parent = ship.parentStation ?: return false
            if (!parent.isAlive) return false
            return parent.system?.isActive == true
        }

        private fun systemActive(): Boolean {
            return ship.system?.isActive == true
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
            if (isDefense) ship.phaseCloak?.forceState(ShipSystemAPI.SystemState.IN, 0f)
            else ship.useSystem()
        }

        private fun deactivate() {
            if (isDefense) ship.phaseCloak?.deactivate()
            else ship.system?.deactivate()
        }
    }

    override fun init(ship: ShipAPI?, system: ShipSystemAPI?, flags: ShipwideAIFlags?, engine: CombatEngineAPI?) {
        if (Helper.anyNull(ship, system)) return
        delegate = Delegate(ship!!, system!! == ship.phaseCloak) // broken for defense systems
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        delegate?.advance()
    }
}