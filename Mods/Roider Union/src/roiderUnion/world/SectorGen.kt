package roiderUnion.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.FullName.Gender
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import exerelin.campaign.ExerelinSetupData
import roiderUnion.helpers.Helper
import roiderUnion.ids.*
import roiderUnion.ModPlugin
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Memory
import roiderUnion.helpers.Settings
import roiderUnion.ids.RoiderIds.Roider_Conditions.ORE_CONDITIONS
import roiderUnion.ids.RoiderIds.Roider_Conditions.RARE_ORE_CONDITIONS
import roiderUnion.ids.RoiderIds.Roider_Conditions.ORGANICS_CONDITIONS
import roiderUnion.ids.RoiderIds.Roider_Conditions.VOLATILES_CONDITIONS
import roiderUnion.ids.systems.AtkaIds
import roiderUnion.ids.systems.OunalashkaIds
import roiderUnion.nomads.bases.NomadBaseLevel
import roiderUnion.world.systems.Atka
import roiderUnion.world.systems.Attu
import roiderUnion.world.systems.Kiska
import roiderUnion.world.systems.Ounalashka
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap


object SectorGen {
    fun generate() {
        generateStarSystems()
    }

    private const val ESTABLISHED_BASES_MULT = 0.8f

    fun preplaceNomads() {
        var baseCount = 0
        if (Settings.MAX_NOMAD_BASES > 0) {
            baseCount++
            SectorGenHelper.createNomadBase(NomadBaseLevel.SHIPWORKS, Factions.INDEPENDENT)
        }
        if (Settings.MAX_NOMAD_BASES > 1) {
            baseCount++
            SectorGenHelper.createNomadBase(NomadBaseLevel.HQ, RoiderFactions.ROIDER_UNION)
        }
        if (Settings.MAX_NOMAD_BASES > 2) {
            baseCount++
            SectorGenHelper.createNomadBase(NomadBaseLevel.BATTLESTATION, Factions.INDEPENDENT)
        }

        while (baseCount < (Settings.MAX_NOMAD_BASES * ESTABLISHED_BASES_MULT).toInt()) {
            baseCount++
            SectorGenHelper.createNomadBase(NomadBaseLevel.ESTABLISHED, RoiderFactions.ROIDER_UNION)
        }
    }

    fun generateVoidResources() {
        Helper.sector?.starSystems?.filterNot { SectorGenHelper.hasVoidResources(it) }?.forEach {
            SectorGenHelper.genVoidResource(it, Commodities.ORE, ORE_CONDITIONS, SectorGenHelper::hasOre)
            SectorGenHelper.genVoidResource(it, Commodities.RARE_ORE, RARE_ORE_CONDITIONS, SectorGenHelper::hasRareOre)
            SectorGenHelper.genVoidResource(it, Commodities.ORGANICS, ORGANICS_CONDITIONS, SectorGenHelper::hasOrganics)
            SectorGenHelper.genVoidResource(it, Commodities.VOLATILES, VOLATILES_CONDITIONS, SectorGenHelper::hasVolatiles)
        }
    }

    private fun generateStarSystems() {
        Atka.generate()
        Attu.generate()
        Ounalashka.generate()
        Kiska.generate()
    }

