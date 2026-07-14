package roiderUnion.fleets.expeditionSpecials

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction.SalvageSpecialData
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction.SalvageSpecialPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIds

/**
 * Author: SafariJohn
 */
class PingTrapSpecial : BaseSalvageSpecial() {
    class PingTrapSpecialData : SalvageSpecialData {
        override fun createSpecialPlugin(): SalvageSpecialPlugin {
            return PingTrapSpecial()
        }
    }

    override fun init(dialog: InteractionDialogAPI, specialData: Any) {
        super.init(dialog, specialData)
        if (random.nextFloat() > 0.5f) {
            addText(ExternalStrings.PING_TRAP_ACTIVE)
            Helper.sector?.addPing(entity, RoiderIds.Pings.PING_TRAP)
            Helper.soundPlayer?.playUISound(RoiderIds.Sounds.SENSOR_BURST_ON_UI, 1f, 1f)
            Helper.sector?.playerFleet?.addScript(PingTrapScript())
        } else {
            if (random.nextFloat() > 0.5f) {
                addText(ExternalStrings.PING_TRAP_FAIL1)
            } else {
                addText(ExternalStrings.PING_TRAP_FAIL2)
            }
        }
        isDone = true
        setEndWithContinue(true)
        setShowAgain(false)
    }
}