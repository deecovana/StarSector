package roiderUnion.retrofits.argos

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.api.combat.CombatReadinessPlugin
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.hullmods.RoiderHullmods

class ArgosConversionAbility : BaseDurationAbility() {
    companion object {
        fun alias(x: XStream) {
            val jClass = ArgosConversionAbility::class.java
            x.alias(Aliases.AABIL, jClass)
//            x.aliasAttribute(jClass, "interactor", "rI")
//            x.aliasAttribute(jClass, "view", "rV")
        }

        const val HOSTILE_TRACKING_DISTANCE = 1500f
    }

    override fun activateImpl() {
        if (entity == null) return
        if (!isUsable) return
        if (entity.isPlayerFleet) {
            ArgosRetrofitController(entity).openDialog()
        }
    }

    override fun isUsable(): Boolean {
        if (!entity.isPlayerFleet) return false
        val fleet = entity as? CampaignFleetAPI ?: return false
        if (isHostileNearbyAndAware(fleet)) return false
        val crPlugin = Helper.settings?.crPlugin
        return fleet.fleetData?.membersListCopy?.any {
            it.variant?.hasHullMod(RoiderHullmods.CONVERSION_DOCK) == true
                    && !it.isMothballed
                    && (it.repairTracker?.cr ?: 0f) > (crPlugin?.getMalfunctionThreshold(it.stats) ?: 0f)
        } ?: false
    }

    private fun isHostileNearbyAndAware(playerFleet: CampaignFleetAPI): Boolean {
        for (fleet in playerFleet.containingLocation?.fleets ?: listOf()) {
            if (fleet.ai == null) continue  // dormant Remnant fleets
            if (fleet.faction.isPlayerFaction) continue
            if (fleet.isStationMode) continue
            if (!fleet.isHostileTo(playerFleet)) continue
            if (fleet.battle != null) continue
            if (playerFleet.getVisibilityLevelTo(fleet) == VisibilityLevel.NONE) continue
            if (fleet.fleetData.membersListCopy.isEmpty()) continue
            val dist = Misc.getDistance(playerFleet.location, fleet.location)
            if (dist > HOSTILE_TRACKING_DISTANCE) continue
            if (fleet.ai is ModularFleetAIAPI) {
                val ai = fleet.ai as ModularFleetAIAPI
                if (ai.tacticalModule != null &&
                    (ai.tacticalModule.isFleeing || ai.tacticalModule.isMaintainingContact ||
                            ai.tacticalModule.isStandingDown)
                ) {
                    continue
                }
            }
            return true
        }
        return false
    }

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        tooltip.addTitle(spec.name)
        val pad = 10f
        tooltip.addPara(ExternalStrings.ARGOS_DESC, pad)
        if (!entity.isPlayerFleet) return
        var noDocks = true
        var notReady = true
        val fleet: CampaignFleetAPI = entity as CampaignFleetAPI
        val crPlugin: CombatReadinessPlugin = Global.getSettings().crPlugin
        for (ship in fleet.fleetData.membersListCopy) {
            if (ship.variant.hasHullMod(RoiderHullmods.CONVERSION_DOCK)) {
                noDocks = false
                if (ship.isMothballed) continue
                val mal: Float = crPlugin.getMalfunctionThreshold(ship.stats)
                val cr: Float = ship.repairTracker.cr
                if (cr > mal) {
                    notReady = false
                    break
                }
            }
        }
        if (noDocks) {
            tooltip.addPara(ExternalStrings.ARGOS_NO_DOCKS, Misc.getNegativeHighlightColor(), pad)
        } else if (notReady) {
            tooltip.addPara(
                ExternalStrings.ARGOS_LOW_CR,
                Misc.getNegativeHighlightColor(),
                pad
            )
        } else if (isHostileNearbyAndAware(fleet)) {
            tooltip.addPara(
                ExternalStrings.ARGOS_HOSTILE_TRACKING,
                Misc.getNegativeHighlightColor(),
                pad
            )
        } else {
            tooltip.addPara(ExternalStrings.ARGOS_READY, Misc.getPositiveHighlightColor(), pad)
        }
        addIncompatibleToTooltip(tooltip, expanded)
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    override fun applyEffect(amount: Float, level: Float) {}
    override fun deactivateImpl() {}
    override fun cleanupImpl() {}
}