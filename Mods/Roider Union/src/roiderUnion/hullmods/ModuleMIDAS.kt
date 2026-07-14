package roiderUnion.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import org.magiclib.util.MagicIncompatibleHullmods
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.helpers.Helper
import roiderUnion.ids.hullmods.CCHullmods
import roiderUnion.ids.ShipsAndWings
import roiderUnion.ids.hullmods.OtherHullmods
import roiderUnion.ids.hullmods.VHullmods

// Copied from SCY_armorChild and modified
class ModuleMIDAS : BaseHullMod() {

    companion object {
        private const val ID = "roider_MIDAS_Armor"

        //</editor-fold>
        // Ship id and list of hullmods
        // Probably needs to be cleared periodically
        @Transient
        val HULLMODS: MutableMap<String?, Set<String>> = HashMap()
        @Transient
        val S_HULLMODS: MutableMap<String?, Set<String>> = HashMap()

        @JvmField
        val PARENT_HULLS: MutableMap<String, String> = HashMap()

        init {
            PARENT_HULLS[ShipsAndWings.ROACH_ARMOR] = ShipsAndWings.ROACH
            PARENT_HULLS[ShipsAndWings.ONAGER_ARMOR] = ShipsAndWings.ONAGER
            PARENT_HULLS[ShipsAndWings.JANE_ARMOR] = ShipsAndWings.JANE
            PARENT_HULLS[ShipsAndWings.AUROCHS_ARMOR] = ShipsAndWings.AUROCHS
            PARENT_HULLS[ShipsAndWings.FIRESTORM_LEFT] = ShipsAndWings.FIRESTORM
            PARENT_HULLS[ShipsAndWings.FIRESTORM_RIGHT] = ShipsAndWings.FIRESTORM
            PARENT_HULLS[ShipsAndWings.GAMBIT_ARMOR] = ShipsAndWings.GAMBIT
            PARENT_HULLS[ShipsAndWings.RANCH_ARMOR] = ShipsAndWings.RANCH
            PARENT_HULLS[ShipsAndWings.RANCH_DAMPER_ARMOR] = ShipsAndWings.RANCH_DAMPER
            PARENT_HULLS[ShipsAndWings.WRECKER_ARMOR] = ShipsAndWings.WRECKER
            PARENT_HULLS[ShipsAndWings.TELAMON_FRONT] = ShipsAndWings.TELAMON
            PARENT_HULLS[ShipsAndWings.TELAMON_LEFT] = ShipsAndWings.TELAMON
            PARENT_HULLS[ShipsAndWings.TELAMON_RIGHT] = ShipsAndWings.TELAMON
        }

        // Some hullmods have size-dependant effects, so they need to be
        // switched to armor module versions
        @JvmField
        val SWITCH: MutableMap<String, String> = HashMap()

        init {
            SWITCH[HullMods.HEAVYARMOR] = RoiderHullmods.HEAVY_ARMOR
            //        HULLMOD_SWITCH.put("CHM_hegemony", Roider_Ids.Hullmods.HEG_COM_CREW);
//        HULLMOD_SWITCH.put("CHM_hegemony_xiv", Roider_Ids.Hullmods.HEG_XIV_COM_CREW);
//        HULLMOD_SWITCH.put("CHM_pirate_xiv", Roider_Ids.Hullmods.HEG_COM_CREW);
        }

        //    public final static Set<String> ALLOWED = new HashSet<>();
        //    static {
        //        ALLOWED.add(HullMods.ARMOREDWEAPONS);
        //        ALLOWED.add(HullMods.BLAST_DOORS);
        //        ALLOWED.add(HullMods.COMP_ARMOR);
        //        ALLOWED.add(HullMods.COMP_HULL);
        //        ALLOWED.add(HullMods.COMP_STRUCTURE);
        //        ALLOWED.add(HullMods.HEAVYARMOR);
        //        ALLOWED.add(HullMods.INSULATEDENGINE);
        //        ALLOWED.add(HullMods.MILITARIZED_SUBSYSTEMS);
        //        ALLOWED.add(HullMods.REINFORCEDHULL);
        //        ALLOWED.add(HullMods.SOLAR_SHIELDING);
        //
        //        // Commissioned Crews
        //        ALLOWED.add("CHM_hegemony");
        //        ALLOWED.add("CHM_hegemony_xiv");
        //        ALLOWED.add("CHM_pirate_xiv");
        //
        //        // SCY
        //        ALLOWED.add("SCY_lightArmor");
        //        ALLOWED.add("SCY_reactiveArmor");
        //
        //        // Shadowyards
        //        ALLOWED.add("ms_fluxLock");
        //
        //        // Underworld
        //        ALLOWED.add("uw_cabal_upgrades");
        //    }

        @JvmField
        val BLOCKED: MutableSet<String> = HashSet()

        init {
            BLOCKED.add(RoiderHullmods.FIGHTER_CLAMPS)
            BLOCKED.add(RoiderHullmods.MIDAS)
            BLOCKED.add(RoiderHullmods.MIDAS_1)
            BLOCKED.add(RoiderHullmods.MIDAS_2)
            BLOCKED.add(RoiderHullmods.MIDAS_3)
            BLOCKED.add(HullMods.CONVERTED_BAY)
            BLOCKED.add(HullMods.CONVERTED_HANGAR)
            BLOCKED.add(HullMods.ACCELERATED_SHIELDS)
            BLOCKED.add(HullMods.ADDITIONAL_BERTHING)
            BLOCKED.add(HullMods.ADVANCEDOPTICS)
            BLOCKED.add(HullMods.ADVANCED_TARGETING_CORE)
            BLOCKED.add(HullMods.AUGMENTEDENGINES)
            BLOCKED.add(HullMods.AUTOMATED)
            BLOCKED.add(HullMods.AUTOREPAIR)
            BLOCKED.add(HullMods.AUXILIARY_FUEL_TANKS)
            BLOCKED.add(HullMods.AUXILIARY_THRUSTERS)
            BLOCKED.add(HullMods.CIVGRADE)
            BLOCKED.add(HullMods.COMP_STORAGE)
            BLOCKED.add(HullMods.DAMAGED_DECK)
            BLOCKED.add(HullMods.DEDICATED_TARGETING_CORE)
            BLOCKED.add(HullMods.DESTROYED_MOUNTS)
            BLOCKED.add(HullMods.ECCM)
            BLOCKED.add(HullMods.ECM)
            BLOCKED.add(HullMods.EFFICIENCY_OVERHAUL)
            BLOCKED.add(HullMods.ERRATIC_INJECTOR)
            BLOCKED.add(HullMods.EXPANDED_CARGO_HOLDS)
            BLOCKED.add(HullMods.EXPANDED_DECK_CREW)
            BLOCKED.add(HullMods.EXTENDED_SHIELDS)
//            BLOCKED.add(HullMods.ESCORT_PACKAGE)
            BLOCKED.add(HullMods.FAULTY_GRID)
            BLOCKED.add(HullMods.FLUXBREAKERS)
            BLOCKED.add(HullMods.FLUX_COIL)
            BLOCKED.add(HullMods.FLUX_DISTRIBUTOR)
            BLOCKED.add(HullMods.FRAGILE_SUBSYSTEMS)
            BLOCKED.add(HullMods.FRONT_SHIELD_CONVERSION)
            BLOCKED.add(HullMods.GLITCHED_SENSORS)
            BLOCKED.add(HullMods.HARDENED_SHIELDS)
            BLOCKED.add(HullMods.HARDENED_SUBSYSTEMS)
            BLOCKED.add(HullMods.ILL_ADVISED)
            BLOCKED.add(HullMods.INCREASED_MAINTENANCE)
            BLOCKED.add(HullMods.INTEGRATED_TARGETING_UNIT)
            BLOCKED.add(HullMods.MAGAZINES)
            BLOCKED.add(HullMods.MAKESHIFT_GENERATOR)
            BLOCKED.add(HullMods.MALFUNCTIONING_COMMS)
            //        BLOCKED.add(HullMods.MILITARIZED_SUBSYSTEMS);
            BLOCKED.add(HullMods.MISSLERACKS)
            BLOCKED.add(HullMods.NAV_RELAY)
            BLOCKED.add(HullMods.OMNI_SHIELD_CONVERSION)
            BLOCKED.add(HullMods.OPERATIONS_CENTER)
            BLOCKED.add(HullMods.PHASE_FIELD)
            BLOCKED.add(HullMods.POINTDEFENSEAI)
            BLOCKED.add(HullMods.RECOVERY_SHUTTLES)
            BLOCKED.add(HullMods.SAFETYOVERRIDES)
            BLOCKED.add(HullMods.SHIELDED_CARGO_HOLDS)
            BLOCKED.add(HullMods.SOLAR_SHIELDING)
            BLOCKED.add(HullMods.STABILIZEDSHIELDEMITTER)
            BLOCKED.add(HullMods.SURVEYING_EQUIPMENT)
            BLOCKED.add(HullMods.TURRETGYROS)
            BLOCKED.add(HullMods.UNSTABLE_COILS)
            BLOCKED.add(HullMods.UNSTABLE_INJECTOR)
            BLOCKED.add(HullMods.NEURAL_INTEGRATOR)
            BLOCKED.add(HullMods.NEURAL_INTERFACE)
            BLOCKED.add(VHullmods.FLUX_SHUNT)
            BLOCKED.add(VHullmods.HIGH_MAINTENANCE)
            BLOCKED.add(VHullmods.DELICATE_SUBSYSTEMS)

            BLOCKED.add(CCHullmods.COM)
            BLOCKED.add(CCHullmods.COM2)
            BLOCKED.add(CCHullmods.ALLIANCE)
            BLOCKED.add(CCHullmods.ALLIANCE2)
            BLOCKED.add(CCHullmods.LEAGUE)
            BLOCKED.add(CCHullmods.TRITACHYON)
            BLOCKED.add(CCHullmods.SINDRIAN)
            BLOCKED.add(CCHullmods.SINDRIAN1)
            BLOCKED.add(CCHullmods.SINDRIAN2)
            BLOCKED.add(CCHullmods.SINDRIAN3)
            BLOCKED.add(CCHullmods.PIRATE)
            BLOCKED.add(CCHullmods.CHURCH)
            BLOCKED.add(CCHullmods.PATH)

            // Ship And Weapon Pack
            BLOCKED.add(OtherHullmods.EXTREME_MODS)
            BLOCKED.add(OtherHullmods.SHIELD_BYPASS)
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats)) return
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)
        val variant: ShipVariantAPI = stats!!.variant
        removeOldHullmods(variant)
        addNewHullmods(variant)
        variant.computeHullModOPCost()
    }

    private fun removeOldHullmods(variant: ShipVariantAPI) {
        val toRemove = ArrayList(variant.hullMods)
        toRemove.remove(RoiderHullmods.MIDAS_ARMOR)
        variant.hullMods.removeAll(toRemove.toSet())
    }

    private fun addNewHullmods(variant: ShipVariantAPI) {
        val allMods = getParentsHullmods(null, variant) ?: return
        val mods = allMods.first
        val sMods = allMods.second

        for (mod in mods) {
//            if (!ALLOWED.contains(mod)) continue;
            if (BLOCKED.contains(mod)) continue

            variant.addPermaMod(resolveHullmod(mod), sMods?.contains(mod) ?: false)
        }
    }

    private fun getParentsHullmods(parent: ShipAPI?, variant: ShipVariantAPI): Pair<Set<String>, Set<String>?>? {
        val mods: Set<String>?
        val sMods: Set<String>?
        if (parent == null) {
            mods = HULLMODS[PARENT_HULLS[variant.hullSpec.baseHullId]]
            if (mods == null) return null
            sMods = S_HULLMODS[PARENT_HULLS[variant.hullSpec.baseHullId]]
        } else {
            mods = HashSet()
            sMods = HashSet()
            for (mod in parent.variant.nonBuiltInHullmods) {
                //            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
                if (BLOCKED.contains(mod)) continue
                mods.add(mod)
            }
            for (mod in parent.variant.permaMods) {
                //            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
                if (BLOCKED.contains(mod)) continue
                val modSpec: HullModSpecAPI = Global.getSettings().getHullModSpec(mod)
                if (modSpec.hasTag(Tags.HULLMOD_DAMAGE)) mods.add(mod)
            }
            for (mod in parent.variant.sMods) {
                //            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
                if (BLOCKED.contains(mod)) continue
                mods.add(mod)
                sMods.add(mod)
            }
            for (mod in parent.variant.sModdedBuiltIns) {
                //            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
                if (BLOCKED.contains(mod)) continue
                mods.add(mod)
                sMods.add(mod)
            }
        }

        return Pair(mods, sMods)
    }

    private fun resolveHullmod(mod: String): String {
        if (SWITCH.containsKey(mod)) return SWITCH[mod]!!
        return mod
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (Helper.anyNull(ship)) return

        if (ship!!.variant.hasHullMod(OtherHullmods.EXTREME_MODS)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                ship.variant,
                OtherHullmods.EXTREME_MODS, RoiderHullmods.MIDAS_ARMOR
            )
        }
        if (ship.variant.hasHullMod(RoiderHullmods.FIGHTER_CLAMPS)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                ship.variant,
                RoiderHullmods.FIGHTER_CLAMPS, RoiderHullmods.MIDAS_ARMOR
            )
        }
    }
}