package roiderUnion.helpers

import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import java.util.*

/**
 * Author: SafariJohn
 */
object DModHelper : DModManager() {
    fun addDShipToFleet(
        numDMods: Int,
        side: FleetSide, variantId: String, name: String?,
        isFlagship: Boolean, random: Random = Helper.random,
        api: MissionDefinitionAPI
    ): FleetMemberAPI {
        val member = if (name == null) {
            api.addToFleet(
                side, variantId,
                FleetMemberType.SHIP, isFlagship
            )
        } else {
            api.addToFleet(
                side, variantId,
                FleetMemberType.SHIP, name, isFlagship
            )
        }
        //        addDMods1(member, false, numDMods, random);
//        setDHull(member.getVariant());
        return member
    }

    fun addDMods1(member: FleetMemberAPI, canAddDestroyedMods: Boolean, num: Int, random: Random) {
        val variant = member.variant

//		if (member.getHullSpec().getHints().contains(ShipTypeHints.CIVILIAN)) {
//			int added = addAllPermaModsWithTags(variant, Tags.HULLMOD_CIV_ALWAYS);
//			if (added > 0) {
//				num -= added;
//				if (num <= 0) return;
//			}
//		}
        var potentialMods = getModsWithTags(Tags.HULLMOD_DAMAGE)
        if (canAddDestroyedMods) potentialMods.addAll(getModsWithTags(Tags.HULLMOD_DESTROYED_ALWAYS))
        removeUnsuitedMods(variant, potentialMods)
        val hasStructDamage = getNumDMods(variant, Tags.HULLMOD_DAMAGE_STRUCT) > 0
        if (hasStructDamage) {
            potentialMods = getModsWithoutTags(potentialMods, Tags.HULLMOD_DAMAGE_STRUCT)
        }
        if (variant.hullSpec.fighterBays > 0) {
            //if (variant.getHullSpec().getFighterBays() > 0 || variant.isCarrier()) {
            potentialMods.addAll(getModsWithTags(Tags.HULLMOD_FIGHTER_BAY_DAMAGE))
        }
        if (variant.hullSpec.defenseType == ShieldAPI.ShieldType.PHASE) {
            potentialMods.addAll(getModsWithTags(Tags.HULLMOD_DAMAGE_PHASE))
        }
        if (variant.isCarrier) {
            potentialMods.addAll(getModsWithTags(Tags.HULLMOD_CARRIER_ALWAYS))
        }
        potentialMods = ArrayList(HashSet(potentialMods))
        removeUnsuitedMods(variant, potentialMods)
        removeModsAlreadyInVariant(variant, potentialMods)

//		System.out.println("");
//		System.out.println("Adding: ");
        val picker = WeightedRandomPicker<HullModSpecAPI?>(random)
        picker.addAll(potentialMods)
        var added = 0
        var i = 0
        while (i < num && !picker.isEmpty) {
            val pick = picker.pickAndRemove()
            if (pick != null) {
                if (pick.hasTag(Tags.HULLMOD_DAMAGE_STRUCT) && getNumDMods(variant, Tags.HULLMOD_DAMAGE_STRUCT) > 0) {
                    i--
                    i++
                    continue
                }
                variant.addPermaMod(pick.id)
                //System.out.println("Mod: " + pick.getId());
                added++
                //                i--;
            }
            i++
        }
        //		if (getNumDMods(variant) < 5) {
//			System.out.println("ewfwefew");
//		}
    }
}