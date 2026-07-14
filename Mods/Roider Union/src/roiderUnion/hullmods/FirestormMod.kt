package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.hullmods.BallisticRangefinder
import org.magiclib.util.MagicIncompatibleHullmods
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.helpers.Helper
import roiderUnion.ids.hullmods.VHullmods

/**
 * Author: SafariJohn
 */
class FirestormMod : BaseHullMod() {
    companion object {
        const val COST_MOD = 4f
        val BALLISTIC_BONUS = BallisticRangefinder.BONUS_SMALL_1
        val HYBRID_BONUS = BALLISTIC_BONUS * BallisticRangefinder.HYBRID_MULT
        val MAX_BONUS = BallisticRangefinder.BONUS_MAX_1
        val MIN_HYBRID_BONUS = BallisticRangefinder.HYBRID_BONUS_MIN
    }

    override fun hasSModEffect(): Boolean = true

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats)) return

        stats!!.dynamic.getMod(Stats.MEDIUM_BALLISTIC_MOD).modifyFlat(id, -COST_MOD)
        stats.dynamic.getMod(Stats.SMALL_BALLISTIC_MOD).modifyFlat(id, -COST_MOD)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (Helper.anyNull(ship)) return

        if (ship!!.variant.hasHullMod(VHullmods.RANGEFINDER)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                ship.variant,
                VHullmods.RANGEFINDER, RoiderHullmods.FIRESTORM_MOD
            )
        }

        if (isSMod(ship)) {
            BallisticRangefinder().applyEffectsAfterShipCreation(ship, id)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
        return if (index == 0) COST_MOD.toInt().toString() else ExternalStrings.DEBUG_NULL
    }

    override fun getSModDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
        return when (index) {
            0 -> ExternalStrings.NUMBER_PLUS.replaceNumberToken(BALLISTIC_BONUS.toInt())
            1 -> ExternalStrings.NUMBER_PLUS.replaceNumberToken(HYBRID_BONUS.toInt())
            2 -> MAX_BONUS.toInt().toString()
            3 -> ExternalStrings.NUMBER_PLUS.replaceNumberToken(MIN_HYBRID_BONUS.toInt())
            else -> ExternalStrings.DEBUG_NULL
        }
    }

    override fun affectsOPCosts(): Boolean = true

    override fun isApplicableToShip(ship: ShipAPI?): Boolean = false
}