package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.InteractionDialogImageVisual
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Highlights
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import exerelin.campaign.AllianceManager
import exerelin.campaign.PlayerFactionStore
import exerelin.utilities.NexUtilsFaction
import org.lwjgl.input.Keyboard
import roiderUnion.ModPlugin
import roiderUnion.retrofits.old.base.BaseRetrofitManager.RetrofitTracker
import java.awt.Color
import java.util.*


/**
 * Author: SafariJohn
 */
open class BaseRetrofitPlugin(
    originalPlugin: InteractionDialogPlugin?,
    manager: BaseRetrofitManager, memoryMap: Map<String, MemoryAPI>?,
) : InteractionDialogPlugin {
    enum class OptionId {
        PICK_TARGET, PICK_SHIPS, PRIORITIZE, CANCEL_SHIPS, LEAVE, CONFIRM, CANCEL
    }

    companion object {
        const val COLUMNS = 7
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "originalPlugin", "op")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "manager", "m")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "memoryMap", "mm")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "dialog", "d")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "text", "t")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "options", "o")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "visual", "v")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "retrofits", "r")
            x.aliasAttribute(BaseRetrofitPlugin::class.java, "selectedRetrofit", "s")
        }
    }

    protected val originalPlugin: InteractionDialogPlugin?
    protected val manager: BaseRetrofitManager
    private val memoryMap: Map<String, MemoryAPI>?
    protected var dialog: InteractionDialogAPI? = null
    protected var text: TextPanelAPI? = null
    protected var options: OptionPanelAPI? = null
    protected var visual: VisualPanelAPI? = null
    protected var retrofits: List<RetrofitData>
    protected var selectedRetrofit: FleetMemberAPI? = null

    init {
        this.originalPlugin = originalPlugin
        this.manager = manager
        this.memoryMap = memoryMap
        retrofits = manager.retrofits
    }

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        text = dialog.textPanel
        options = dialog.optionPanel
        visual = dialog.visualPanel
        updateText()
        updateOptions()
        updateVisual()
    }

    protected open fun updateText() {
        text!!.clear()

        // Update queued retrofits
        if (manager.queued.isNotEmpty()) {
            // Show time remaining for first hull
            val costs = StringBuilder()
            val highlights: MutableList<String> = ArrayList()
            val first: RetrofitTracker = manager.queued[0]
            var firstSpec: ShipHullSpecAPI? = null
            var firstTargetSpec: ShipHullSpecAPI? = null
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (matchesHullId(spec, first.data.sourceHull)) {
                    firstSpec = spec
                }
                if (spec.hullId == first.data.targetHull) {
                    firstTargetSpec = spec
                }
                if (firstSpec != null && firstTargetSpec != null) break
            }
            if (firstSpec == null || firstTargetSpec == null) {
                text!!.addPara(
                    "Could not find ShipHullSpecAPI for " + first.data.targetHull,
                    Misc.getNegativeHighlightColor()
                )
                return
            }
            costs.append(firstSpec.nameWithDesignationWithDashClass).append("\n")
            costs.append("to ").append(firstTargetSpec.nameWithDesignationWithDashClass).append("\n")
            if (first.daysRemaining == 1) {
                costs.append("- Complete in %s day\n")
            } else {
                costs.append("- Complete in %s days\n")
            }
            highlights.add(first.daysRemaining.toString())

            // Give projected completion date of all queued hulls
            if (manager.queued.size > 1) {
                var days = 0
                for (tracker in manager.queued) {
                    days += tracker.daysRemaining
                }

//                long timestamp = Global.getSector().getClock().getTimestamp();
//                timestamp += 1000 * Global.getSector().getClock().convertToSeconds(days);
//                CampaignClockAPI completeDate = Global.getSector().getClock().createClock(timestamp);
                if (days == 1) {
                    costs.append("\nAll queued retrofits will be complete in %s day\n")
                } else {
                    costs.append("\nAll queued retrofits will be complete in %s days\n")
                }
                highlights.add("" + days)
            }
            text!!.addPara(costs.toString(), Misc.getHighlightColor())
            val playerHulls: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(
                Global.getSector().playerFaction.id,
                FleetTypes.MERC_PRIVATEER, null
            )
            val retrofitHulls: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(
                manager.faction.id,
                FleetTypes.MERC_PRIVATEER, null
            )
            for (t in manager.queued) {
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
            val members: MutableList<FleetMemberAPI> = ArrayList<FleetMemberAPI>()
            members.addAll(retrofitMembers)
            val targetTooltip: TooltipMakerAPI = text!!.beginTooltip()
            targetTooltip.addTitle("Queued Retrofits")
            val rows = retrofitMembers.size / COLUMNS + 1
            val iconSize: Float = dialog!!.textWidth / COLUMNS
            val pad = 0f // 10f
            val color: Color = manager.faction.baseUIColor
            targetTooltip.addShipList(COLUMNS, rows, iconSize, color, members, pad)
            text!!.addTooltip()
        }

        // List targets if nothing queued
        if (selectedRetrofit == null) {
            text!!.addPara("Please select a hull to retrofit to.", Misc.getButtonTextColor()) // extern
            if (manager.queued.isEmpty()) {
                val playerFleet: CampaignFleetAPI = Global.getSector().playerFleet
                val playerHulls: MutableSet<String> = HashSet()
                for (m in playerFleet.membersWithFightersCopy) {
                    playerHulls.add(m.hullSpec.hullId)
                    playerHulls.add(m.hullSpec.baseHullId)
                }
                val targets: MutableList<String> = ArrayList()
                val specs = ArrayList<ShipHullSpecAPI>()
                val specsAvailable = ArrayList<ShipHullSpecAPI>()
                val specsIllegal = ArrayList<ShipHullSpecAPI>()
                val specsNotAllowed = ArrayList<ShipHullSpecAPI>()
                for (data in retrofits) {
                    if (targets.contains(data.targetHull)) continue
                    specs.add(Global.getSettings().getHullSpec(data.targetHull))
                    targets.add(data.targetHull)
                }

                // Find which targets the player has source ships for
                // If player has at least one source hull for target, include spec in allowed hulls
                targets.clear()
                for (data in retrofits) {
                    if (targets.contains(data.targetHull)) continue
                    if (playerHulls.contains(data.sourceHull)) {
                        specsAvailable.add(Global.getSettings().getHullSpec(data.targetHull))
                        targets.add(data.targetHull)
                    }
                }

                // Find which targets are illegal for the player
                // or not allowed
                for (spec in specs) {
                    // Temp retrofit
                    selectedRetrofit = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, spec.hullId + "_Hull"
                    )
                    if (!isAllowed) {
                        specsNotAllowed.add(spec)
                    } else if (isIllegal) {
                        specsIllegal.add(spec)
                    }

                    // Remove temp retrofit
                    selectedRetrofit = null
                }
                specs.removeAll(specsAvailable.toSet())
                specs.removeAll(specsIllegal.toSet())
                specs.removeAll(specsNotAllowed.toSet())
                specsAvailable.removeAll(specsIllegal.toSet())
                specsAvailable.removeAll(specsNotAllowed.toSet())
                sortShipSpecs(specsAvailable)
                sortShipSpecs(specs)
                sortShipSpecs(specsIllegal)
                sortShipSpecs(specsNotAllowed)
                val available = StringBuilder()
                var first = true
                for (spec in specsAvailable) {
                    if (first) first = false else available.append("\n")
                    available.append(spec.nameWithDesignationWithDashClass)
                }
                if (available.isNotEmpty()) showCatalog(
                    "Available Retrofits",
                    specsAvailable,
                    manager.faction.baseUIColor
                )
                //                if (available.length() > 0) text!!.addPara(available.toString());
                val unavailable = StringBuilder()
                first = true
                for (spec in specs) {
                    if (first) first = false else unavailable.append("\n")
                    unavailable.append(spec.nameWithDesignationWithDashClass)
                }
                if (unavailable.isNotEmpty()) showCatalog("Unavailable Retrofits", specs, Misc.getGrayColor())
                //                if (unavailable.length() > 0) text!!.addPara(unavailable.toString(), Misc.getGrayColor());
                val illegal = StringBuilder()
                first = true
                for (spec in specsIllegal) {
                    if (first) first = false else illegal.append("\n")
                    illegal.append(spec.nameWithDesignationWithDashClass)
                }
                if (illegal.isNotEmpty()) showCatalog("Illegal Retrofits", specsIllegal, Misc.getNegativeHighlightColor())
                //                if (illegal.length() > 0) text!!.addPara(illegal.toString(), Misc.getNegativeHighlightColor());
                val notAllowed = StringBuilder()
                first = true
                for (spec in specsNotAllowed) {
                    if (first) first = false else notAllowed.append("\n")
                    notAllowed.append(spec.nameWithDesignationWithDashClass)
                }
                if (notAllowed.isNotEmpty()) showCatalog(
                    notAllowedRetrofitsTitle,
                    specsNotAllowed,
                    Misc.getNegativeHighlightColor()
                )
                //                if (illegal.length() > 0) text!!.addPara(illegal.toString(), Misc.getNegativeHighlightColor());
            }
        } else {
            text!!.addPara(
                "Retrofitting to: " + selectedRetrofit!!.variant
                    .fullDesignationWithHullName, Misc.getButtonTextColor()
            )
            val costs = StringBuilder()
            val highlights: MutableList<String> = ArrayList()
            val illegalCosts = StringBuilder()
            var firstIllegal = true
            var firstLegal = true
            val matched: MutableList<String> = ArrayList()

            // Check for specific matches
            for (data in retrofits) {
                if (data.targetHull != selectedRetrofit!!.hullId) continue
                for (spec in Global.getSettings().allShipHullSpecs) {
                    if (matched.contains(spec.hullId)) continue
                    if (spec.isDHull && !matchesHullId(spec.hullId, data.sourceHull)) continue
                    if (spec.hullId == selectedRetrofit!!.hullId) continue
                    if (matchesHullId(spec.hullId, data.sourceHull)) {
                        if (!isAllowed(data.sourceHull)) {
                            if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                            illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                            appendIllegalsCosts(illegalCosts, data.cost.toInt(), data.time.toInt())
                            illegalCosts.append(getNotAllowedRetrofitText(data.sourceHull))
                        } else if (isIllegal(data.sourceHull)) {
                            if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                            illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                            appendIllegalsCosts(illegalCosts, data.cost.toInt(), data.time.toInt())
                            illegalCosts.append(getIllegalRetrofitText(data.sourceHull))
                        } else {
                            if (firstLegal) firstLegal = false else costs.append("\n")
                            costs.append(spec.nameWithDesignationWithDashClass).append("\n")
                            appendLegalCosts(costs, highlights, data.cost.toInt(), data.time.toInt())
                        }
                        matched.add(spec.hullId)
                        matched.add(spec.hullName)
                        break
                    }
                }
            }

            // Check for general matches
            for (data in retrofits) {
                if (data.targetHull != selectedRetrofit!!.hullId) continue
                for (spec in Global.getSettings().allShipHullSpecs) {
                    if (matched.contains(spec.hullId)) continue
                    if (matched.contains(spec.hullName)) continue
                    if (spec.isDHull && !matchesHullId(spec.hullId, data.sourceHull)) continue
                    if (spec.hullId == selectedRetrofit!!.hullId) continue
                    if (matchesHullId(spec, data.sourceHull)) {
                        if (!isAllowed(data.sourceHull)) {
                            if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                            illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                            appendIllegalsCosts(illegalCosts, data.cost.toInt(), data.time.toInt())
                            illegalCosts.append(getNotAllowedRetrofitText(data.sourceHull))
                        } else if (isIllegal(data.sourceHull)) {
                            if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                            illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                            appendIllegalsCosts(illegalCosts, data.cost.toInt(), data.time.toInt())
                            illegalCosts.append(getIllegalRetrofitText(data.sourceHull))
                        } else {
                            if (firstLegal) firstLegal = false else costs.append("\n")
                            costs.append(spec.nameWithDesignationWithDashClass).append("\n")
                            appendLegalCosts(costs, highlights, data.cost.toInt(), data.time.toInt())
                        }
                    }
                }
            }
            if (costs.isNotEmpty()) text!!.addPara(
                costs.toString(),
                Misc.getHighlightColor()
            )
            if (illegalCosts.isNotEmpty()) text!!.addPara(
                illegalCosts.toString(),
                Misc.getNegativeHighlightColor(), illegalCosts.toString()
            )
        }
    }

    private fun showCatalog(name: String?, specs: List<ShipHullSpecAPI>, color: Color?) {
        val retrofitHulls: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(
            manager.faction.id,
            FleetTypes.MERC_PRIVATEER, null
        )
        val included: MutableList<ShipHullSpecAPI> = ArrayList<ShipHullSpecAPI>()
        for (spec in specs) {
            if (included.contains(spec)) continue
            retrofitHulls.fleetData.addFleetMember(spec.hullId + "_Hull") // extern
            included.add(spec)
        }
        val retrofitMembers: List<FleetMemberAPI> = retrofitHulls.fleetData.membersListCopy

        // Match names
        for (m in retrofitMembers) {
            m.shipName = "Retrofit to"
            m.repairTracker.cr = 0.7f // Looks cleaner
        }
        val members: MutableList<FleetMemberAPI> = ArrayList<FleetMemberAPI>()
        members.addAll(retrofitMembers)
        val targetTooltip: TooltipMakerAPI = dialog!!.textPanel.beginTooltip()
        targetTooltip.addTitle(name)
        val rows = 1 + (retrofitMembers.size - 1) / COLUMNS
        val iconSize: Float = dialog!!.textWidth / COLUMNS
        val pad = 0f // 10f
        //        Color color = manager.faction.getBaseUIColor();
        targetTooltip.addShipList(COLUMNS, rows, iconSize, color, members, pad)
        text!!.addTooltip()
    }

    private fun appendIllegalsCosts(builder: StringBuilder, cost: Int, time: Int) {
        if (cost > 0 && time > 0) {
            builder.append("- Costs ").append(Misc.getDGSCredits(cost.toFloat()))
                .append(" and takes ").append(time).append(" days\n")
        } else if (cost < 0 && time > 0) {
            builder.append("- Pays ").append(Misc.getDGSCredits(cost.toFloat()))
                .append(" and takes ").append(time).append(" days\n")
        } else if (cost > 0) {
            builder.append("- Costs ").append(Misc.getDGSCredits(cost.toFloat())).append("\n")
        } else if (cost < 0) {
            builder.append("- Pays ").append(Misc.getDGSCredits(cost.toFloat())).append("\n") // extern
        } else if (time > 0) {
            builder.append("- Takes ").append(time).append(" days\n")
        } else {
            builder.append("- Free\n")
        }
    }

    private fun appendLegalCosts(costs: StringBuilder, highlights: MutableList<String>, cost: Int, time: Int) {
        if (cost > 0 && time > 0) {
            costs.append("- Costs %s and takes %s days")
            highlights.add(Misc.getDGSCredits(cost.toFloat()))
            highlights.add("" + time)
        } else if (cost < 0 && time > 0) {
            costs.append("- Pays %s and takes %s days")
            highlights.add(Misc.getDGSCredits(cost.toFloat()))
            highlights.add("" + time)
        } else if (cost > 0) {
            costs.append("- Costs %s")
            highlights.add(Misc.getDGSCredits(cost.toFloat()))
        } else if (cost < 0) {
            costs.append("- Pays %s")
            highlights.add(Misc.getDGSCredits(cost.toFloat()))
        } else if (time > 0) {
            costs.append("- Takes %s days") // extern
            highlights.add("" + time)
        } else {
            costs.append("- %s")
            highlights.add("Free")
        }
    }

    private fun sortShipSpecs(specs: MutableList<ShipHullSpecAPI>?) {
        // Sort by name
        specs!!.sortWith { o1, o2 ->
            o1.nameWithDesignationWithDashClass.compareTo(o2.nameWithDesignationWithDashClass)
        }


        // Sort by size
        specs.sortWith { o1, o2 ->
            val size1: HullSize = o1.hullSize
            val size2: HullSize = o2.hullSize
            size1.compareTo(size2)
        }
    }

    protected open fun updateOptions() {
        options!!.clearOptions()
        options!!.addOption("Pick retrofit hull", OptionId.PICK_TARGET)
        var queue = "Retrofit ships"
        if (manager.queued.isNotEmpty()) queue = "Queue retrofits" // extern
        options!!.addOption(queue, OptionId.PICK_SHIPS)
        if (manager.queued.isNotEmpty()) {
            options!!.addOption("Prioritize retrofit", OptionId.PRIORITIZE)
            options!!.addOption("Cancel retrofits", OptionId.CANCEL_SHIPS)
        }
        options!!.addOption(leaveOptionText, OptionId.LEAVE)
        options!!.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true)
        if (retrofits.isEmpty()) {
            // Should never happen normally.
            options!!.setEnabled(OptionId.PICK_TARGET, false)
            options!!.setTooltip(OptionId.PICK_TARGET, "No possible retrofits!")
        }
        if (selectedRetrofit == null) {
            options!!.setTooltip(OptionId.PICK_SHIPS, "Please select a hull to retrofit to.") // extern
            options!!.setEnabled(OptionId.PICK_SHIPS, false)
        } else {
            if (!isAllowed) {
                options!!.setTooltip(OptionId.PICK_SHIPS, getNotAllowedRetrofitText(null))
                options!!.setTooltipHighlightColors(
                    OptionId.PICK_SHIPS,
                    Misc.getNegativeHighlightColor(),
                    Misc.getNegativeHighlightColor()
                )
                options!!.setTooltipHighlights(OptionId.PICK_SHIPS, *getNotAllowedRetrofitTextHighlights(null).text)
                options!!.setEnabled(OptionId.PICK_SHIPS, false)
            } else if (isIllegal) { // Check if retrofit is blocked by reputation or commission
                options!!.setTooltip(OptionId.PICK_SHIPS, getIllegalRetrofitText(null))
                options!!.setTooltipHighlightColors(
                    OptionId.PICK_SHIPS,
                    Misc.getNegativeHighlightColor(),
                    Misc.getNegativeHighlightColor()
                )
                options!!.setTooltipHighlights(OptionId.PICK_SHIPS, *getIllegalRetrofitTextHighlights(null)?.text)
                options!!.setEnabled(OptionId.PICK_SHIPS, false)
            } else  // and that there are ships available
                if (availableShips.isEmpty()) {
                    options!!.setTooltip(
                        OptionId.PICK_SHIPS,
                        "You have no ships that can retrofit to " + selectedRetrofit!!.hullSpec // extern
                            .hullNameWithDashClass + "."
                    )
                    options!!.setEnabled(OptionId.PICK_SHIPS, false)
                } else {
                    options!!.setEnabled(OptionId.PICK_SHIPS, true)
                }
        }
        val queueEmpty: Boolean = manager.queued.isNotEmpty()
        options!!.setEnabled(OptionId.PRIORITIZE, queueEmpty)
        options!!.setEnabled(OptionId.CANCEL_SHIPS, queueEmpty)
    }

    protected open val leaveOptionText: String?
        get() = "Leave" // extern

    /**
     * Checks for selectedRetrofit.
     * @return
     */
    val isIllegal: Boolean
        get() {
            val req: RepLevel = requiredLevel
            val level: RepLevel =
                manager.faction.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER))
            var legal: Boolean = level.isAtWorst(req)
            if (requiresCommission()) {
                legal = legal and hasCommission()
            }
            return !legal
        }

    /**
     * @param sourceHull
     * @return
     */
    protected fun isIllegal(sourceHull: String?): Boolean {
        val req: RepLevel = getRequiredLevel(sourceHull) ?: return false
        val level: RepLevel = manager.faction.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER))
        var legal: Boolean = level.isAtWorst(req)
        if (requiresCommission(sourceHull)) {
            legal = legal and hasCommission()
        }
        return !legal
    }

    protected fun getIllegalRetrofitText(hullId: String?): String? {
        val req: RepLevel = hullId?.let { getRequiredLevel(it) } ?: requiredLevel
        if (req != null) {
            var str = ""
            val level: RepLevel =
                manager.faction.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER))
            if (hullId == null) {
                if (!level.isAtWorst(req)) {
                    str += "Req: " + manager.faction.displayName + " - " + req.displayName
                        .lowercase(Locale.getDefault())
                }
                if (requiresCommission() && !hasCommission()) {
                    if (str.isNotEmpty()) str += "\n"
                    str += "Req: " + manager.faction.displayName + " - " + "commission"
                }
            } else {
                if (!level.isAtWorst(req)) {
                    str += "- Req: " + manager.faction.displayName + " - " + req.displayName
                        .lowercase(Locale.getDefault())
                }
                if (requiresCommission(hullId) && !hasCommission()) {
                    if (str.isNotEmpty()) str += "\n"
                    str += "- Req: " + manager.faction.displayName + " - " + "commission"
                }
            }
            return str
        }
        return null
    }

    protected fun getIllegalRetrofitTextHighlights(hullId: String?): Highlights? {
        val req: RepLevel = hullId?.let { getRequiredLevel(it) } ?: requiredLevel

        if (req != null) {
            val c: Color = Misc.getNegativeHighlightColor()
            val h = Highlights()
            val level: RepLevel =
                manager.faction.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER))
            if (hullId == null) {
                if (!level.isAtWorst(req)) {
                    h.append(
                        "Req: " + manager.faction.displayName + " - " + req.displayName
                            .lowercase(Locale.getDefault()), c
                    )
                }
                if (requiresCommission() && !hasCommission()) {
                    h.append("Req: " + manager.faction.displayName + " - commission", c)
                }
            } else {
                if (!level.isAtWorst(req)) {
                    h.append(
                        "- Req: " + manager.faction.displayName + " - " + req.displayName
                            .lowercase(Locale.getDefault()), c
                    )
                }
                if (requiresCommission(hullId) && !hasCommission()) {
                    h.append("- Req: " + manager.faction.displayName + " - commission", c)
                }
            }
            return h
        }
        return null
    }

    /**
     * @return The lowest RepLevel of all the source hulls.
     */
    private val requiredLevel: RepLevel
        get() {
            var lowest: RepLevel = RepLevel.COOPERATIVE
            for (data in retrofits) {
                if (data.targetHull != selectedRetrofit!!.hullId) continue
                var rMin: Float = data.reputation.min
                if (data.reputation.isNegative) rMin = -rMin
                val lMin: Float = lowest.min
                if (lowest.isNegative) rMin = -lMin
                if (rMin < lMin) lowest = data.reputation
            }
            return lowest
        }

    private fun getRequiredLevel(sourceHull: String?): RepLevel {
        for (data in retrofits) {
            if (data.targetHull == selectedRetrofit!!.hullId && matchesHullId(sourceHull!!, data.sourceHull)) {
                return data.reputation
            }
        }
        return RepLevel.FAVORABLE
    }

    /**
     * @return False if even one source hull doesn't require a commission to convert.
     */
    private fun requiresCommission(): Boolean {
        for (data in retrofits) {
            if (data.targetHull != selectedRetrofit!!.hullId) continue
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (matchesHullId(spec, data.sourceHull) && !data.commission) {
                    return false
                }
            }
        }
        return true
    }

    private fun requiresCommission(sourceHull: String?): Boolean {
        for (data in retrofits) {
            if (data.targetHull == selectedRetrofit?.hullId && matchesHullId(sourceHull!!, data.sourceHull)) {
                return data.commission
            }
        }
        return true
    }

    private fun hasCommission(): Boolean {
        if (ModPlugin.hasNexerelin) {
            val commissionFaction = NexUtilsFaction.getCommissionFactionId()
            if (commissionFaction != null && AllianceManager.areFactionsAllied(
                    commissionFaction,
                    manager.faction.id
                )
            ) {
                return true
            }
            if (AllianceManager.areFactionsAllied(
                    PlayerFactionStore.getPlayerFactionId(),
                    manager.faction.id
                )
            ) {
                return true
            }
        }
        return manager.faction.id.equals(Misc.getCommissionFactionId())
    }

    protected open val isAllowed: Boolean
        get() = true

    protected open fun isAllowed(sourceHull: String): Boolean {
        return true
    }

    protected open fun getNotAllowedRetrofitText(hullId: String?): String? {
        return ""
    }

    protected open fun getNotAllowedRetrofitTextHighlights(hullId: String?): Highlights {
        return Highlights()
    }

    protected open val notAllowedRetrofitsTitle: String?
        get() = "Not Allowed Retrofits" // extern

    protected open fun updateVisual() {
        visual!!.fadeVisualOut()
        if (selectedRetrofit == null) {
            visual!!.showImageVisual(InteractionDialogImageVisual(
                "illustrations",
                "orbital_construction", // extern
                640f,
                400f))
        } else {
            visual!!.showFleetMemberInfo(selectedRetrofit)
        }
    }

    protected fun matchesHullId(hull: ShipHullSpecAPI, source: String?): Boolean {
        return matchesHullId(hull.hullId, source!!) || matchesHullId(hull.baseHullId, source)
    }

    protected fun matchesHullId(hull: String, source: String): Boolean {
        return hull == source
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (OptionId.PICK_TARGET == optionData) pickTarget()
        else if (OptionId.PICK_SHIPS == optionData) pickShips()
        else if (OptionId.PRIORITIZE == optionData) prioritize()
        else if (OptionId.CANCEL_SHIPS == optionData) cancelShips()
        else if (optionData is List<*>) {
            confirmRetrofits(optionData as List<FleetMemberAPI>)
        } else if (OptionId.CANCEL == optionData) {
            updateText()
            updateOptions()
        } else {
            dialog!!.dismiss()
        }
    }

    protected fun pickTarget() {
        val targetIds: MutableList<String> = ArrayList()
        val blockedIds: MutableList<String> = ArrayList()
        val rep: RepLevel = manager.faction.getRelationshipLevel(Factions.PLAYER)
        val commissioned = Misc.getCommissionFaction() === manager.faction
        for (data in retrofits) {
            val repPass: Boolean = rep.isAtWorst(data.reputation)
            val comPass = !data.commission || data.commission && commissioned
            if (targetIds.contains(data.targetHull)) {
                if (repPass && comPass) blockedIds.remove(data.targetHull)
                continue
            }
            var foundTarget = false
            var foundSource = false
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (spec.hullId == data.targetHull) {
                    foundTarget = true
                }
                if (matchesHullId(spec, data.sourceHull)) {
                    foundSource = true
                }
                if (foundTarget && foundSource) {
                    targetIds.add(data.targetHull)
                    if (!repPass || !comPass) {
                        blockedIds.add(data.targetHull)
                    }
                    break
                }
            }
        }
        val pool: CampaignFleetAPI =
            FleetFactoryV3.createEmptyFleet(manager.faction.id, FleetTypes.MERC_PRIVATEER, null)
        for (id in targetIds) {
            pool.fleetData.addFleetMember(id + "_Hull")
        }
        for (m in pool.fleetData.membersListCopy) {
            m.shipName = "Retrofit to"
            if (blockedIds.contains(m.hullId)) {
                m.repairTracker.isMothballed = true
                m.repairTracker.cr = 0f
            } else {
                m.repairTracker.cr = m.repairTracker.maxCR // Looks cleaner
            }
        }
        pool.fleetData.sort()
        val targets: List<FleetMemberAPI> = pool.fleetData.membersListCopy
        val rows = targets.size / 7 + 1

        // Calculate rows, columns, and iconSize?
        dialog!!.showFleetMemberPickerDialog(
            "Pick retrofit hull",
            "Ok",
            "Cancel",
            rows,
            COLUMNS,
            58f,
            true,
            false,
            targets,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    selectedRetrofit = if (members.isEmpty()) {
                        null
                    } else {
                        members[0]
                    }
                    updateText()
                    updateOptions()
                    updateVisual()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    protected fun pickShips() {
        val avail: List<FleetMemberAPI> = availableShips
        val rows = avail.size / 7 + 1
        dialog!!.showFleetMemberPickerDialog(
            "Pick ships to retrofit",
            "Ok",
            "Cancel",
            rows,
            COLUMNS,
            58f,
            true,
            true,
            avail,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    if (members.isEmpty()) return

                    // Tally cost
                    var tCost = 0.0
                    for (ship in members) {
                        var data: RetrofitData? = null
                        // Check for exact match
                        for (d in retrofits) {
                            if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                                    ship.hullSpec.hullId, d.sourceHull
                                )
                            ) {
                                data = d
                                break
                            }
                        }
                        if (data != null) {
                            tCost += data.cost
                            continue
                        }

                        // Check for general match
                        for (d in retrofits) {
                            if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                                    ship.hullSpec,
                                    d.sourceHull
                                )
                            ) {
                                data = d
                                break
                            }
                        }
                        if (data != null) {
                            tCost += data.cost
                        }
                    }


                    // Update UI
                    if (tCost != 0.0) showRetrofitConfirmDialog(members) else confirmRetrofits(members)
                    //                updateText();
