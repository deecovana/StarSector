package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.CommMessageAPI
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.Helper
import java.util.*

class RoidCutting(private val roid: SectorEntityToken, private var prospector: CampaignFleetAPI?) : EveryFrameScript {

    override fun isDone(): Boolean = !roid.isAlive
    override fun runWhilePaused(): Boolean = false

    private val seed = Helper.random.nextLong()

    override fun advance(amount: Float) {
        if (Helper.sector?.isPaused == true) return
        checkExclusivity()
        checkCollection()
    }

    private fun checkExclusivity() {
        if (prospector != null && ((AsteroidBouncer.FLEETS[prospector] ?: 0f) <= 0f)) {
            val type = RoidMiningHelper.getRoidType(roid) ?: RoidType.DUST
            roid.setFaction(type.faction)
            prospector = null
        }
    }

    private fun checkCollection() {
        if (prospector != null) {
            if (canCollect(prospector!!)) collectRoid(prospector!!)
        } else {
            roid.containingLocation?.fleets
                ?.firstOrNull { RoidMiningHelper.canMine(it, roid) && canCollect(it) }
                ?.apply { collectRoid(this) }
        }
    }

    private fun canCollect(fleet: CampaignFleetAPI): Boolean {
        if (roid.containingLocation != fleet.containingLocation) return false
        return Misc.getDistance(roid, fleet) <= roid.radius + fleet.radius
    }

    private fun collectRoid(prospector: CampaignFleetAPI) {
        Misc.fadeAndExpire(roid, 0.1f)
        val type = RoidMiningHelper.getRoidType(roid) ?: return
        val roidValue = RoidMiningHelper.getRoidValue(roid) ?: return
        val commodityWeights = mutableMapOf<String, Float>()
        for (weightMap in type.commodityWeights) {
            commodityWeights += if (weightMap.size == 1) {
                if (weightMap.keys.first() == RoidType.NULL_COM_PICK) continue
                Pair(weightMap.keys.first(), weightMap.values.first())
            } else {
                val picker = WeightedRandomPicker<String>(Random(seed))
                weightMap.forEach { picker.add(it.key, it.value) }
                val com = picker.pick()
                if (com == RoidType.NULL_COM_PICK) continue
                Pair(com, weightMap[com] ?: 1f)
            }
        }
        val commodities = mutableMapOf<String, Float>()
        val totalWeight = commodityWeights.values.sum()
        commodityWeights.forEach {
            val basePrice = Helper.settings?.getCommoditySpec(it.key)?.basePrice
            if (basePrice != null) {
                val num = it.value / totalWeight * roidValue / basePrice + commodities.getOrElse(it.key) { 0f }
                commodities[it.key] = num
            }
        }
        commodities.forEach {
            prospector.cargo?.addCommodity(it.key, it.value)
        }
        if (prospector != Helper.sector?.playerFleet) return
        val text = "${type.name} roid collected" // extern
        val intel = MessageIntel(text, Misc.getNegativeHighlightColor())
        intel.icon = Helper.settings?.getSpriteName("intel", "damage_report")

        Helper.sector?.campaignUI?.addMessage(intel, CommMessageAPI.MessageClickAction.INCOME_TAB, prospector)
        roid.removeScript(this)
        // Dives summary intel? Later
    }
}