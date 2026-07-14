package scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.AddedEntity
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ids.MemoryKeys
import scripts.campaign.cleanup.Roider_ExpeditionMajorLootCleaner
import scripts.campaign.fleets.expeditions.NomadStashPickupScript
import roiderUnion.nomads.minefields.MajorLootStashPlugin
import roiderUnion.fleets.expeditionSpecials.PingTrapSpecial
import java.util.*

/**
 * Author: SafariJohn
 */
class Roider_ExpeditionLoot : BaseCommandPlugin() {
    //	private InteractionDialogAPI dialog;
    protected var text: TextPanelAPI? = null
    private var entity: SectorEntityToken? = null

    //	private Map<String, MemoryAPI> memoryMap;
    private var random: Random? = null

    //    private boolean major;
    override fun execute(
        ruleId: String, dialog: InteractionDialogAPI,
        params: List<Misc.Token>, memoryMap: Map<String, MemoryAPI>
    ): Boolean {
//		this.dialog = dialog;
        text = dialog.textPanel
        //		this.memoryMap = memoryMap;
        val command: String = params[0].getString(memoryMap) ?: return false
        entity = dialog.interactionTarget

//		Object test = entity!!.getMemoryWithoutUpdate().get(MemFlags.SALVAGE_DEBRIS_FIELD);
//		if (test instanceof DebrisFieldTerrainPlugin) {
//			debris = (DebrisFieldTerrainPlugin) test;
//		} else {
//            return false;
//        }
        random = Random(entity!!.memoryWithoutUpdate.getLong(MemFlags.SALVAGE_SEED))
        when (command) {
            "printDesc" -> printDesc()
            "minefieldDisable" -> minefieldConsequence() // extern
            "scanDebris" -> spawnLoot()
        }
        return true
    }

    private fun printDesc() {
//		float daysLeft = debris.getDaysLeft();
//		if (daysLeft >= 1000) {
//			text!!.addParagraph("The field appears stable and will not drift apart any time soon.");
//		} else {
//			String atLeastTime = Misc.getAtLeastStringForDays((int) daysLeft);
//			text!!.addParagraph("The field is unstable, but should not drift apart for " + atLeastTime + ".");
//		}

//        if (major) {
        text!!.addPara(
            "The mines are blazoned with Roider Union "
                    + "emblems and are densest around a volume "
                    + "approximately the size of a capital ship." // extern
        )
        text!!.addPara(
            "There must be something very valuable "
                    + "hidden here, but anyone who bothers to "
                    + "lay a minefield means business. Shooting "
                    + "your way through the mines would surely " // extern
                    + "cause the cache to be destroyed as well."
        )
        //        } else {
//            text!!.addPara("Long-range sensors indicate there is "
//                        + "nothing of value here. Then your sensors "
//                        + "officer reports a suspicious variation "
//                        + "on the phase sensors - perhaps a stealth "
//                        + "satellite being used as a nav beacon?");
//
//            text!!.addPara("Many roiders have recently begun using "
//                        + "such devices in their salvage operations. "
//                        + "A close scan should reveal anything of "
//                        + "interest.");
//        }
    }

    private fun minefieldConsequence() {
        // Do nothing for now
    }

