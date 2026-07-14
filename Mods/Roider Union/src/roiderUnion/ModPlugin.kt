package roiderUnion

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.thoughtworks.xstream.XStream
import exerelin.campaign.SectorManager
import org.dark.shaders.light.LightData
import org.dark.shaders.util.ShaderLib
import org.dark.shaders.util.TextureData
import retroLib.RetrofitsKeeper
import retroLib.api.FittersToTagsConvertor
import roiderUnion.cleanup.FringeStationCleaner
import roiderUnion.cleanup.MadMIDASHealer
import roiderUnion.cleanup.TrackerSingleBPAdderScript
import roiderUnion.econ.DivesBPOpenMarketAdder
import roiderUnion.econ.DivesSupplyManager
import roiderUnion.fleets.mining.DisposableRoiderScoutManager
import roiderUnion.fleets.mining.RoiderMiningRouteManager
import roiderUnion.fleets.nomads.NomadTradeRouteManager
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.MarketHelper
import roiderUnion.hullmods.PhasenetTowScriptAdder
import roiderUnion.ids.*
import roiderUnion.nomads.bases.NomadBaseSpawnScript
import roiderUnion.nomads.bases.NomadBaseSupplier
import roiderUnion.retrofits.argos.ArgosAbilityAdderScript
import roiderUnion.retrofits.blueprints.NomadsLearnBPsScript
import roiderUnion.retrofits.blueprints.PiratesLearnBPsScript
import roiderUnion.world.*


/**
 * Want to rework fringe bases. Main goals:
 * - bases should be less reliable
 * - highlight _nomadic_ part of roiders
 * - also highlight roaming part
 *
 * Plans:
 * - make bases abandon after a while
 *   - fleets spawn and travel to new base or to an existing market
 *     - send small fleets first? idk
 *   - that a base is being abandoned is not automatically known
 *   - nomad trade fleets are redirected to the migration target
 *   - new base spawns as soon as abandon begins
 *   - new base activates as soon as old base deactivates
 * - add specific nomad groups, led by a storm admiral
 * - add "X's Storm Armada" fleet defending each base
 *   - possibly bounties on them if they are the raiding type
 *   - despawn in hyper to simulate their "stealth and speed"?
 * - add "free roiders"
 *   - replace pirate roiders
 *   - need to tag free roider fleets for flipping script?
 *   - can be indie, pirate, or flip between the two
 *   - free roider nomad groups can launch pirate-style raids from their bases
 *   - free roider bases are either indie or pirate
 *     - if a free base is abandoned for a new base, the new base does not have to be the same faction
 *
 *
 * Outposts should not be fleets?
 * - Instead be a dialog encounter battle
 * - Claim -> Site -> Outpost -> Outpost (Garrison) -> Outpost (Heavy Garrison)
 * - claims and sites are automated
 * - maybe garrison should start with sites?
 * - or all levels should have ships guarding?
 * - should outposts have great stealth?
 *
 *
 *
 * Need to fix frames being usable without BP??? Not sure if they are.
 * Make Glitz flares unable to hit the parent ship
 */


class ModPlugin: BaseModPlugin() {
    companion object {
        @Transient
        var hasNexerelin = false

        @Transient
        var hasStarshipLegends = false

        const val LIGHT_DATA_CSV = "data/lights/roider_light_data.csv"
        const val TEXTURE_DATA_CSV = "data/lights/roider_texture_data.csv"

        const val SAVE_BREAK_VERSION = "2_1_0"
    }

    override fun onApplicationLoad() {
        if (!Helper.isModEnabled(ModIds.LAZY_LIB)) throw RuntimeException(ExternalStrings.LAZY_LIB_REQ)
        if (!Helper.isModEnabled(ModIds.MAGIC_LIB)) throw RuntimeException(ExternalStrings.MAGIC_LIB_REQ)
        if (!Helper.isModEnabled(ModIds.RETRO_LIB)) throw RuntimeException(ExternalStrings.RETRO_LIB_REQ)
        if (Helper.isModEnabled(ModIds.GRAPHICS_LIB)) {
            ShaderLib.init()
            LightData.readLightDataCSV(LIGHT_DATA_CSV)
            TextureData.readTextureDataCSV(TEXTURE_DATA_CSV)
        }
        RetrofitsKeeper.CONVERTORS += object : FittersToTagsConvertor {
            override fun convert(vararg fitters: String): Set<String> {
                if (!fitters.contains(RoiderFactions.ROIDER_UNION)) return setOf()
                return fitters.toSet()
            }
        }
        retroLib.Helper.HEAVY_INDUSTRIES.add(RoiderIndustries.SHIPWORKS)
        hasNexerelin = Helper.isModEnabled(ModIds.NEXERELIN)
        hasStarshipLegends = Helper.isModEnabled(ModIds.STARSHIP_LEGENDS)

        MarketHelper.addItemEffectsToRepo()
    }

    override fun onCodexDataGenerated() {
        RoiderCodex.initializeCodexEntries()
    }


