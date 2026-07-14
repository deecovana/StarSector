package scripts.campaign.cleanup

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.Misc

/**
 * Author: SafariJohn
 */
class Roider_ExpeditionMajorLootCleaner(entity: SectorEntityToken) : EveryFrameScript {
    private var entity: SectorEntityToken?
    private var token: SectorEntityToken?

    init {
        this.entity = entity
        token = entity.orbitFocus
    }

    override fun isDone(): Boolean {
        return entity == null && token == null
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (entity == null) {
            if (token != null) {
                Misc.fadeAndExpire(token)
                token = null
            }
            return
        }
        if (entity!!.isExpired || entity!!.containingLocation == null) {
            entity = null
            Misc.fadeAndExpire(token)
            token = null
        }
    }
}