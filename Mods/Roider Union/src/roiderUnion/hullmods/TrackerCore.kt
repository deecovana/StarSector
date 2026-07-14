package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.helpers.Helper
import roiderUnion.hullmods.TrackerCoreShip.Companion.MAX_SPEED
import roiderUnion.hullmods.TrackerCoreShip.Companion.SPEED_BOOST

/**
 * Author: SafariJohn
 */
class TrackerCore : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats, id)) return

        stats?.autofireAimAccuracy?.modifyFlat(id, 1f)
        stats?.eccmChance?.modifyFlat(id, 1f)
        stats?.dynamic?.getMod(Stats.PD_IGNORES_FLARES)?.modifyFlat(id, 1f)
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
        return when (index) {
            0 -> ExternalStrings.NUMBER_PLUS.replaceNumberToken(SPEED_BOOST.toInt())
            1 -> MAX_SPEED
            else -> ExternalStrings.DEBUG_NULL
        }
    }
}