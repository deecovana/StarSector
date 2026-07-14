package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class Outrider : BaseHullMod() {
    companion object {
        const val WEAPON_COST_MOD = 3f
        const val BOMBER_COST_MOD = 10000
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats, id)) return
        stats?.dynamic?.getMod(Stats.SMALL_BALLISTIC_MOD)?.modifyFlat(id, -WEAPON_COST_MOD)
        stats?.dynamic?.getMod(Stats.MEDIUM_BALLISTIC_MOD)?.modifyFlat(id, -WEAPON_COST_MOD)
        stats?.dynamic?.getMod(Stats.SMALL_ENERGY_MOD)?.modifyFlat(id, -WEAPON_COST_MOD)
        stats?.dynamic?.getMod(Stats.MEDIUM_ENERGY_MOD)?.modifyFlat(id, -WEAPON_COST_MOD)
        stats?.dynamic?.getMod(Stats.BOMBER_COST_MOD)?.modifyFlat(id, BOMBER_COST_MOD.toFloat())
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String {
        return WEAPON_COST_MOD.toInt().toString()
    }

    override fun affectsOPCosts(): Boolean {
        return true
    }
}