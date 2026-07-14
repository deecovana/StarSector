package roiderUnion.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags
import com.fs.starfarer.api.input.InputEventAPI
import roiderUnion.ids.RoiderTags

/**
 * Trying to stop fighters from auto-escorting armor modules
 *
 * Author: SafariJohn
 */
class ArmorNoEscort : BaseEveryFrameCombatPlugin() {
    private var elapsed = 0f
    private val INTERVAL = 0.05f
    override fun advance(amount: Float, events: List<InputEventAPI>) {
        val engine = Global.getCombatEngine()
        if (engine.isPaused) return
        if (engine.isUIShowingDialog) return
        elapsed += amount
        if (elapsed < INTERVAL) return
        elapsed -= INTERVAL

        for (ship in engine.ships) {
            val flags = ship.aiFlags
            if (ship.hullSpec.tags.contains(RoiderTags.ARMOR_MODULE)) {
                flags.unsetFlag(AIFlags.NEEDS_HELP)
                flags.removeFlag(AIFlags.NEEDS_HELP)
            }
            if (flags.hasFlag(AIFlags.CARRIER_FIGHTER_TARGET)) {
                val target = flags.getCustom(AIFlags.CARRIER_FIGHTER_TARGET)
                if (target is ShipAPI) {
                    if (target.hullSpec.tags.contains(RoiderTags.ARMOR_MODULE)
                        && ship.owner == target.owner
                    ) {
                        flags.unsetFlag(AIFlags.CARRIER_FIGHTER_TARGET)
                    }
                }
            }
        }
    }
}