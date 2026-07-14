package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys

class Roider_MadRockpiperSetRant : BaseCommandPlugin() {
    private val eventDays: Float
        get() = Roider_MadRockpiper.EVENT_DAYS

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val memory = memoryMap?.get(MemKeys.LOCAL) ?: return false
        val key = when (params?.firstOrNull()?.getString(memoryMap)) {
            Roider_MadRockpiperCheckRant.SP -> MemoryKeys.MRP_SP_RANT
            Roider_MadRockpiperCheckRant.INHOSPITABLE -> MemoryKeys.MRP_INHOSP_RANT
            else -> MemoryKeys.MRP_DEFAULT_RANT
        }
        val id = Memory.getNullable(key, memory, { true }, { null }) as? String
        if (id.isNullOrEmpty()) {
            memory[key, ruleId] = eventDays
            return true
        }
        return false
    }
}