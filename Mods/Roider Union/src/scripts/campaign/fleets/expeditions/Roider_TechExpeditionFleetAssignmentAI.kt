package scripts.campaign.fleets.expeditions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.EncounterOption
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ids.MemoryKeys
import scripts.Roider_Misc

class Roider_TechExpeditionFleetAssignmentAI(fleet: CampaignFleetAPI, route: RouteData?, pirate: Boolean) :
    RouteFleetAssignmentAI(fleet, route) {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_TechExpeditionFleetAssignmentAI::class.java, "originalFaction", "o")
            x.aliasAttribute(Roider_TechExpeditionFleetAssignmentAI::class.java, "pirate", "p")
            x.aliasAttribute(Roider_TechExpeditionFleetAssignmentAI::class.java, "piracyCheck", "c")
        }
    }

    private var originalFaction: String
    private var pirate: Boolean
    private var piracyCheck = IntervalUtil(0.2f, 0.4f)

    init {
        originalFaction = fleet.faction.id
        this.pirate = pirate
    }

    override fun getTravelActionText(segment: RouteSegment): String {
        //if (segment.systemTo == route.getMarket().getContainingLocation()) {
        return if (segment.to === route.market.primaryEntity) {
            "returning to " + route.market.name
        } else "travelling"
    }

    override fun getInSystemActionText(segment: RouteSegment): String {
        return "exploring"
    }

    override fun getStartingActionText(segment: RouteSegment): String {
        return if (segment.from === route.market.primaryEntity) {
            "preparing for an expedition"
        } else "exploring"
    }

    override fun addLocalAssignment(segment: RouteSegment, justSpawned: Boolean) {
        val custom = segment.custom
        if (custom != null && custom is List<*>) {
            val stashes = custom as MutableList<SectorEntityToken>
            if (stashes.isEmpty()) {
                route.expire()
                return
            }

            // Pick closest stash
            var target: SectorEntityToken? = null
            var minDist = (Short.MAX_VALUE * Short.MAX_VALUE).toFloat()
            for (e in stashes) {
                val dist: Float = Roider_Misc.getDistanceSquared(fleet, e)
                if (dist < minDist) {
                    minDist = dist
                    target = e
                }
            }

            // Fallback in case no stashes or something
            var collectStash = true
            if (target == null) {
                val loc: EntityLocation = BaseThemeGenerator.pickHiddenLocationNotNearStar(
                    route.random, fleet.starSystem, 50f + route.random.nextFloat() * 100f, null
                )
                var currLoc: Vector2f? = loc.location
                if (loc.orbit != null) currLoc = loc.orbit.computeCurrentLocation()
                if (currLoc == null) currLoc = Vector2f()
                target = fleet.starSystem.createToken(currLoc)
                collectStash = false
            } else {
                stashes.remove(target)
            }
            if (justSpawned) {
                val loc: Vector2f = Misc.getPointAtRadius(Vector2f(target?.location), 500f)
                fleet.setLocation(loc.x, loc.y)
            }
            if (collectStash) fleet.addAssignment(
                FleetAssignment.GO_TO_LOCATION,
                target,
                100f,
                "collecting stash",
                NomadStashPickupScript(fleet, target)
            ) else fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 100f, "exploring")
        } else {
            super.addLocalAssignment(segment, justSpawned)
        }
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Global.getSector().clock.convertToDays(amount)
        piracyCheck.advance(days)
        if (piracyCheck.intervalElapsed()) {
            if (pirate) doPiracyCheck()
            if (fleet.faction.id == Factions.PIRATES) {
                Misc.makeNoRepImpact(fleet, "roider_pirate")
            } else {
                // Clear tags
                Misc.makeNotLowRepImpact(fleet, "roider_tOff")
                if (pirate || !fleet.isTransponderOn || isThieveryDetected) Misc.makeLowRepImpact(
                    fleet,
                    "roider_tOff"
                )
            }
        }
    }

    protected fun doPiracyCheck() {
        if (fleet.battle != null) return
        val isCurrentlyPirate = fleet.faction.id == Factions.PIRATES
        if (fleet.isTransponderOn && !isCurrentlyPirate) {
            return
        }
        val visible: List<CampaignFleetAPI> = Misc.getVisibleFleets(fleet, false)
        if (isCurrentlyPirate) {
            if (visible.isEmpty()) {
                fleet.setFaction(originalFaction, true)
                Misc.clearTarget(fleet, true)
            }
            return
        }
        if (visible.size == 1) {
            var weakerCount = 0
            for (other in visible) {
                if (fleet.ai != null &&
                    Global.getSector().getFaction(Factions.PIRATES).isHostileTo(other.faction)
                ) {
                    val option: EncounterOption = fleet.ai.pickEncounterOption(null, other, true)
                    if (option == EncounterOption.ENGAGE || option == EncounterOption.HOLD) {
                        val dist: Float = Misc.getDistance(fleet.location, other.location)
                        val level: VisibilityLevel = other.getVisibilityLevelTo(fleet)
                        val seesComp = level == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS ||
                                level == VisibilityLevel.COMPOSITION_DETAILS
                        if (dist < 800f && seesComp) {
                            weakerCount++
                        }
                    }
                }
            }
            if (weakerCount == 1) {
                fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_PIRATE, true)
                fleet.isNoFactionInName = true
                fleet.setFaction(Factions.PIRATES, true)
            }
        }
    }

    private val isThieveryDetected: Boolean
        get() = Misc.flagHasReason(
            fleet.memoryWithoutUpdate,
            MemFlags.MEMORY_KEY_MAKE_HOSTILE,
            MemoryKeys.THIEF_KEY
        )
}