//                updateOptions();
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    protected open fun showRetrofitConfirmDialog(members: List<FleetMemberAPI>) {
        if (members.isEmpty()) return

        // Tally cost
        var tCost = 0.0
        for (ship in members) {
            var data: RetrofitData? = null
            // Check for exact match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                        ship.hullSpec.hullId,
                        d.sourceHull
                    )
                ) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
                continue
            }

            // Check for general match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(ship.hullSpec, d.sourceHull)) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
            }
        }

        // Update text
        text!!.clear()
        val targetTooltip: TooltipMakerAPI = text!!.beginTooltip()
        targetTooltip.addTitle("Confirm Retrofits")
        val rows = members.size / COLUMNS + 1
        val iconSize: Float = dialog!!.textWidth / COLUMNS
        val pad = 0f // 10f
        val color: Color = manager.faction.baseUIColor
        targetTooltip.addShipList(COLUMNS, rows, iconSize, color, members, pad)
        text!!.addTooltip()
        var message = "It is free "
        if (tCost > 0) {
            message = "You must pay " + Misc.getDGSCredits(tCost.toFloat())
        } else if (tCost < 0) {
            message = "You will receive " + Misc.getDGSCredits(-tCost.toFloat())
        }
        message += " to convert"
        message += if (members.size == 1) " this ship. The new hull will be pristine." else " these ships. The new hulls will be pristine."
        text!!.addPara(message, Misc.getHighlightColor(), Misc.getDGSCredits(tCost.toFloat()))

        // Update options
        options!!.clearOptions()
        var confirmText = "Confirm"
        if (tCost > 0) {
            confirmText = "Pay " + Misc.getDGSCredits(tCost.toFloat()) + " credits"
        } else if (tCost < 0) {
            tCost = -tCost
            confirmText = "Receive " + Misc.getDGSCredits(tCost.toFloat()) + " credits"
        }
        options!!.addOption(confirmText, members)
        if (tCost > Global.getSector().playerFleet.cargo.credits.get()) {
            options!!.setEnabled(members, false)
            options!!.setTooltip(members, "You do not have enough credits!")
            options!!.setTooltipHighlights(members, "You do not have enough credits!")
            options!!.setTooltipHighlightColors(members, Misc.getNegativeHighlightColor())
        }
        options!!.addOption("Cancel", OptionId.CANCEL)
        options!!.setShortcut(OptionId.CANCEL, Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    protected open fun confirmRetrofits(members: List<FleetMemberAPI>) {
        // Tally cost
        var tCost = 0.0
        for (ship in members) {
            var data: RetrofitData? = null
            // Check for exact match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                        ship.hullSpec.hullId,
                        d.sourceHull
                    )
                ) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
                continue
            }

            // Check for general match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(ship.hullSpec, d.sourceHull)) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
            }
        }

        // Charge player
        if (tCost > 0) {
            Global.getSector().playerFleet.cargo.credits.subtract(tCost.toFloat())
            Global.getSector().campaignUI.messageDisplay.addMessage("Paid $tCost for retrofits")
        }
        // Pay player
        if (tCost < 0) {
            tCost = -tCost
            Global.getSector().playerFleet.cargo.credits.add(tCost.toFloat())
            Global.getSector().campaignUI.messageDisplay.addMessage("Received $tCost for retrofits")
        }

        // Strip ships
        val playerCargo: CargoAPI = Global.getSector().playerFleet.cargo
        var stripped = false
        for (ship in members) {
            for (slot in ship.variant.nonBuiltInWeaponSlots) {
                if (ship.variant.getWeaponId(slot) == null || ship.variant.getWeaponId(slot)
                        .isEmpty()
                ) continue
                playerCargo.addWeapons(ship.variant.getWeaponId(slot), 1)
                stripped = true
                ship.variant.clearSlot(slot)
            }
            for (wing in ship.variant.fittedWings) {
                playerCargo.addFighters(wing, 1)
                stripped = true
            }
            ship.variant.fittedWings.clear()
        }
        if (stripped) Global.getSector().campaignUI.messageDisplay.addMessage("Stripped weapons and fighters to cargo")

        // Remove from player's fleet
        val playerFleet: CampaignFleetAPI = Global.getSector().playerFleet
        for (ship in members) {
            playerFleet.fleetData.removeFleetMember(ship)
        }

        // Add to queue
        for (ship in members) {
            var data: RetrofitData? = null
            // Check for exact match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                        ship.hullSpec.hullId,
                        d.sourceHull
                    )
                ) {
                    data = d
                    break
                }
            }
            if (data != null) {
                manager.addToQueue(RetrofitTracker(ship, data, data.cost))
                continue
            }

            // Check for general match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(ship.hullSpec, d.sourceHull)) {
                    data = d
                    break
                }
            }
            if (data != null) {
                manager.addToQueue(RetrofitTracker(ship, data, data.cost))
            }
        }
        updateText()
        updateOptions()
    }

    protected val availableShips: List<FleetMemberAPI>
        get() {
            val sourceIds: MutableList<String> = ArrayList()
            for (data in retrofits) {
                if (data.targetHull == selectedRetrofit?.hullId && !isIllegal(data.sourceHull)
                    && isAllowed(data.sourceHull)
                ) {
                    sourceIds.add(data.sourceHull)
                }
            }
            val pool: CampaignFleetAPI =
                FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null)
            for (m in Global.getSector().playerFleet.fleetData.membersListCopy) {
                if (m.hullId == selectedRetrofit?.hullId) continue
                for (id in sourceIds) {
                    if (matchesHullId(m.hullSpec, id)) {
                        pool.fleetData.addFleetMember(m)
                        break
                    }
                }
            }
            return pool.fleetData.membersListCopy
        }

    protected fun prioritize() {
        val pool: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null)
        for (tracker in manager.queued) {
            val ship: FleetMemberAPI = tracker.ship
            pool.fleetData.addFleetMember(ship)
        }
        val targets: List<FleetMemberAPI> = pool.fleetData.membersListCopy
        val rows = targets.size / 7 + 1

        // Calculate rows, columns, and iconSize?
        dialog!!.showFleetMemberPickerDialog(
            "Prioritize retrofit",
            "Ok",
            "Cancel",
            rows,
            COLUMNS,
            58f,
            true,
            false,
            targets,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    // Put selected at the front of the queue
                    for (tracker in manager.queued) {
                        if (members.contains(tracker.ship)) {
                            manager.queued.remove(tracker)
                            manager.queued.add(0, tracker)
                            break
                        }
                    }

                    // Update UI
                    updateText()
                    updateOptions()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    protected fun cancelShips() {
        val pool: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null)
        for (tracker in manager.queued) {
            val ship: FleetMemberAPI = tracker.ship
            pool.fleetData.addFleetMember(ship)
        }
        val targets: List<FleetMemberAPI> = pool.fleetData.membersListCopy
        val rows = targets.size / 7 + 1

        // Calculate rows, columns, and iconSize?
        dialog!!.showFleetMemberPickerDialog(
            "Cancel retrofits",
            "Ok",
            "Cancel",
            rows,
            COLUMNS,
            58f,
            true,
            true,
            targets,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    if (members.isEmpty()) return

                    // Tally refund
                    var tRefund = 0.0

                    // Remove from queue
                    val remove: MutableList<RetrofitTracker> = ArrayList<RetrofitTracker>()
                    for (tracker in manager.queued) {
                        if (members.contains(tracker.ship)) {
                            remove.add(tracker)
                            tRefund += tracker.cost
                        }
                    }
                    manager.queued.removeAll(remove.toSet())

                    // Refund player
                    if (tRefund > 0) {
                        Global.getSector().playerFleet.cargo.credits.add(tRefund.toFloat())
                        Global.getSector().campaignUI.messageDisplay.addMessage("Refunded $tRefund for cancelled retrofits")
                    }

                    // Add to player's fleet
                    val playerFleet: CampaignFleetAPI = Global.getSector().playerFleet
                    for (ship in members) {
                        playerFleet.fleetData.addFleetMember(ship)
                    }

                    // Update UI
                    updateText()
                    updateOptions()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    override fun getMemoryMap(): Map<String, MemoryAPI>? {
        return memoryMap
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {}
    override fun advance(amount: Float) {}
    override fun backFromEngagement(battleResult: EngagementResultAPI?) {}
    override fun getContext(): Any? {
        return null
    }
}