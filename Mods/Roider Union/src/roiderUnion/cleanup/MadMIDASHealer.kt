package roiderUnion.cleanup

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.hullmods.RoiderHullmods

/**
 * Fixes bug where build-in menu de-permafies built in MIDAS
 * Author: SafariJohn
 */
class MadMIDASHealer : EveryFrameScript {
    companion object {
        fun alias(x: XStream) {
            val jClass = MadMIDASHealer::class.java
            x.alias(Aliases.MADHLR, jClass)
            x.aliasAttribute(jClass, "interval", "i")
        }

        private val madMidasShips = mutableSetOf<String>()
        fun isMadMidas(memberId: String?): Boolean {
            if (memberId == null) return false
            return madMidasShips.contains(memberId)
        }

        fun healLostMIDAS(m: FleetMemberAPI?) {
            if (m?.variant == null) return
            if (isMadMidas(m.id) && !Helper.hasBuiltInMIDAS(m)) {
                forceHealMIDAS(m.variant)
            }
        }

        fun forceHealMIDAS(variant: ShipVariantAPI?) {
            if (variant == null) return
            variant.sMods -= RoiderHullmods.MIDAS
            variant.nonBuiltInHullmods -= RoiderHullmods.MIDAS
            variant.hullMods += RoiderHullmods.MIDAS
            variant.permaMods += RoiderHullmods.MIDAS
            variant.sModdedBuiltIns += RoiderHullmods.MIDAS
        }

        fun rebuildShipList() {
            madMidasShips.clear()
            Helper.sector?.playerFleet?.membersWithFightersCopy
                ?.filter { it.variant.permaMods.contains(RoiderHullmods.MIDAS) && it.variant.sModdedBuiltIns.contains(RoiderHullmods.MIDAS) }
                ?.forEach { madMidasShips.add(it.id) }

        }
    }

    private val interval = IntervalUtil(1f, 1f)

    init {
        madMidasShips.clear()
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        if (Helper.sector?.campaignUI?.currentCoreTab != CoreUITabId.REFIT) return
        interval.advance(amount)
        if (!interval.intervalElapsed()) return
        Helper.sector?.playerFleet?.membersWithFightersCopy?.forEach { healLostMIDAS(it) }
        rebuildShipList()
    }
}