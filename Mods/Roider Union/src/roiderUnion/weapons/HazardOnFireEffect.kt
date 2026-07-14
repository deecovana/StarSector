package roiderUnion.weapons

import com.fs.starfarer.api.combat.*

class HazardOnFireEffect : OnFireEffectPlugin {
    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI) {
        if (projectile is MissileAPI) {
            engine.addPlugin(HazardBoomPlugin(projectile))
        }
    }
}