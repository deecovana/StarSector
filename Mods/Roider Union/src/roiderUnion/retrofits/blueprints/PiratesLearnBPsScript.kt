package roiderUnion.retrofits.blueprints

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SpecialItemPlugin
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener
import com.fs.starfarer.api.impl.campaign.DelayedBlueprintLearnScript
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import retroLib.RetrofitsKeeper
import retroLib.impl.BaseRetrofitAdjuster
import roiderUnion.helpers.Helper
import roiderUnion.retrofits.RoiderAllFilter

class PiratesLearnBPsScript : SubmarketUpdateListener {
    @Transient
    private lateinit var sources: Map<String, List<String>>

    override fun reportSubmarketCargoAndShipsUpdated(submarket: SubmarketAPI?) {
        val base = submarket?.market?.primaryEntity ?: return
        if (submarket.specId != Submarkets.SUBMARKET_BLACK) return
        val cargo = submarket.cargoNullOk ?: return
        delayedLearnBlueprintsFromTransaction(base.faction, cargo, 60f + 60f * Helper.random.nextFloat())
    }

    private fun delayedLearnBlueprintsFromTransaction(faction: FactionAPI, cargo: CargoAPI, daysDelay: Float) {
        val script = DelayedBlueprintLearnScript(faction.id, daysDelay)
        for (stack in cargo.stacksCopy) {
            val plugin: SpecialItemPlugin = stack.plugin ?: continue
            if (plugin is RetrofitBlueprintPlugin) {
                val id: String = plugin.providedShip ?: continue
                if (faction.knowsShip(id)) continue
                if (!sourceKnown(id, faction)) continue
                script.ships.add(id)
                cargo.removeItems(stack.type, stack.data, 1f)
            }
        }
        if (script.fighters.isNotEmpty() || script.ships.isNotEmpty()) {
            Helper.sector?.addScript(script)
            cargo.sort()
        }
    }

    private fun sourceKnown(hullId: String, faction: FactionAPI): Boolean {
        if (!this::sources.isInitialized) {
            sources = getSources()
        }
        return sources[hullId]?.any { faction.knowsShip(it) } == true
    }

    private fun getSources(): Map<String, List<String>> {
        val results= mutableMapOf<String, List<String>>()
        val targets = mutableSetOf<String>()
        val retrofits = RetrofitsKeeper.getRetrofits(RoiderAllFilter(BaseRetrofitAdjuster(null)))
        retrofits.forEach { targets.add(it.target) }
        targets.forEach { target -> results[target] = retrofits.filter { it.target == target }.map { it.source } }
        return results
    }
}