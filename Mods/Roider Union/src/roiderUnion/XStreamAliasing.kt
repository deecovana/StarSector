package roiderUnion

import com.thoughtworks.xstream.XStream
import roiderUnion.cleanup.MadMIDASHealer
import roiderUnion.cleanup.TrackerSingleBPAdderScript
import roiderUnion.econ.*
import roiderUnion.fleets.UnionHQPatrolManager
import roiderUnion.fleets.expeditionSpecials.PingTrapScript
import roiderUnion.nomads.NomadsData
import roiderUnion.nomads.bases.NomadBaseIntelPlugin
import roiderUnion.nomads.bases.NomadBaseLevelTracker
import roiderUnion.nomads.bases.NomadBaseSpawnScript
import roiderUnion.nomads.bases.RoiderSupplyScript
import roiderUnion.nomads.minefields.MajorLootStashPlugin
import roiderUnion.retrofits.argos.ArgosAbilityAdderScript
import roiderUnion.retrofits.argos.ArgosConversionAbility
import roiderUnion.retrofits.blueprints.RetrofitBlueprintPlugin
import roiderUnion.submarkets.NomadSubmarketPlugin
import roiderUnion.world.SystemMusicScript

object XStreamAliasing {
    fun alias(x: XStream) {
        Savebreak.alias(x)
        DivesController.alias(x)
        Shipworks.alias(x)
        UnionHQBlueprint.alias(x)
        ShipworksBlueprint.alias(x)
        UnionHQPatrolManager.alias(x)
        RetrofitBlueprintPlugin.alias(x)
        NomadSubmarketPlugin.alias(x)
        MajorLootStashPlugin.alias(x)
        PingTrapScript.alias(x)
        SystemMusicScript.alias(x)
        ArgosConversionAbility.alias(x)
        ArgosAbilityAdderScript.alias(x)
        TrackerSingleBPAdderScript.alias(x)
        MadMIDASHealer.alias(x)
        NomadsData.alias(x)
        NomadBaseSpawnScript.alias(x)
        RoiderSupplyScript.alias(x)
        NomadBaseLevelTracker.alias(x)
        NomadBaseIntelPlugin.alias(x)
        RemoteRezSource.alias(x)
    }
}