package scripts.campaign.fleets.expeditions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ArrowData
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.MemoryKeys
import java.awt.Color

/**
 * Author: SafariJohn
 */
class Roider_TechExpeditionIntel(
    route: RouteData, sourceMarket: String,
    destSystem: String, factionId: String, thiefId: String
) : BaseIntelPlugin() {
    companion object {
        const val LAUNCHED = "launched"
        const val ENCOUNTERED = "encountered"
        const val ENCOUNTERED_THIEF = "encounteredThief"
        const val THIEF = "thief"
        const val LOST = "lost"
        const val RETURNED = "returned"

        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "route", "rt")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "source", "s")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "dest", "d")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "factionId", "f")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "thiefId", "t")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "endQueueDelay", "eqd")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "lostDelay", "lod")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "endDelay", "ed")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "launched", "ld")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "knownThief", "kt")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "returned", "r")
            x.aliasAttribute(Roider_TechExpeditionIntel::class.java, "lost", "l")
        }
    }

    private val route: RouteData
    private val source: String
    private val dest: String
    private val factionId: String
    private val thiefId: String
    private var endQueueDelay: Float
    private var lostDelay: Float
    private var endDelay: Float
    private var launched: Boolean
    private var knownThief: Boolean
    private var returned: Boolean
    private var lost: Boolean

    init {
        this.route = route
        source = sourceMarket
        dest = destSystem
        this.factionId = factionId
        this.thiefId = thiefId
        endQueueDelay = baseDaysAfterEnd
        lostDelay = route.segments.get(3).daysMax
        endDelay = baseDaysAfterEnd
        launched = false
        knownThief = false
        returned = false
        lost = false
    }

    // ----------
    // Mechanical
    // ----------
    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (route.delay > 0) return
        if (endDelay <= 0) {
            Global.getSector().removeScript(this)
            return
        }
        if (returned) {
            endDelay -= Misc.getDays(amount)
            return
        }
        if (!returned && route.isExpired && route.currentIndex == route.segments.size - 1) {
            sendUpdateIfPlayerHasIntel(RETURNED, true)
            setListInfoParam(RETURNED)
            returned = true
            return
        }

