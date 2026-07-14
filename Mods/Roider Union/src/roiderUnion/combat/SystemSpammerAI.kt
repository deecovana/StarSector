package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState
import com.fs.starfarer.api.plugins.ShipSystemStatsScriptAdvanced
import com.fs.starfarer.api.util.IntervalUtil
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class SystemSpammerAI : ShipSystemAIScript {
    private lateinit var ship: ShipAPI
    private lateinit var system: ShipSystemAPI
    private var script: ShipSystemStatsScriptAdvanced? = null
    private var interval = IntervalUtil(0.08f, 0.12f)
    override fun init(ship: ShipAPI?, system: ShipSystemAPI?, flags: ShipwideAIFlags?, engine: CombatEngineAPI?) {
        if (Helper.anyNull(ship, system)) return

        this.ship = ship!!
        this.system = system!!
        script = system.specAPI.statsScript as? ShipSystemStatsScriptAdvanced
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        if (Helper.anyNull(ship, system)) return

        interval.advance(amount)
        if (interval.intervalElapsed()) {
            if (!ship.isAlive) return
            if (system.state == SystemState.IDLE
                && ship.owner != target?.owner
                && (script == null || script!!.isUsable(system, ship))
            ) {
                ship.useSystem()
            }
        }
    }
}