    fun initFactionRelationships() {
        if (ModPlugin.hasNexerelin && ExerelinSetupData.getInstance().startRelationsMode == ExerelinSetupData.StartRelationsMode.RANDOM) return
        val roider = Helper.sector?.getFaction(RoiderFactions.ROIDER_UNION) ?: return

        roider.setRelationship(Factions.INDEPENDENT, RepLevel.FRIENDLY)
        roider.setRelationship(Factions.TRITACHYON, RepLevel.HOSTILE)
        roider.setRelationship(Factions.PIRATES, RepLevel.VENGEFUL)
        roider.setRelationship(Factions.LUDDIC_PATH, RepLevel.HOSTILE)
        roider.setRelationship(Factions.REMNANTS, RepLevel.HOSTILE)
        roider.setRelationship(Factions.DERELICT, RepLevel.HOSTILE)
        roider.setRelationship(Factions.HEGEMONY, RepLevel.FAVORABLE)
        roider.setRelationship(Factions.REMNANTS, RepLevel.HOSTILE)
        roider.setRelationship(Factions.OMEGA, RepLevel.HOSTILE)

        // Mod factions
        roider.setRelationship(RoiderFactions.THI, RepLevel.FAVORABLE)
        roider.setRelationship(RoiderFactions.IRON_SHELL, RepLevel.SUSPICIOUS)

        // Various pirate mod factions and the like
        roider.setRelationship(RoiderFactions.CABAL, -1f)
        roider.setRelationship(RoiderFactions.ARS, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.COLONIAL_PIRATES, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.SCY, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.BLADE_BREAKERS, RepLevel.VENGEFUL)
        roider.setRelationship(RoiderFactions.EXIPIRATED, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.CARTEL, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.CRYSTANITE_PIRATES, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.GMDA, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.GMDA_PATROL, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.GREY_GOO, RepLevel.VENGEFUL)
        roider.setRelationship(RoiderFactions.JUNK_HOUNDS, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.JUNK_PIRATES, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.JUNK_JUNKBOYS, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.JUNK_TECHNICIANS, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.TEMPLARS, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.NULL_ORDER, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.VAMPIRES, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.WEREWOLVES, RepLevel.HOSTILE)
        roider.setRelationship(RoiderFactions.ZOMBIES, RepLevel.HOSTILE)
    }

    fun addCoreDives(sector: SectorAPI) {
        SectorGenHelper.addDive(sector, "Samarra", "orthrus", RoiderIndustries.DIVES) // extern
        SectorGenHelper.addDive(sector, "Samarra", "tigra_city", RoiderIndustries.DIVES)
        SectorGenHelper.addDive(sector, "Westernesse", "ailmar", RoiderIndustries.DIVES)
        SectorGenHelper.addDive(sector, "Magec", "new_maxios", RoiderIndustries.DIVES)
        SectorGenHelper.addDive(sector, "Magec", "kantas_den", RoiderIndustries.DIVES)
        SectorGenHelper.addDive(sector, "Eos Exodus", "baetis", RoiderIndustries.DIVES)
        SectorGenHelper.markClaimed(sector, "Eos Exodus", "daedaleon") // Block mining on Daedaleon
        SectorGenHelper.addDive(sector, "Galatia", "derinkuyu_station", RoiderIndustries.DIVES)
        SectorGenHelper.addDive(sector, "Corvus", "corvus_pirate_station", RoiderIndustries.DIVES)

//        SectorGenHelper.markSystemClaimed(sector, "Shaanxi"); // Tiandong Heavy Industries system
    }

