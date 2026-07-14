package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class PileDriverReload : OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
    private val smoke: PileDriverSmoke = PileDriverSmoke()
    private var fired = false
    private var currentCooldown = 0f
    private var cooldownRemaining = 0f

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        if (Helper.anyNull(engine, weapon)) return

        smoke.advance(amount, engine!!, weapon!!)

        if (fired) {
            fired = false
            beginCooldown(weapon)
        }

        if (cooldownRemaining > 0) {
            coolWeapon(amount, weapon)
        }
    }

    private fun beginCooldown(weapon: WeaponAPI) {
        val baseCooldown = weapon.cooldown
        var maxSpread = weapon.spec.maxSpread * weapon.ship.mutableStats.maxRecoilMult.modifiedValue
        if (weapon.slot.isHardpoint) maxSpread /= 2f
        currentCooldown = baseCooldown + (baseCooldown - weapon.currSpread / maxSpread * baseCooldown)
        cooldownRemaining = currentCooldown

    }

    private fun coolWeapon(amount: Float, weapon: WeaponAPI) {
        // Cooldown is off for some reason compared to unmodified weapon cooldown
        if (cooldownRemaining < amount * 3f) {
            cooldownRemaining = 0f
            weapon.setRemainingCooldownTo(0f)
        } else {
            cooldownRemaining -= amount * weapon.ship.mutableStats.ballisticRoFMult.modifiedValue
            weapon.setRemainingCooldownTo(cooldownRemaining / currentCooldown)
        }
    }

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        fired = true
    }
}