//        if (isPlayerVisible() && Global.getSector().getScripts().contains(this)
//                    && !isImportant()) {
//			Global.getSector().removeScript(this);
//        }

        // Player encounters fleet before picking up intel
        if (!isPlayerVisible && isFleetVisible) {
            if (Global.getSector().intelManager.hasIntelQueued(this)) Global.getSector().intelManager.unqueueIntel(this)
            launched = true

            // Player is marked thief
            if (isPlayerThiefSpotted) {
                setListInfoParam(ENCOUNTERED_THIEF)
                Global.getSector().intelManager.addIntel(this)
                thieveryDetected()
            } else {
                setListInfoParam(ENCOUNTERED)
                Global.getSector().intelManager.addIntel(this)
                setListInfoParam(null)
            }

//			Global.getSector().removeScript(this);
            return
        }
        if (!launched) {
            launched = true
            sendUpdateIfPlayerHasIntel(LAUNCHED, true)
            //            setListInfoParam(LAUNCHED);
            return
        }

        // Player is marked thief
        if (isPlayerVisible && isPlayerThiefSpotted) {
            sendUpdateIfPlayerHasIntel(THIEF, false)
            thieveryDetected()
        }
        val days: Float = Misc.getDays(amount)
        if (!isPlayerVisible && endQueueDelay > 0) {
            endQueueDelay -= days
            if (endQueueDelay <= 0) {
                Global.getSector().intelManager.unqueueIntel(this)
            }
        }
        if (route.isExpired) {
            if (isFleetVisible) {
                lostDelay = 0f
                //                sendUpdateIfPlayerHasIntel("lost", true);
//                lost = true;
            }
            if (lostDelay > 0) {
                lostDelay -= days
            } else {
                endDelay -= days
            }
            if (lostDelay <= 0 && !lost) {
                sendUpdateIfPlayerHasIntel(LOST, true)
                setListInfoParam(LOST)
                lost = true
            }
        }
    }

    // Already dealt with
    private val isPlayerMarkedThief: Boolean
        get() = if (knownThief) false else Global.getSector().playerFleet.memoryWithoutUpdate
            .contains(MemoryKeys.THIEF_KEY + thiefId) // Already dealt with
    private val isPlayerThiefSpotted: Boolean
        get() {
            if (!isPlayerMarkedThief) return false
            val fleet: CampaignFleetAPI = route.activeFleet ?: return false
            return (fleet.visibilityLevelOfPlayerFleet == VisibilityLevel.COMPOSITION_DETAILS
                    || fleet.visibilityLevelOfPlayerFleet == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS)
        }

    private fun thieveryDetected() {
        setListInfoParam(THIEF)
        knownThief = true
        Misc.setFlagWithReason(
            route.activeFleet.memoryWithoutUpdate,
            MemFlags.MEMORY_KEY_MAKE_HOSTILE, MemoryKeys.THIEF_KEY,
            true, Short.MAX_VALUE.toFloat()
        )
        val impact = CoreReputationPlugin.CustomRepImpact()
        impact.limit = RepLevel.HOSTILE
        impact.delta = -CoreReputationPlugin.RepRewards.SMALL
        val action = CoreReputationPlugin.RepActionEnvelope(
            CoreReputationPlugin.RepActions.CUSTOM, impact,
            null, null, true
        )
        Global.getSector().adjustPlayerReputation(action, route.factionId)
        Global.getSector().adjustPlayerReputation(action, route.activeFleet.commander)
    }

    // -------
    // Display
    // -------
    override fun addBulletPoints(info: TooltipMakerAPI, mode: ListInfoMode) {
        val faction: FactionAPI = Global.getSector().getFaction(factionId)
        val market: MarketAPI = Global.getSector().economy.getMarket(source)
        val target: StarSystemAPI = Global.getSector().getStarSystem(dest)
        val pad = 3f
        val opad = 10f
        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad
        val tc: Color = getBulletColorForMode(mode)
        bullet(info)
        var infoParam = getListInfoParam() as String?
        if (infoParam == null) infoParam = ""
        if (mode != ListInfoMode.IN_DESC) {
            info.addPara(
                "Faction: " + faction.displayName, initPad, tc,
                faction.baseUIColor, faction.displayName
            )
            initPad = 0f
            val label: LabelAPI =
                info.addPara("From " + market.name + " to " + target.nameWithTypeIfNebula, tc, initPad)
            label.setHighlight(
                market.name,
                target.nameWithTypeIfNebula
            )
            label.setHighlightColors(
                market.faction.baseUIColor,
                getStarSystemColor(target)
            )
        }
        when (infoParam) {
            LAUNCHED -> info.addPara("Expedition launched", tc, initPad)
            ENCOUNTERED -> info.addPara("Expedition encountered", tc, initPad)
            ENCOUNTERED_THIEF -> {
                info.addPara("Expedition encountered", tc, initPad)
                initPad = 0f
                info.addPara("Your thievery has been discovered", Misc.getNegativeHighlightColor(), initPad)
            }

            THIEF -> info.addPara("Your thievery has been discovered", Misc.getNegativeHighlightColor(), initPad)
            LOST -> info.addPara("Expedition lost", tc, initPad)
            RETURNED -> info.addPara("Expedition over", tc, initPad)
            else -> {
                val delay: Float = route.delay
                if (delay > 0) {
                    addDays(info, "until departure", delay, tc, initPad)
                } else {
                    info.addPara("On expedition", tc, initPad)
                }
            }
        }
        unindent(info)
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: ListInfoMode) {
        val c: Color = getTitleColor(mode)
        info.addPara(name, c, 0f)
        addBulletPoints(info, mode)
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val faction: FactionAPI = Global.getSector().getFaction(factionId)
        val market: MarketAPI = Global.getSector().economy.getMarket(source)
        val target: StarSystemAPI = Global.getSector().getStarSystem(dest)
        val tc: Color = Misc.getTextColor()
        val opad = 10f

        // Faction logo
        info.addImage(faction.logo, width, 128f, opad)

        // Get star system color
        var starColor: Color = Misc.getButtonTextColor()
        val star: PlanetAPI? = target.star
        if (star != null) starColor = star.spec.iconColor
        val label: LabelAPI = info.addPara(
            "Your contacts " + market.onOrAt + " " + market.name +
                    " let you know that " +
                    faction.personNamePrefix + " roiders are preparing a tech expedition and will soon depart for " +
                    target.nameWithTypeIfNebula + ".",
            opad, tc,
            faction.baseUIColor,
            faction.personNamePrefix
        )
        label.setHighlight(
            market.name,
            faction.personNamePrefix,
            target.nameWithTypeIfNebula
        )
        label.setHighlightColors(
            market.faction.baseUIColor,
            faction.baseUIColor,
            starColor
        )
        addBulletPoints(info, ListInfoMode.IN_DESC)
    }

    private fun getStarSystemColor(system: StarSystemAPI): Color {
        var starColor: Color = Misc.getButtonTextColor()
        val star: PlanetAPI? = system.star
        if (star != null) starColor = star.spec.iconColor
        return starColor
    }

    override fun getArrowData(map: SectorMapAPI?): List<ArrowData>? {
        val market: MarketAPI = Global.getSector().economy.getMarket(source)
        val target: StarSystemAPI = Global.getSector().getStarSystem(dest)
        val result: MutableList<ArrowData> = ArrayList<ArrowData>()
        if (market.containingLocation === target && market.containingLocation != null &&
            !market.containingLocation.isHyperspace
        ) {
            return null
        }
        var entityFrom: SectorEntityToken? = market.primaryEntity
        if (map != null) {
            val iconEntity: SectorEntityToken? = map.getIntelIconEntity(this)
            if (iconEntity != null) {
                entityFrom = iconEntity
            }
        }
        val arrow = ArrowData(entityFrom, target.center)
        arrow.color = factionForUIColors.baseUIColor
        result.add(arrow)
        return result
    }

    override fun getIcon(): String {
        return Global.getSettings().getSpriteName("intel", "roider_expedition")
    }

    override fun getMapLocation(map: SectorMapAPI): SectorEntityToken {
        return route.market.primaryEntity
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(factionId)
    }

    override fun getName(): String {
        val faction: FactionAPI = Global.getSector().getFaction(factionId)
        return Misc.ucFirst(faction.personNamePrefix) + " Tech Expedition"
    }

    override fun getSmallDescriptionTitle(): String {
         return name
    }

    override fun getSortString(): String {
        return "Roider Tech Expedition"
    }

    override fun getIntelTags(map: SectorMapAPI): Set<String> {
        val tags: MutableSet<String> = super.getIntelTags(map)
        tags.add("Roider Tech Expedition")
        val faction: FactionAPI = Global.getSector().getFaction(factionId)
        tags.add(faction.id)
        return tags
    }

    // ---------------
    // Intel Mechanics
    // ---------------
    override fun shouldRemoveIntel(): Boolean {
        if (route.delay > 0) return false
        if (isImportant) return false
        return if (isFleetAlive) false else route.isExpired && endDelay <= 0
    }

    private val isFleetAlive: Boolean
        get() = route.activeFleet != null && route.activeFleet.isAlive
    private val isFleetVisible: Boolean
        get() = route.activeFleet != null && route.activeFleet.isVisibleToPlayerFleet

    override fun setImportant(important: Boolean) {
        super.setImportant(important)
        //		if (isImportant()) {
//			if (!Global.getSector().getScripts().contains(this)) {
//				Global.getSector().addScript(this);
//			}
//		} else {
//			Global.getSector().removeScript(this);
//		}
    }

    override fun reportRemovedIntel() {
        super.reportRemovedIntel()
        Global.getSector().removeScript(this)
    }
}