    fun pickSystemForRoiderBase(): StarSystemAPI? {
        val random = Random()
        val far = WeightedRandomPicker<StarSystemAPI>(random)
        val picker = WeightedRandomPicker<StarSystemAPI>(random)
        for (system in Global.getSector().starSystems) {
            val days: Float = Global.getSector().clock.getElapsedDaysSince(system.lastPlayerVisitTimestamp)
            if (days < 45f) continue
            if (system.center.memoryWithoutUpdate.contains(PirateBaseManager.RECENTLY_USED_FOR_BASE)) continue
            var weight = 0f
            if (system.hasTag(Tags.THEME_MISC_SKIP)) {
                weight = 1f
            } else if (system.hasTag(Tags.THEME_MISC)) {
                weight = 3f
            } else if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
                weight = 3f
            } else if (system.hasTag(Tags.THEME_RUINS)) {
                weight = 5f
            }
            if (weight <= 0f) continue
            val usefulStuff = (system.getCustomEntitiesWithTag(Tags.OBJECTIVE).size +
                    system.getCustomEntitiesWithTag(Tags.STABLE_LOCATION).size).toFloat()
            if (usefulStuff <= 0) continue
            if (Misc.hasPulsar(system)) continue
            if (Misc.getMarketsInLocation(system).size > 0) continue
            val weights: LinkedHashMap<LocationType, Float> = LinkedHashMap()
            weights[LocationType.IN_ASTEROID_BELT] = 1f
            weights[LocationType.IN_ASTEROID_FIELD] = 1f
            weights[LocationType.IN_RING] = 1f
            weights[LocationType.PLANET_ORBIT] = 1f
            val locs = BaseThemeGenerator.getLocations(random, system, HashSet(), 200f, weights)
            if (locs.isEmpty) continue
            if (system.location.length() > 36000f) {
                far.add(system, weight * usefulStuff)
            } else {
                picker.add(system, weight * usefulStuff)
            }
        }
        if (picker.isEmpty) {
            picker.addAll(far)
        }
        return picker.pick()
    }

    fun assignCustomAdmins(sector: SectorAPI) {
        sector.getStarSystem(OunalashkaIds.SYSTEM_NAME)?.getEntityById(OunalashkaIds.MAGGIES.id)?.market?.apply {
            val person = this.faction.createRandomPerson()
            person.setFaction(Factions.PIRATES)
            person.gender = Gender.FEMALE
            person.rankId = Ranks.SPACE_ADMIRAL
            person.postId = Ranks.POST_FLEET_COMMANDER
            person.name.first = ExternalStrings.MAGGIE_FIRST
            person.name.last = ExternalStrings.MAGGIE_LAST
            person.portraitSprite = Global.getSettings().getSpriteName(Categories.CHARACTERS, Portraits.MAGGIE)
            this.admin = person
            this.commDirectory.addPerson(person, 0)
            this.addPerson(person)

        }
        sector.getStarSystem(AtkaIds.SYSTEM_NAME)?.getEntityById(AtkaIds.KOROVIN.id)?.market?.apply {
            this.removePerson(this.admin)
            val person = this.faction.createRandomPerson()
            person.gender = Gender.MALE
            person.rankId = Ranks.FACTION_LEADER
            person.postId = Ranks.POST_FACTION_LEADER
            person.name.first = ExternalStrings.LEWIS_FIRST
            person.name.last = ExternalStrings.LEWIS_LAST
            person.stats.setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3f)
            this.admin = person
            this.commDirectory.addPerson(person, 0)
            this.addPerson(person)
        }
    }

    fun assignRandomAdmins(sector: SectorAPI) {
        sector.economy?.marketsCopy?.filter { it.primaryEntity?.id?.startsWith(RoiderIds.PREFIX) == true }
            ?.filter { it.size <= 4 }
            ?.filterNot { Memory.isFlag(MemFlags.MARKET_DO_NOT_INIT_COMM_LISTINGS) }
            ?.forEach {
                val admin = assignStationCommander(it) ?: findAdmin(it) ?: createAdmin(it)
                if (admin != null) addSkillsAndAssignAdmin(it, admin)
            }
    }

    private fun assignStationCommander(market: MarketAPI): PersonAPI? {
        return market.peopleCopy.firstOrNull { it.post == Ranks.POST_STATION_COMMANDER && it.faction == market.faction }
    }

    private fun findAdmin(market: MarketAPI): PersonAPI? {
        return market.peopleCopy.firstOrNull { it.post == Ranks.POST_ADMINISTRATOR && it.faction == market.faction }
    }

    private fun createAdmin(market: MarketAPI): PersonAPI? {
        val result = market.faction.createRandomPerson() ?: return null
        result.rankId = Ranks.CITIZEN
        result.postId = Ranks.POST_ADMINISTRATOR
        market.commDirectory.addPerson(result)
        market.addPerson(result)
        val ip = Helper.sector?.importantPeople ?: return result
        ip.addPerson(result)
        ip.getData(result).location.market = market
        ip.checkOutPerson(result, RoiderIds.PERMANENT_STAFF)
        return result
    }

    private fun addSkillsAndAssignAdmin(market: MarketAPI, admin: PersonAPI) {
        if (Helper.settings?.sortedSkillIds?.contains(Skills.INDUSTRIAL_PLANNING) == false) return
        admin.stats.isSkipRefresh = true
        admin.stats.setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3f)
        admin.stats.isSkipRefresh = false
        admin.stats.refreshCharacterStatsEffects()
        market.admin = admin
    }

    fun applyMarketBuffs(sector: SectorAPI) {
        val korovin = sector.getStarSystem(AtkaIds.SYSTEM_NAME)?.getEntityById(AtkaIds.KOROVIN.id)
        korovin?.market?.getIndustry(RoiderIndustries.UNION_HQ)?.isImproved = true
    }
}