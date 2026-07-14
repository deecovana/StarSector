package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Settings

class RotaryHammerAmmoLights {
    companion object {
        const val THREE_AMMO_TURRET_SPRITE = "rotary_hammer_3ammoTurret"
        const val TWO_AMMO_TURRET_SPRITE = "rotary_hammer_2ammoTurret"
        const val THREE_AMMO_HP_SPRITE = "rotary_hammer_3ammoHP"
        const val TWO_AMMO_HP_SPRITE = "rotary_hammer_2ammoHP"
        private val LIGHTS_COLOR = Misc.getPositiveHighlightColor()
    }

    private val interval = IntervalUtil(0.05f, 0.05f)

    fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?, drumAmmo: Int) {
        if (Helper.anyNull(engine, weapon?.ship)) return
        val ship = weapon!!.ship
        if (engine!!.playerShip.id != ship.id) return

        if (drumAmmo == 0) return

        interval.advance(amount)
        if (interval.intervalElapsed()) {
            val glowSprite: SpriteAPI = getSprite(weapon, drumAmmo) ?: return
            val offset = getOffset(glowSprite, weapon)
            val angle = weapon.currAngle - ship.facing + 180f
            MagicRender.objectspace(
                glowSprite, ship, offset, Vector2f(),
                Vector2f(glowSprite.width, glowSprite.height), Vector2f(),
                angle, 0f, true, LIGHTS_COLOR, true,
                0f, 0.06f, 0f,
                true
            )
        }
    }

    private fun getOffset(glowSprite: SpriteAPI, weapon: WeaponAPI): Vector2f {
        val result = Vector2f(weapon.slot?.location ?: Vector2f())
        val isHardpoint = weapon.slot?.isHardpoint == true
        if (isHardpoint) result.x += glowSprite.height * 0.25f
        return result

    }

    private fun getSprite(weapon: WeaponAPI, drumAmmo: Int): SpriteAPI? {
        val isTurret = weapon.slot?.isTurret == true
        val isHardpoint = weapon.slot?.isHardpoint == true
        val isMaxAmmo = drumAmmo == RotaryHammerReloadScript.MAX_AMMO
        val canCycle = drumAmmo > 1

        val spriteId = if (isTurret && isMaxAmmo) THREE_AMMO_TURRET_SPRITE
            else if (isTurret && canCycle) TWO_AMMO_TURRET_SPRITE
            else if (isHardpoint && isMaxAmmo) THREE_AMMO_HP_SPRITE
            else if (isHardpoint && canCycle) TWO_AMMO_HP_SPRITE
            else return null

        return Helper.settings?.getSprite(Settings.GRAPHICS_COMBAT, spriteId)
    }

}
