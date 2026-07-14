package roiderUnion.combat

import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.GuidedMissileAI
import com.fs.starfarer.api.combat.MissileAIPlugin

/**
 * Author: SafariJohn
 */
class DummyMissileAI : MissileAIPlugin, GuidedMissileAI {
    override fun advance(amount: Float) {}
    override fun getTarget(): CombatEntityAPI? = null
    override fun setTarget(target: CombatEntityAPI?) {}
}