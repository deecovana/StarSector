package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.FighterLaunchBayAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.magiclib.util.MagicIncompatibleHullmods
import roiderUnion.ids.hullmods.RoiderHullmods

/**
 * Author: SafariJohn
 */
class DroneReserves : BaseHullMod() {
    companion object {
        private class ReservesListener(private val ship: ShipAPI) : AdvanceableListener {
            @Transient
            val wingCounts = mutableMapOf<FighterLaunchBayAPI, Int>()

            override fun advance(amount: Float) {
                ship.launchBaysCopy.forEach { bay ->
                    if (bay.wing != null) {
                        val wing = bay.wing
                        val count = wingCounts[bay] ?: 0
                        if (wing.wingMembers.size > count) {
                            val maxTotal = wing.spec.numFighters
                            val numToAdd = maxTotal - wing.wingMembers.size
                            bay.fastReplacements = bay.fastReplacements + numToAdd
                            wingCounts[bay] = maxTotal
                        } else {
                            wingCounts[bay] = wing.wingMembers.size + bay.fastReplacements
                        }
                    }
                }
            }
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.dynamic.getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 0f)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.addListener(ReservesListener(ship))
        if (ship.variant.hasHullMod(HullMods.EXPANDED_DECK_CREW)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                ship.variant,
                HullMods.EXPANDED_DECK_CREW, RoiderHullmods.DRONE_RESERVES
            )
        }
    }
}