    private fun spawnLoot() {
        val system: StarSystemAPI = entity!!.starSystem
        val loot = createLootDrop(system) ?: return
        val seed: Float = entity!!.memoryWithoutUpdate.getFloat(MemFlags.SALVAGE_SEED)
        loot.entity.memoryWithoutUpdate.set(MemFlags.SALVAGE_SEED, seed)
        loot.entity!!.memoryWithoutUpdate.set(MemoryKeys.EXPEDITION_LOOT_MAJOR, true)
        loot.entity!!.memoryWithoutUpdate.set(MemoryKeys.EXPEDITION_LOOT, true)
        val thiefId: String = entity!!.memoryWithoutUpdate.getString(MemoryKeys.THIEF_KEY)
        loot.entity!!.memoryWithoutUpdate.set(MemoryKeys.THIEF_KEY, thiefId)
        loot.entity!!.isDiscoverable = true
        //        loot.entity!!.setDiscoveryXP(0f);
        loot.entity!!.orbit = entity!!.orbit.makeCopy()
        system.addEntity(loot.entity)

//        String faction = entity!!.getMemoryWithoutUpdate().getString(Roider_MemFlags.EXPEDITION_FACTION);
//        String source = entity!!.getMemoryWithoutUpdate().getString(Roider_MemFlags.EXPEDITION_MARKET);

//        Roider_ExpeditionTrapCreator creator;
////        if (major)
//            creator = new Roider_ExpeditionTrapCreator(random,
//                    1f, Roider_FleetTypes.MAJOR_EXPEDITION,
//                    faction, source, 10, 20, true);
////        else creator = new Roider_ExpeditionTrapCreator(random,
////                    0.9f, Roider_FleetTypes.MINING_FLEET,
////                    faction, source, 7, 14, false);
//
//		SpecialCreationContext context = new SpecialCreationContext();
//
//		Object specialData = creator.createSpecial(loot.entity, context);
//		if (specialData != null) {
//			Misc.setSalvageSpecial(loot.entity, specialData);
//        }
        val picker = WeightedRandomPicker<String>(random)
//        picker.add("thiefTrap", 10f)
        picker.add("pingTrap", 10f) // extern
        //            picker.add("droneDefenders", 1);
        val special: String = picker.pick()
        when (special) {
//            "thiefTrap" -> Misc.setSalvageSpecial(loot.entity, Roider_ThiefTrapSpecial.Roider_ThiefTrapSpecialData())
            "pingTrap" -> Misc.setSalvageSpecial(loot.entity, PingTrapSpecial.PingTrapSpecialData())
//            else -> Misc.setSalvageSpecial(loot.entity, Roider_ThiefTrapSpecial.Roider_ThiefTrapSpecialData())
        }

//        debris.gete.setContainingLocation(null);
//        system.removeEntity(entity);

//        debris.setScavenged(true);
//        debris.getEntity().setOrbit(loot.entity!!.getOrbit());
        if (loot.entityType == Entities.WRECK) {
            text!!.addPara(
                "A " + loot.entity!!.name.lowercase(Locale.getDefault()) + " dephases before you.", // extern
                Misc.getHighlightColor(),
                loot.entity!!.name.lowercase(Locale.getDefault())
            )
        } else {
            text!!.addPara(
                "An abandoned " + loot.entity!!.name.lowercase(Locale.getDefault())
                        + " dephases before you, much to your amazement!",
                Misc.getHighlightColor(),
                loot.entity!!.name.lowercase(Locale.getDefault())
            )
        }
        Global.getSoundPlayer().playSound(
            "system_phase_cloak_deactivate", 1f, 1f, // extern
            Global.getSector().playerFleet.location, Vector2f(0f, 0f)
        )
        val temp = entity
        entity = loot.entity
        (temp!!.customPlugin as MajorLootStashPlugin).fadeOut()
        Misc.fadeAndExpire(temp)

        // Save entity in token's data for later access
        // Have to avoid a ConcurrentModificationException
        entity!!.orbitFocus.customData
            .put(NomadStashPickupScript.STASH_ENTITY_KEY, entity)

        // Need to clean up orbit focus when the loot entity expires
        entity!!.addScript(Roider_ExpeditionMajorLootCleaner(entity!!))
    }

    private fun createLootDrop(system: StarSystemAPI): AddedEntity? {
        // Copy location
        val loc = EntityLocation()
        loc.location = entity!!.location
        loc.orbit = entity!!.orbit.makeCopy()
        loc.type = LocationType.IN_SMALL_NEBULA
        val picker = WeightedRandomPicker<String>(random)
        picker.add(Entities.STATION_RESEARCH, 0.1f)
        picker.add(Entities.STATION_MINING, 1f)
        picker.add(Entities.ORBITAL_HABITAT, 5f)
        picker.add(Entities.WRECK, 50f)
        val type: String = picker.pick()
        if (type == Entities.WRECK) {
            val factions: List<String> = listOf(Factions.INDEPENDENT)
            picker.clear()
            picker.addAll(factions)
            var iter = 0
            do {
                if (iter > 110) break
                val faction: String = picker.pick()
                val params = DerelictShipEntityPlugin.createRandom(
                    faction,
                    null,
                    random,
                    DerelictShipEntityPlugin.getDefaultSModProb()
                )
                iter++
                if (params != null) {
                    if (params.ship.getVariant().hullSize != HullSize.CAPITAL_SHIP && iter < 100) {
                        continue
                    }
                    val salEntity = BaseThemeGenerator.addSalvageEntity(
                        random,
                        system,
                        Entities.WRECK,
                        Factions.NEUTRAL,
                        params
                    )
                    salEntity.isDiscoverable = true
                    BaseThemeGenerator.setEntityLocation(
                        salEntity,
                        loc,
                        Entities.WRECK
                    )
                    return AddedEntity(salEntity, null, Entities.WRECK)
                }
            } while (true)
        }
        return addStation(loc, system, type, Factions.NEUTRAL)
    }

    fun addStation(
        loc: EntityLocation?,
        system: StarSystemAPI?, customEntityId: String?,
        factionId: String?
    ): AddedEntity? {
        if (loc == null) return null
        val station: AddedEntity = BaseThemeGenerator.addEntity(
            random,
            system, loc, customEntityId, factionId
        )
        val focus: SectorEntityToken = station.entity!!.orbitFocus
        if (focus is PlanetAPI) {
            val planet: PlanetAPI = focus
            val nearStar =
                planet.isStar && station.entity!!.orbit != null && station.entity!!.circularOrbitRadius < 5000
            if (planet.isStar && !nearStar) {
//				station.entity!!.setFacing(random.nextFloat() * 360f);
//				convertOrbitNoSpin(station.entity);
            } else {
                BaseThemeGenerator.convertOrbitPointingDown(station.entity)
            }
        }

//		station.entity!!.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_DEFENDER_FACTION, Factions.REMNANTS);
//		station.entity!!.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_DEFENDER_PROB, 1f);
        return station
    }
}