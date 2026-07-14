package roiderUnion.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import roiderUnion.combat.RoiderFlaresScript
import roiderUnion.helpers.Helper

class RoiderFlaresOnFire : OnFireEffectPlugin {
    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        if (Helper.anyNull(projectile, engine)) return
        if (projectile !is MissileAPI) return
        engine!!.addPlugin(RoiderFlaresScript(projectile))
    }
}