    override fun onNewGame() {
        SectorGen.initFactionRelationships()
        SharedData.getData().personBountyEventData.addParticipatingFaction(RoiderFactions.ROIDER_UNION)
        if (!hasNexerelin || SectorManager.getManager().isCorvusMode) {
            SectorGen.generate()
        }
    }

    override fun onNewGameAfterEconomyLoad() {
        val sector: SectorAPI = Global.getSector()
        if (!hasNexerelin || SectorManager.getManager().isCorvusMode) {
            SectorGen.applyMarketBuffs(sector)
            SectorGen.addCoreDives(sector)
            SectorGen.assignCustomAdmins(sector)
            SectorGen.assignRandomAdmins(sector)
        }
        if (NexGen.isRoiderUnionEnabled) {
            SectorGen.preplaceNomads()
            if (!sector.hasScript(RoiderUnionReformer::class.java)) {
                sector.addScript(RoiderUnionReformer())
            }
        }
    }

    override fun onNewGameAfterTimePass() {
        if (hasNexerelin && !SectorManager.getManager().isCorvusMode
            && NexGen.isRoiderUnionEnabled
        ) {
            NexGen.addNexRandomModeDives()
            NexGen.addNexRandomRockpiper()
        }
    }

    override fun onGameLoad(newGame: Boolean) {
        val sector = Global.getSector()!!

        if (NexGen.isRoiderUnionEnabled) {
            if (!sector.hasScript(RoiderUnionReformer::class.java)) {
                sector.addScript(RoiderUnionReformer())
            }
        }

        addKnownIndustries(sector)

        SectorGen.generateVoidResources()
//        RoidMiningHelper.tagAsteroids(sector) // Needs to be called more often

        sector.registerPlugin(RoiderCampaignPlugin())

        if (!newGame) {
            FringeStationCleaner.removeOrphanedFringeStations(sector)
        }
        if (!sector.hasScript(Savebreak::class.java)) {
            sector.addScript(Savebreak())
        }
        if (!sector.hasScript(CodexLinkRefresher::class.java)) {
            sector.addScript(CodexLinkRefresher())
        }
        if (!sector.characterData.abilities.contains(RoiderIds.Abilities.ARGOS_RETROFITS)) {
            sector.addTransientScript(ArgosAbilityAdderScript())
        }
        val pirates = sector.getFaction(Factions.PIRATES)
        if (sector.listenerManager?.hasListenerOfClass(PiratesLearnBPsScript::class.java) == false && pirates != null) {
            sector.listenerManager?.addListener(PiratesLearnBPsScript())
        }

        if (sector.economy?.updateListeners?.none { it is DivesBPOpenMarketAdder } == true) {
            sector.economy?.addUpdateListener(DivesBPOpenMarketAdder())
        }

        if (sector.economy?.updateListeners?.none { it is NomadBaseSupplier } == true) {
            sector.economy?.addUpdateListener(NomadBaseSupplier())
        }

        if (!sector.hasScript(DivesSupplyManager::class.java)) {
            sector.addScript(DivesSupplyManager())
        }

        if (sector.listenerManager?.hasListenerOfClass(NomadsLearnBPsScript::class.java) == false) {
            sector.listenerManager?.addListener(NomadsLearnBPsScript())
        }

        if (!sector.hasScript(NomadBaseSpawnScript::class.java)) {
            sector.addScript(NomadBaseSpawnScript())
        }
        if (!sector.hasScript(NomadTradeRouteManager::class.java)) {
            sector.addScript(NomadTradeRouteManager())
        }
        if (!sector.hasScript(DisposableRoiderScoutManager::class.java)) {
            sector.addScript(DisposableRoiderScoutManager())
        }
        if (!sector.hasScript(RoiderMiningRouteManager::class.java)) {
            sector.addScript(RoiderMiningRouteManager())
        }
        sector.addTransientScript(TrackerSingleBPAdderScript())
        sector.addTransientScript(MadMIDASHealer())
        sector.addTransientScript(SystemMusicScript())
        sector.addTransientScript(PhasenetTowScriptAdder())
//        sector.addTransientScript(PlayerProspecting())
//        sector.addTransientScript(AsteroidBouncer())
    }

    private fun addKnownIndustries(sector: SectorAPI) {
        val roiders = Helper.roiders
        if (roiders != null) {
            if (!roiders.knowsIndustry(RoiderIndustries.DIVES)) roiders.addKnownIndustry(RoiderIndustries.DIVES)
            if (!roiders.knowsIndustry(RoiderIndustries.UNION_HQ)) roiders.addKnownIndustry(RoiderIndustries.UNION_HQ)
            if (!roiders.knowsIndustry(RoiderIndustries.SHIPWORKS)) roiders.addKnownIndustry(RoiderIndustries.SHIPWORKS)
        }
        val indie = sector.getFaction(Factions.INDEPENDENT)
        if (indie != null) {
            if (!indie.knowsIndustry(RoiderIndustries.DIVES)) indie.addKnownIndustry(RoiderIndustries.DIVES)
        }
    }

    override fun configureXStream(x: XStream) {
        XStreamAliasing.alias(x)
    }
}