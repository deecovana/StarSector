package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.magiclib.util.MagicIncompatibleHullmods
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.helpers.Helper
import java.util.*

class FighterClamps : BaseHullMod() {
    companion object {
        const val ALL_FIGHTER_COST_PERCENT = 50
        const val BOMBER_COST_MOD = 10000f
        private val CREW_REQ: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)

        init {
            CREW_REQ[ShipAPI.HullSize.FRIGATE] = 10f
            CREW_REQ[ShipAPI.HullSize.DESTROYER] = 10f
            CREW_REQ[ShipAPI.HullSize.CRUISER] = 30f
            CREW_REQ[ShipAPI.HullSize.CAPITAL_SHIP] = 70f
        }

        private val BAYS: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)

        init {
            BAYS[ShipAPI.HullSize.FRIGATE] = 1f
            BAYS[ShipAPI.HullSize.DESTROYER] = 1f
            BAYS[ShipAPI.HullSize.CRUISER] = 2f
            BAYS[ShipAPI.HullSize.CAPITAL_SHIP] = 4f
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(hullSize, stats, id)) return
        if (!CREW_REQ.containsKey(hullSize) || !BAYS.containsKey(hullSize)) return

        stats!!.minCrewMod.modifyFlat(id, CREW_REQ[hullSize]!!)
        stats.fighterRefitTimeMult.modifyMult(id, 1000000f)
        val builtInWingsSize = stats.variant?.hullSpec?.builtInWings?.size?.toFloat() ?: 0f
        stats.numFighterBays.modifyFlat(id, BAYS[hullSize]!! + builtInWingsSize)
        stats.dynamic.getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, 1000000f)
        stats.dynamic.getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyMult(id, 0f)

        stats.dynamic.getMod(Stats.BOMBER_COST_MOD).modifyFlat(id, BOMBER_COST_MOD)
        stats.dynamic.getMod(Stats.FIGHTER_COST_MOD).modifyPercent(id, ALL_FIGHTER_COST_PERCENT.toFloat())
        stats.dynamic.getMod(Stats.INTERCEPTOR_COST_MOD).modifyPercent(id, ALL_FIGHTER_COST_PERCENT.toFloat())
        stats.dynamic.getMod(Stats.SUPPORT_COST_MOD).modifyPercent(id, ALL_FIGHTER_COST_PERCENT.toFloat())

        val variant = stats.variant ?: return
        val builtInWings = variant.hullSpec?.builtInWings ?: return
        if (builtInWings.size > 0) {
            val wings = variant.wings ?: return
            val size = wings.size.coerceAtMost(builtInWings.size)
            for (i in 0 until size) {
                if (wings[i] == builtInWings[i]) wings[i] = ""
            }
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (Helper.anyNull(ship)) return

        if (ship!!.variant.hasHullMod(HullMods.CONVERTED_HANGAR)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                ship.variant,
                HullMods.CONVERTED_HANGAR, RoiderHullmods.FIGHTER_CLAMPS
            )
        }

        // Trying to block Fighter Clamps on armor modules.
        if (ship.hullSpec.getOrdnancePoints(null) <= 1) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                ship.variant,
                RoiderHullmods.FIGHTER_CLAMPS, RoiderHullmods.MIDAS_ARMOR
            )
        }
    }

    override fun isApplicableToShip(ship: ShipAPI?): Boolean {
        return if (ship == null) false
        else if (ship.hullSpec?.shipDefenseId == "phasecloak") false
        else if (ship.hullSpec.baseHullId.startsWith("swp_arcade")) false
        else if (ship.variant.hasHullMod(RoiderHullmods.MIDAS_ARMOR)) false
        else  if (ship.engineController.shipEngines.isEmpty() && ship.hullSpec.getOrdnancePoints(null) <= 1) false
        else if (ship.variant?.hasHullMod(RoiderHullmods.GARRUCHA_DRAW) == true) false
//        else if (GarruchaDraw.VALID_HULLS.contains(ship.hullSpec?.baseHullId)) false
        else (ship.hullSpec.fighterBays <= 0 && !ship.variant.hasHullMod(HullMods.CONVERTED_HANGAR)
                && !ship.variant.hasHullMod(HullMods.CONVERTED_BAY)
                && !ship.variant.hasHullMod(HullMods.PHASE_FIELD))
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        if (ship == null) return ExternalStrings.HULLMOD_CANT_INSTALL
        if (ship.hullSpec.baseHullId.startsWith("swp_arcade")) {
            return ExternalStrings.HULLMOD_NO_ARCADE
        }
        val armorDesc = ExternalStrings.HULLMOD_NO_ARMOR_MODULE
        if (ship.variant.hasHullMod(RoiderHullmods.MIDAS_ARMOR)) return armorDesc
        if (ship.engineController.shipEngines.isEmpty()
            && ship.hullSpec.getOrdnancePoints(null) <= 1) {
            return armorDesc
        }
        if (ship.hullSpec.fighterBays > 0) return ExternalStrings.HULLMOD_STD_FIGHTERS
        if (ship.variant.hasHullMod(HullMods.CONVERTED_HANGAR)) return ExternalStrings.HULLMOD_HAS_FIGHTERS
        if (ship.variant.hasHullMod(HullMods.CONVERTED_BAY)) return ExternalStrings.HULLMOD_HAS_FIGHTERS
        if (ship.variant.hasHullMod(RoiderHullmods.GARRUCHA_DRAW)) return ExternalStrings.HULLMOD_HAS_FIGHTERS
        return if (ship.hullSpec?.shipDefenseId == "phasecloak" || ship.variant.hasHullMod(HullMods.PHASE_FIELD)) ExternalStrings.HULLMOD_NO_PHASE
        else ExternalStrings.HULLMOD_CANT_INSTALL
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?, ship: ShipAPI?): String? {
        if (index == 0) return "1/1/2/4"
        if (index == 1 && CREW_REQ.containsKey(hullSize)) return CREW_REQ[hullSize]!!.toInt().toString()
        return if (index == 2) ExternalStrings.NUMBER_PERCENT.replaceNumberToken(ALL_FIGHTER_COST_PERCENT)
        else null
    }

    override fun affectsOPCosts(): Boolean = true
}