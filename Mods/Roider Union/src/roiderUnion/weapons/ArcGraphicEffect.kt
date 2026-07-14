package roiderUnion.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import roiderUnion.helpers.Helper

class ArcGraphicEffect : OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
    companion object {
        @JvmField
        val ZAP_GRAPHICS = mutableMapOf(
            Pair("roider_arcZapper", 3), // extern
            Pair("roider_arcCannon", 3)
        )
        @JvmField
        val ZAP_DURATION = mutableMapOf(
            Pair("roider_arcZapper", 0.1f), // extern
            Pair("roider_arcCannon", 0.15f)
        )
    }

    private var fired = false
    private var duration = -1f
    private var lastFrame = 0

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        if (weapon == null) return
        fired = true
        val weaponId = weapon.spec?.weaponId ?: return
        duration = ZAP_DURATION.getOrElse(weaponId) { return }
    }

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        if (weapon == null) return
        if (duration > 0) duration -= amount
        else {
            duration = -1f
            weapon.animation?.frame = 0
        }
        if (fired) {
            fired = false
            val weaponId = weapon.spec?.weaponId ?: return
            val bounds = ZAP_GRAPHICS.getOrElse(weaponId) { return }
            var frame = Helper.random.nextInt(bounds) + 1
            while (frame == lastFrame) frame = Helper.random.nextInt(bounds) + 1
            weapon.animation?.frame = frame
            lastFrame = frame
        }
    }
}