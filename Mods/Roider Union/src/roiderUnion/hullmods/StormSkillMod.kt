package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier
import com.fs.starfarer.api.loading.WeaponSpecAPI

class StormSkillMod : BaseHullMod(), WeaponOPCostModifier {
    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
//        if (stats?.getListeners(WeaponOPCostModifier::class.java)?.contains(this) == false) {
//            stats.addListener(this)
//        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
//        if (ship?.mutableStats?.listenerManager?.hasListener(this) == false) {
//            ship.mutableStats?.listenerManager?.addListener(this)
//        }
    }

//    override fun affectsOPCosts(): Boolean = true

    override fun getWeaponOPCost(stats: MutableShipStatsAPI?, weapon: WeaponSpecAPI?, currCost: Int): Int {
        return currCost
//        return when (weapon?.size) {
//            WeaponAPI.WeaponSize.SMALL -> {
//                if (weapon.maxRange < Storm.SMALL_RANGE_CUTOFF) currCost - Storm.OP_REDUCTION_SMALL else currCost
//            }
//            WeaponAPI.WeaponSize.MEDIUM -> {
//                if (weapon.maxRange < Storm.MEDIUM_RANGE_CUTOFF) currCost - Storm.OP_REDUCTION_MEDIUM else currCost
//            }
//            WeaponAPI.WeaponSize.LARGE -> {
//                if (weapon.maxRange < Storm.LARGE_RANGE_CUTOFF) currCost - Storm.OP_REDUCTION_LARGE else currCost
//            }
//            else -> currCost
//        }
    }
}