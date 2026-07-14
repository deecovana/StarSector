package roiderUnion.weapons

import com.fs.starfarer.api.combat.*

/**
 * Author: SafariJohn
 */
class ZapOnFireEffect : OnFireEffectPlugin {
    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI) {
        if (projectile is MissileAPI) {
            engine.addPlugin(ZapArcKeeper(projectile))
        }
    }
}