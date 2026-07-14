package roiderUnion.rulecmd

import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AICores
import roiderUnion.ids.RoiderIds.Roider_Ranks

/**
 * Author: SafariJohn
 */
class Roider_AICores : AICores() {
    override fun personCanAcceptCores(): Boolean {
        return if (person == null || !buysAICores) false
        else Roider_Ranks.POST_BASE_COMMANDER == person.postId
    }
}