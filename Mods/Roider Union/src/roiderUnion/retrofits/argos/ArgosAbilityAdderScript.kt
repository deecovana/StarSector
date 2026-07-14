package roiderUnion.retrofits.argos

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.Categories
import roiderUnion.ids.RoiderIds
import roiderUnion.ids.hullmods.RoiderHullmods

/**
 * Author: SafariJohn
 */
class ArgosAbilityAdderScript : EveryFrameScript {
    companion object {
        fun alias(x: XStream) {
            val jClass = ArgosAbilityAdderScript::class.java
            x.alias(Aliases.AABILADD, jClass)
            x.aliasAttribute(jClass, "interval", "i")
        }
    }

    private val interval = IntervalUtil(0.1f, 0.1f)

    override fun isDone(): Boolean {
        return Helper.sector?.characterData?.abilities?.contains(RoiderIds.Abilities.ARGOS_RETROFITS) == true
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        for (ship in Helper.sector?.playerFleet?.fleetData?.membersListCopy ?: emptyList()) {
            if (ship.variant?.hasHullMod(RoiderHullmods.CONVERSION_DOCK) == true) {
                Helper.sector?.characterData?.addAbility(RoiderIds.Abilities.ARGOS_RETROFITS)
                Helper.sector?.playerFleet?.addAbility(RoiderIds.Abilities.ARGOS_RETROFITS)
                Helper.sector?.campaignUI?.addMessage(object : BaseIntelPlugin() {
                    override fun getIcon(): String {
                        return Helper.settings?.getSpriteName(Categories.INTEL, RoiderIds.Abilities.ARGOS_RETROFITS) ?: ""
                    }

                    override fun createIntelInfo(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode) {
                        info.addPara(ExternalStrings.ARGOS_ABILITY_ADDED, getTitleColor(mode), 0f)
                    }
                })
                break
            }
        }
    }
}