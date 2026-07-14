package scripts.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Factions
import roiderUnion.ids.RoiderFactions

/**
 * Author: SafariJohn
 */
class Roider_IndieRepMatcher : EveryFrameScript {
    override fun advance(amount: Float) {
        val player = Global.getSector().playerFaction
        val indies = Global.getSector().getFaction(Factions.INDEPENDENT)
        val roiders = Global.getSector().getFaction(RoiderFactions.ROIDER_UNION)
        if (indies?.relToPlayer !== roiders?.relToPlayer) {
            roiders.setRelationship(player.id, indies?.relToPlayer?.rel ?: 0f)
        }
    }
    override fun isDone(): Boolean { return false }
    override fun runWhilePaused(): Boolean { return true }
}