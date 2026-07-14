package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.ModPlugin
import roiderUnion.helpers.Helper
import starship_legends.RepRecord
import java.awt.Color

/**
 * Author: SafariJohn
 */
open class BaseRetrofitManager @JvmOverloads constructor(
    open val fitter: String,
    entity: SectorEntityToken?, faction: FactionAPI,
    addIntel: Boolean = true
) : BaseIntelPlugin(), RetrofitVerifier {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(BaseRetrofitManager::class.java, "fitter", "ft")
            x.aliasAttribute(BaseRetrofitManager::class.java, "entity", "e")
            x.aliasAttribute(BaseRetrofitManager::class.java, "faction", "f")
            x.aliasAttribute(BaseRetrofitManager::class.java, "queued", "q")
            x.aliasAttribute(BaseRetrofitManager::class.java, "completedOne", "c")
            x.alias("roider_retTrk", RetrofitTracker::class.java)
            RetrofitTracker.aliasAttributes(x)
        }

        protected const val COLUMNS = 7
    }

    class RetrofitTracker(val ship: FleetMemberAPI, val data: RetrofitData, val cost: Double) {
        companion object {
            fun aliasAttributes(x: XStream) {
                x.aliasAttribute(RetrofitTracker::class.java, "ship", "s")
                x.aliasAttribute(RetrofitTracker::class.java, "data", "da")
                x.aliasAttribute(RetrofitTracker::class.java, "cost", "c")
                x.aliasAttribute(RetrofitTracker::class.java, "timeRemaining", "t")
                x.aliasAttribute(RetrofitTracker::class.java, "isPaused", "p")
                x.aliasAttribute(RetrofitTracker::class.java, "pauseReason", "r")
                x.aliasAttribute(RetrofitTracker::class.java, "isDone", "d")
            }
        }

        var timeRemaining: Double = data.time
        var isPaused: Boolean
            private set
        var pauseReason: String
            private set
        var isDone = timeRemaining <= 0
            private set

        init {
            isPaused = false
            pauseReason = ""
        }

        fun advance(days: Double) {
            timeRemaining -= days
            if (timeRemaining <= 0) isDone = true
        }

        val daysRemaining: Int
            get() = timeRemaining.toInt()

        fun pause(reason: String) {
            isPaused = true
            pauseReason = reason
        }

        fun unpause() {
            if (!isPaused) return
            isPaused = false
            pauseReason = ""
        }
    }

    val entity: SectorEntityToken?
    val faction: FactionAPI
    val queued = mutableListOf<RetrofitTracker>()
    private var completedOne = false

    init {
        this.entity = entity
        this.faction = faction
        if (entity != null && addIntel) {
            Global.getSector().intelManager.addIntel(this)
        }
    }

    override fun advanceImpl(amount: Float) {
        if (entity == null || entity.market?.isPlanetConditionMarketOnly == true) {
            endImmediately()
            return
        }
        if (queued.isEmpty()) return
        if (Helper.sector?.isPaused == true) return
        isHidden = false
        val days = Helper.sector?.clock?.convertToDays(amount)?.toDouble() ?: 0.0
        val active = queued[0]
        if (active.isPaused) return
        active.advance(days)
        if (active.isDone) {
            deliverShip(active, false)
            completedOne = true
            queued.removeAt(0)
        }
    }

    fun transferDMods(source: FleetMemberAPI, target: FleetMemberAPI) {
        val dMods = mutableListOf<HullModSpecAPI>()
        DModManager.getModsWithTags(Tags.HULLMOD_DMOD)
            .filter { source.variant?.hullMods?.contains(it.id) == true }
            .forEach { dMods += it }
        if (dMods.isEmpty()) return
        val targetVariant: ShipVariantAPI = target.variant.clone()
        targetVariant.originalVariant = null
        DModManager.setDHull(targetVariant)
        target.setVariant(targetVariant, false, true)
        val sourceDMods = dMods.size
        DModManager.removeUnsuitedMods(targetVariant, dMods)
        val addedDMods = dMods.size
        if (dMods.isNotEmpty()) {
            for (mod in dMods) {
                targetVariant.addPermaMod(mod.id)
            }

            if (sourceDMods > addedDMods) {
                DModManager.addDMods(target, false, sourceDMods - addedDMods, null)
            }
        } else if (sourceDMods > 0) {
            DModManager.addDMods(target, false, sourceDMods, null)
        }
    }

    private fun transferSMods(source: FleetMemberAPI, target: FleetMemberAPI) {
        val sMods: LinkedHashSet<String> = source.variant.sMods
        for (mod in sMods) {
            target.variant.addPermaMod(mod, true)
        }
    }

    private fun transferStarshipLegendsRep(source: FleetMemberAPI?, target: FleetMemberAPI?) {
        if (!ModPlugin.hasStarshipLegends) return
        if (!RepRecord.existsFor(source)) return
        RepRecord.transfer(source, target)
    }

    private fun notifyPlayerCompleted(finished: RetrofitTracker) {
        notifyPlayerCompleted(finished.ship, finished.data)
    }

    private fun notifyPlayerCompleted(ship: FleetMemberAPI, data: RetrofitData) {
        var text = "The " + ship.shipName + " has finished retrofitting to "
        var targetSpec: ShipHullSpecAPI? = null
        for (spec in Global.getSettings().allShipHullSpecs) {
            if (spec.hullId == data.targetHull) {
                targetSpec = spec
                break
            }
        }
        if (targetSpec == null) return
        text += targetSpec.hullNameWithDashClass
        text += if (entity != null) " at " + entity.name + "." else "."
        val message = MessageIntel(text)
        message.icon = Global.getSettings().getSpriteName("intel", "roider_retrofit")
        Global.getSector().campaignUI.addMessage(message)
    }

    fun addToQueue(tracker: RetrofitTracker) {
        if (tracker.timeRemaining <= 0) {
            deliverShip(tracker, true)
        } else {
            queued.add(tracker)
        }
    }

    private fun deliverShip(tracker: RetrofitTracker, toPlayer: Boolean) {
        // Get storage if can
        var toPlayer = toPlayer
        var storage: SubmarketAPI? = null
        if (entity != null && entity.market != null) {
            storage = entity.market.getSubmarket(Submarkets.SUBMARKET_STORAGE)
        }
        // Fallback to player's fleet
        if (storage == null) {
            toPlayer = true
        }

        // Create finished ship
        val ship: FleetMemberAPI =
            Global.getFactory().createFleetMember(FleetMemberType.SHIP, tracker.data.targetHull + "_Hull")
        ship.shipName = tracker.ship.shipName

        // Transfer D-mods
//            transferDMods(active.ship, ship);

        // Transfer S-mods
        transferSMods(tracker.ship, ship)

        // Transfer Starship Legends reputation
        transferStarshipLegendsRep(tracker.ship, ship)
        ship.repairTracker.cr = tracker.ship.repairTracker.cr
        if (toPlayer) {
            // Add ship to player's fleet
            Global.getSector().playerFleet.fleetData.addFleetMember(ship)
        } else {
            storage?.cargo?.mothballedShips?.addFleetMember(ship)
        }
        notifyPlayerCompleted(tracker)
    }

    val retrofits: List<RetrofitData>
        get() = RetrofitsKeeper.getRetrofits(this, fitter)

    override fun verifyData(
        id: String?, fitter: String?,
        source: String?, target: String?, cost: Double,
        time: Double, rep: RepLevel?, commission: Boolean
    ): RetrofitData? {
        // Recalculate cost if there's a market
        var cost = cost
        if (entity != null && entity.market != null && !entity.market.isPlanetConditionMarketOnly) {
            cost = RetrofitsKeeper.calculateCost(source, target, entity.market)
        }
        return RetrofitData(
            id!!, fitter!!, source!!, target!!, cost,
            time, rep!!, commission
        )
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    //////////////////////////////
    //  INTEL PLUGIN METHODS    //
    //////////////////////////////

    override fun getSmallDescriptionTitle(): String {
        return "Retrofits at " + entity?.name // extern
    }

    override fun getFactionForUIColors(): FactionAPI {
        return faction
    }

    override fun getIcon(): String {
        return Global.getSettings().getSpriteName("intel", "roider_retrofit") // extern
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode) {
        val opad = 10f // 10f
        info.addTitle("Retrofits at " + entity?.name)
        if (queued.isEmpty()) {
            info.addPara("No retrofits in progress.", opad)
            //            info.addPara("Pick up completed retrofits from storage.", opad);
        } else {
            // Show time remaining for first hull
            val time = StringBuilder()
            val highlights: MutableList<String> = ArrayList()
            val first = queued[0]
            var firstSpec: ShipHullSpecAPI? = null
            var firstTargetSpec: ShipHullSpecAPI? = null
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (spec.baseHullId == first.data.sourceHull || spec.hullId == first.data.sourceHull) {
                    firstSpec = spec
                }
                if (spec.hullId == first.data.targetHull) {
                    firstTargetSpec = spec
                }
                if (firstSpec != null && firstTargetSpec != null) break
            }
            if (firstSpec == null || firstTargetSpec == null) {
                info.addPara(
                    "Could not find ShipHullSpecAPI for " + first.data.targetHull, // extern
                    opad,
                    Misc.getNegativeHighlightColor()
                )
                return
            }
            if (first.isPaused) {
                info.addPara(
                    "Retrofits paused because " + first.pauseReason + ".",
                    Misc.getNegativeHighlightColor(),
                    opad
                )
            } else {
                time.append(firstSpec.nameWithDesignationWithDashClass).append("\n")
                time.append("to ").append(firstTargetSpec.nameWithDesignationWithDashClass).append("\n")
                if (first.daysRemaining == 1) {
                    time.append("- Complete in %s day\n")
                } else {
                    time.append("- Complete in %s days\n")
                }
                highlights.add("" + first.daysRemaining)

                // Give projected completion date of all queued hulls
                if (queued.size > 1) {
                    var days = 0
                    for (tracker in queued) {
                        days += tracker.daysRemaining
                    }
                    if (days == 1) {
                        time.append("\nAll queued retrofits will be complete in %s day")
                    } else {
                        time.append("\nAll queued retrofits will be complete in %s days") // extern
                    }
                    highlights.add("" + days)
                }
                info.addPara(time.toString(), opad, Misc.getHighlightColor(), "")
            }
        }
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
//        float pad = 3f;
        val opad = 10f // 10f

        // Description of what this intel is
        // Location of retrofitting
        // What about finished retrofits?
        // What about when everything is finished?
        info.addImage(faction.logo, width, 128f, opad)
        if (queued.isEmpty()) {
            info.addPara("No retrofits in progress.", opad)
            if (completedOne) info.addPara("Pick up completed retrofits from storage.", opad) // extern
        } else {
            // Show queued retrofits
            val playerHulls: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(
                Global.getSector().playerFaction.id,
                FleetTypes.MERC_PRIVATEER, null
            )
            val retrofitHulls: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(
                faction.id,
                FleetTypes.MERC_PRIVATEER, null
            )
            for (t in queued) {
                playerHulls.fleetData.addFleetMember(t.ship)
                retrofitHulls.fleetData.addFleetMember(t.data.targetHull + "_Hull")
            }
            val playerMembers: List<FleetMemberAPI> = playerHulls.fleetData.membersListCopy
            val retrofitMembers: List<FleetMemberAPI> = retrofitHulls.fleetData.membersListCopy

            // Match names
            for (i in playerMembers.indices) {
                val name: String = playerMembers[i].shipName
                retrofitMembers[i].shipName = name
            }
            val members = ArrayList<FleetMemberAPI>()
            members.addAll(retrofitMembers)
            info.addPara("Queued Retrofits", opad)
            //            info.addTitle("Queued Retrofits");
            val rows = retrofitMembers.size / COLUMNS + 1
            val iconSize = width / COLUMNS
            val color: Color = faction.baseUIColor
            info.addShipList(COLUMNS, rows, iconSize, color, members, 0f)


            // Show time remaining for first hull
            val time = StringBuilder()
            val highlights: MutableList<String> = ArrayList()
            val first = queued[0]
            var firstSpec: ShipHullSpecAPI? = null
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (spec.hullId == first.data.targetHull) {
                    firstSpec = spec
                    break
                }
            }
            if (firstSpec == null) {
                info.addPara(
                    "Could not find ShipHullSpecAPI for" + first.data.targetHull,
                    opad,
                    Misc.getNegativeHighlightColor()
                )
                return
            }
            if (first.isPaused) {
                info.addPara(
                    "Retrofits paused because " + first.pauseReason + ".",
                    Misc.getNegativeHighlightColor(),
                    opad
                )
            } else {
                time.append(firstSpec.nameWithDesignationWithDashClass).append("\n")
                if (first.daysRemaining == 1) {
                    time.append("- Complete in %s day\n")
                } else {
                    time.append("- Complete in %s days\n")
                }
                highlights.add("" + first.daysRemaining)

                // Give projected completion date of all queued hulls
                if (queued.size > 1) {
                    var days = 0
                    for (tracker in queued) {
                        days += tracker.daysRemaining
                    }
                    if (days == 1) {
                        time.append("\nAll queued retrofits will be complete in %s day\n")
                    } else {
                        time.append("\nAll queued retrofits will be complete in %s days\n")
                    }
                    highlights.add("" + days)
                }
                info.addPara(time.toString(), opad, Misc.getHighlightColor(), "")
            }
        }
    }

    override fun getIntelTags(map: SectorMapAPI): Set<String> {
        val tags: MutableSet<String> = super.getIntelTags(map)
        tags.add(Tags.INTEL_FLEET_LOG)
        tags.add(faction.id)
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI): SectorEntityToken {
        return entity!!
    }

    override fun isHidden(): Boolean {
        return hidden != null
    }

    override fun notifyEnded() {
        val text = "The retrofitter at " + entity?.name + " has been destroyed."
        val message = MessageIntel(text)
        message.icon = Global.getSettings().getSpriteName("intel", "roider_retrofit")
        Global.getSector().campaignUI.addMessage(message)

        // Refund remaining retrofits???

//		Global.getSector().getIntelManager().removeIntel(this);
    }
}