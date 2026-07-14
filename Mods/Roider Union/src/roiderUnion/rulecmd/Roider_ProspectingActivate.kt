package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import roiderUnion.roidMining.RoidMiningHelper
import roiderUnion.roidMining.ProspectingSpawner
import roiderUnion.helpers.Helper
import roiderUnion.roidMining.AsteroidBouncer

class Roider_ProspectingActivate : BaseCommandPlugin() {
    companion object {
        const val CUTTING_DAYS = 6f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?,
    ): Boolean {
        if (true) return false
        if (dialog?.interactionTarget == null) return false
        val playerFleet = Helper.sector?.playerFleet ?: return false
        val spawner = ProspectingSpawner(RoidMiningHelper.seed, playerFleet, dialog.interactionTarget!!)
        // configure
        Helper.sector?.addScript(spawner)
        RoidMiningHelper.seed = Helper.random.nextLong()
        AsteroidBouncer.FLEETS[playerFleet] = CUTTING_DAYS
        return true
    }
}