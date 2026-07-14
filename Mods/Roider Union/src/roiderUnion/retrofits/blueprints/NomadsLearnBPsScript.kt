package roiderUnion.retrofits.blueprints

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SpecialItemPlugin
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderIds
import roiderUnion.nomads.NomadsData

class NomadsLearnBPsScript : SubmarketUpdateListener {
    override fun reportSubmarketCargoAndShipsUpdated(submarket: SubmarketAPI?) {
        val base = submarket?.market?.primaryEntity ?: return
        if (submarket.specId != RoiderIds.Roider_Submarkets.NOMAD_MARKET) return
        val cargo = submarket.cargoNullOk ?: return
        for (stack in cargo.stacksCopy) {
            val plugin: SpecialItemPlugin = stack.plugin ?: continue
            if (plugin is RetrofitBlueprintPlugin) {
                if (knowsRetrofit(base, plugin.providedShip)) {
                    learnRetrofit(base, plugin.providedShip!!)
                    cargo.removeItems(stack.type, stack.data, 1f)
                }
            }
        }
    }

    private fun knowsRetrofit(base: SectorEntityToken, retrofit: String?): Boolean {
        if (retrofit == null) return false
        val nomads = Memory.getNullable(
            MemoryKeys.NOMAD_GROUP,
            base,
            { it is NomadsData },
            { null }
        ) as? NomadsData ?: return false
        return nomads.knownBPs.contains(retrofit)
    }

    private fun learnRetrofit(base: SectorEntityToken, retrofit: String) {
        val nomads = Memory.getNullable(
            MemoryKeys.NOMAD_GROUP,
            base,
            { it is NomadsData },
            { null }
        ) as? NomadsData ?: return
        nomads.knownBPs += retrofit
    }
}