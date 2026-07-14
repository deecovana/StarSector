package roiderUnion.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import roiderUnion.ids.RoiderTags

/**
 * Description: Deletes destroyed armor plates so we don't have them
 * floating around or absorbing more damage than they should.
 * Doesn't affect station armor.
 * Author: SafariJohn
 */
class ArmorCleaner : BaseEveryFrameCombatPlugin() {
    private var elapsed = 0f
    private val INTERVAL = 0.05f
    override fun advance(amount: Float, events: List<InputEventAPI>) {
        val engine = Global.getCombatEngine()
        if (engine.isPaused) return
        if (engine.isUIShowingDialog) return
        elapsed += amount
        if (elapsed < INTERVAL) return
        elapsed -= INTERVAL

        val destroyedArmor = ArrayList<ShipAPI>()
        for (ship in engine.ships) {
            if (!ship.isAlive && ship.hullSpec.tags.contains(RoiderTags.ARMOR_MODULE)) {
                destroyedArmor.add(ship)
            }
        }

        for (armor in destroyedArmor) {
            engine.removeEntity(armor)
        }
    }
}