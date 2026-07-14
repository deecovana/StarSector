package scripts.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.ShipsAndWings
import java.awt.Color

/**
 * Author: SafariJohn
 */
class Roider_ConversionFleetIntel(fleet: CampaignFleetAPI, timeRemaining: Float) : BaseIntelPlugin(), FleetEventListener {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_ConversionFleetIntel::class.java, "fleetId", "f")
            x.aliasAttribute(Roider_ConversionFleetIntel::class.java, "timeRemaining", "t")
            x.aliasAttribute(Roider_ConversionFleetIntel::class.java, "offeringConversions", "o")
            x.aliasAttribute(Roider_ConversionFleetIntel::class.java, "playerVisible", "v")
        }
    }

    private val fleetId: String
    private var timeRemaining: Float
    private var offeringConversions: Boolean
    private var playerVisible: Boolean

    init {
        fleetId = fleet.id
        this.timeRemaining = timeRemaining
        offeringConversions = true
        playerVisible = false
    }

    override fun advance(amount: Float) {
        if (!playerVisible) {
            advanceImpl(amount)
            return
        }
        super.advance(amount)
    }

    override fun advanceImpl(amount: Float) {
        if (isEnding || isEnded) return
        val days: Float = Misc.getDays(amount)
        timeRemaining -= days
        val fleet: CampaignFleetAPI = Global.getSector().getEntityById(fleetId) as CampaignFleetAPI
        if (fleet.isVisibleToPlayerFleet && !playerVisible) {
            playerVisible = true
            Global.getSector().intelManager.addIntel(this)
        }
        if (timeRemaining <= 0 || !fleet.isAlive) {
            endAfterDelay()
        }
    }

    override fun getName(): String {
        val fleet = Global.getSector().getEntityById(fleetId) as? CampaignFleetAPI
        return if (fleet != null) Misc.ucFirst(
            fleet.faction.displayName
        ) + " Retrofit Fleet" else "Retrofit Fleet" // extern
    }


    override fun getIcon(): String {
        return Global.getSettings().getSpriteName("intel", "roider_expedition") // extern
    }

    override fun isPlayerVisible(): Boolean {
        return if (!playerVisible) false else super.isPlayerVisible()
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val h: Color = Misc.getHighlightColor()
        val pad = 3f
        val opad = 10f
        val fleet = Global.getSector().getEntityById(fleetId)
        if (fleet == null) {
            info.addImage(Global.getSector().getFaction(Factions.PIRATES).logo, width, 128f, opad)
            info.addPara("This retrofit fleet is no longer offering services.", pad) // extern
            return
        }
        offeringConversions = fleet.memoryWithoutUpdate.`is`(MemoryKeys.APR_RETROFITTING, true)
        val faction: FactionAPI = fleet.faction
        info.addImage(faction.logo, width, 128f, opad)
        var text = StringBuilder()
        text.append("You have encountered ").append(faction.personNamePrefixAOrAn)
            .append(" ").append(faction.personNamePrefix)
            .append(" retrofit fleet at the ")
            .append(fleet.containingLocation.nameWithLowercaseType)
            .append(".")
        info.addPara(text.toString(), pad)
        if (offeringConversions && timeRemaining > 0) {
            text = StringBuilder()
            text.append("They will continue offering services for another ")
                .append(timeRemaining.toInt()).append(" days.")
            info.addPara(text.toString(), pad, h, "" + timeRemaining.toInt())
        } else {
            info.addPara("They are no longer offering services.", pad) // extern
        }
    }

    override fun addBulletPoints(
        info: TooltipMakerAPI,
        mode: IntelInfoPlugin.ListInfoMode,
        isUpdate: Boolean,
        tc: Color,
        initPad: Float
    ) {
        val fleet = Global.getSector().getEntityById(fleetId)
        if (fleet != null) offeringConversions =
            fleet.memoryWithoutUpdate.`is`(MemoryKeys.APR_RETROFITTING, true)
        if (timeRemaining > 0 && offeringConversions && fleet != null) {
            val h: Color = Misc.getHighlightColor()
            info.addPara(
                "Faction: " + fleet.faction.displayName, // extern
                initPad, fleet.faction.color,
                fleet.faction.displayName
            )
            info.addPara(
                "Location: " + fleet.containingLocation.nameWithTypeIfNebula,
                0f, h, fleet.containingLocation.nameWithTypeIfNebula
            )
            info.addPara(timeRemaining.toInt().toString() + " days remaining", 0f, h, "" + timeRemaining.toInt())
        } else {
            info.addPara("No longer offering services", initPad)
        }
    }

    override fun getIntelTags(map: SectorMapAPI): Set<String> {
        val tags: MutableSet<String> = super.getIntelTags(map)
        tags.add(RoiderFactions.ROIDER_UNION)
        tags.add(Tags.INTEL_FLEET_LOG)
        val fleet = Global.getSector().getEntityById(fleetId) as? CampaignFleetAPI
        if (fleet != null) tags.add(fleet.faction.id)
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI): SectorEntityToken? {
        val fleet: CampaignFleetAPI = Global.getSector().getEntityById(fleetId) as CampaignFleetAPI
        if (fleet.isVisibleToPlayerFleet) return fleet
        return if (fleet.isInHyperspace) null else fleet.starSystem.center
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI, reason: CampaignEventListener.FleetDespawnReason, param: Any) {
        endAfterDelay()
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI, primaryWinner: CampaignFleetAPI, battle: BattleAPI) {
        // if fleet has lost all its Argosi, then it can no longer offer conversions
        var hasArgos = false
        for (m in fleet.membersWithFightersCopy) {
            if (m.hullId.startsWith(ShipsAndWings.ARGOS)) {
                hasArgos = true
                break
            }
        }
        if (!hasArgos) {
            endAfterDelay()
        }
    }

    override fun notifyEnded() {
        notifyEnding()
    }

    override fun notifyEnding() {
        offeringConversions = false
        sendUpdateIfPlayerHasIntel(listInfoParam, true)
    }
}