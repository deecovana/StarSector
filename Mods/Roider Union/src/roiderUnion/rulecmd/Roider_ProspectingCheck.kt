package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.AsteroidAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.roidMining.AsteroidBouncer
import roiderUnion.roidMining.RoidMiningHelper

class Roider_ProspectingCheck : BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?,
    ): Boolean {
        if (true) return false
        if (AsteroidBouncer.FLEETS.containsKey(dialog?.interactionTarget)) return false
        val asteroid = dialog?.interactionTarget as? AsteroidAPI ?: return false
        // Instead of checking source, check if player fleet is in an asteroid belt/field or ring?
        // And grey out option with explanation if not.
        if (!RoidMiningHelper.isInTerrain(dialog.interactionTarget)) return false
        if (!RoidMiningHelper.isInTerrain(Helper.sector?.playerFleet)) return false
//        Memory.set(MemoryKeys.PROSPECTING_BLOCKED, true, 0f, asteroid)
        return true
    }
}