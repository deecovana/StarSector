package roiderUnion.nomads.old.bases

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.Factions
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys

class MinerSpawningListener(
    private val system: StarSystemAPI,
    entityId: String,
    factionId: String,
    weight: Float
) : FleetEventListener {
    private val sourceKey = MemoryKeys.MINER_SOURCE + entityId
    private val factionsKey = MemoryKeys.MINER_FACTION_WEIGHTS + entityId

    init {
        Memory.set(sourceKey, weight, system)
        val factionWeights = mutableMapOf<String, Float>()
        factionWeights[Factions.INDEPENDENT] = 1f
        factionWeights[Factions.PIRATES] = 1f
        factionWeights[factionId] = weight
        Memory.set(factionsKey, factionWeights, system)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        Helper.sector?.starSystems?.forEach {
            Memory.unset(sourceKey, it)
            Memory.unset(factionsKey, it)
        }
//        Memory.unsetFlag(MemoryKeys.MINER_SOURCE, RoiderIndustries.DIVES, system)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}
}