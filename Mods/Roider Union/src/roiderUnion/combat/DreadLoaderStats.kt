package roiderUnion.combat

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.RoiderIds
import roiderUnion.helpers.Helper
import java.awt.Color

class DreadLoaderStats : BaseShipSystemScript() {
    companion object {
        const val DAMAGE_BONUS = 50f
        const val ROF_BONUS = 33f
        const val FLUX_BONUS = 25f
        const val TOKEN_BONUS = "\$bonus"

        val GLOW_COLOR = Color(255, 0, 0, 255)
    }

    override fun apply(stats: MutableShipStatsAPI?, id: String, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        if (Helper.anyNull(stats)) return

        val damageBonus = 1f + DAMAGE_BONUS / 100f * effectLevel
        stats!!.ballisticWeaponDamageMult?.modifyMult(id, damageBonus)
        val rofBonus = 1f + ROF_BONUS / 100f * effectLevel
        stats.ballisticRoFMult?.modifyMult(id, rofBonus)
        val fluxBonus = 1f - FLUX_BONUS / 100f * effectLevel
        stats.ballisticWeaponFluxCostMod?.modifyMult(id, fluxBonus)

        val ship = stats.entity as? ShipAPI ?: return

        // Copied visuals code with permission from MesoTroniK's tiandong_fluxOverrideStats.java
        var glowDeco: WeaponAPI? = null
        for (weapon in ship.allWeapons) {
            if (weapon.id.startsWith(RoiderIds.PREFIX) && weapon.id.endsWith("_glow")) {
                glowDeco = weapon
                break
            }
        }

        ship.setJitter(ship, GLOW_COLOR, effectLevel / 30f, 6, 30f)
        ship.setJitterUnder(ship, GLOW_COLOR, effectLevel / 8f, 10, 50f)

        if (glowDeco != null && glowDeco.animation != null) {
            glowDeco.sprite.setAdditiveBlend()
            glowDeco.animation.alphaMult = effectLevel
            glowDeco.animation.frame = 1
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        stats.ballisticWeaponDamageMult?.unmodify(id)
        stats.ballisticRoFMult?.unmodify(id)
        stats.ballisticWeaponFluxCostMod?.unmodify(id)
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        val bonusPercent = DAMAGE_BONUS * effectLevel
        val rofBonus = ROF_BONUS * effectLevel
        val fluxBonus = FLUX_BONUS * effectLevel
        return when (index) {
            0 -> {
                StatusData(ExternalStrings.DREAD_FLUX_BUFF.replace(TOKEN_BONUS, fluxBonus.toInt().toString()), false)
            }

            1 -> {
                StatusData(ExternalStrings.DREAD_ROF_BUFF.replace(TOKEN_BONUS, rofBonus.toInt().toString()), false)
            }

            2 -> {
                StatusData(ExternalStrings.DREAD_DAM_BUFF.replace(TOKEN_BONUS, bonusPercent.toInt().toString()), false)
            }

            else -> null
        }
    }
}