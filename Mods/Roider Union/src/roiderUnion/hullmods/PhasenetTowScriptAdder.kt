package roiderUnion.hullmods

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import roiderUnion.helpers.Helper
import roiderUnion.ids.hullmods.RoiderHullmods

/**
 * Finds fleets with ships that have PhasenetTowPattern hullmod
 * and adds a transient script to them to handle the campaign effects.
 */
class PhasenetTowScriptAdder : EveryFrameScript {
    private var index = 0

    init {
        Helper.sector?.allLocations?.forEach { it.fleets?.forEach { fleet -> addPhasenetScriptIfApplicable(fleet) } }
    }

    override fun advance(amount: Float) {
        if (Helper.sector?.playerFleet != null) {
            addPhasenetScriptIfApplicable(Helper.sector!!.playerFleet)
        }
        val fleets = mutableListOf<CampaignFleetAPI>()
        Helper.sector?.allLocations?.forEach { fleets.addAll(it.fleets) }
        if (fleets.isEmpty()) return
        if (index >= fleets.size) index = 0
        addPhasenetScriptIfApplicable(fleets[index])
        index++
    }

    private fun addPhasenetScriptIfApplicable(fleet: CampaignFleetAPI) {
        if (hasPhasenet(fleet) && !fleetHasTowScript(fleet)) Helper.sector?.addTransientScript(PhasenetTowScript(fleet))
    }

    private fun hasPhasenet(fleet: CampaignFleetAPI): Boolean = fleet.membersWithFightersCopy
        ?.any { it.variant?.hasHullMod(RoiderHullmods.PHASENET_TOW_PATTERN) == true } == true

    private fun fleetHasTowScript(fleet: CampaignFleetAPI): Boolean = Helper.sector?.transientScripts
        ?.filterIsInstance<PhasenetTowScript>()
        ?.any { it.fleet === fleet } == true

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = true
}