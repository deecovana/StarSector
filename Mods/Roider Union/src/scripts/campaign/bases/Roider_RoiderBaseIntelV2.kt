package scripts.campaign.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.EconomyAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.DebugFlags
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.procgen.MarkovNames
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.*
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import org.json.JSONException
import org.json.JSONObject
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIndustries
import java.awt.Color
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 * Differs from base by allowing bounties to be put on it
 */
class Roider_RoiderBaseIntelV2(
    system: StarSystemAPI,
    factionId: String?, tier: RoiderBaseTier
) : BaseIntelPlugin(), EveryFrameScript, FleetEventListener, EconomyAPI.EconomyUpdateListener {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "system", "s")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "market", "m")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "entity", "e")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "elapsedDays", "ed")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "duration", "d")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "bountyData", "b")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "tier", "t")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "matchedStationToTier", "mt")
            x.aliasAttribute(Roider_RoiderBaseIntelV2::class.java, "monthlyInterval", "i")
            x.alias("roider_baseBounty", BaseBountyData::class.java)
            BaseBountyData.aliasAttributes(x)
        }

        const val MARKET_PREFIX = "roider_fringeIndieBase_"
        var BOUNTY_EXPIRED_PARAM = Any()
        var DISCOVERED_PARAM = Any()
        var log = Global.getLogger(Roider_RoiderBaseIntelV2::class.java)
    }

    enum class RoiderBaseTier {
        TIER_1_1MODULE, TIER_2_1MODULE, TIER_3_2MODULE, TIER_4_3MODULE, TIER_5_3MODULE
    }

    class BaseBountyData {
        var bountyElapsedDays = 0f
        var bountyDuration = 0f
        var baseBounty = 0f
        var repChange = 0f
        var bountyFaction: FactionAPI? = null

        companion object {
            fun aliasAttributes(x: XStream) {
                x.aliasAttribute(BaseBountyData::class.java, "bountyElapsedDays", "e")
                x.aliasAttribute(BaseBountyData::class.java, "bountyDuration", "d")
                x.aliasAttribute(BaseBountyData::class.java, "baseBounty", "b")
                x.aliasAttribute(BaseBountyData::class.java, "repChange", "r")
                x.aliasAttribute(BaseBountyData::class.java, "bountyFaction", "f")
            }
        }
    }

    private var system: String
    private var market: MarketAPI
    private var entity: SectorEntityToken?
    private var elapsedDays = 0f
    private var duration = 45f
    private var bountyData: BaseBountyData? = null
    private var tier: RoiderBaseTier
    private var matchedStationToTier: RoiderBaseTier? = null
    private var monthlyInterval = IntervalUtil(20f, 40f)
    fun init() {
        if (isDone) return
        Global.getSector().economy.addMarket(market, true)
        log.info(String.format("Added indie roider base in [%s], tier: %s", getSystem().name, tier.name))
        Global.getSector().intelManager.addIntel(this, true)
        timestamp = null
        Global.getSector().listenerManager.addListener(this)
        Global.getSector().economy.addUpdateListener(this)
    }

    private var monthsAtCurrentTier = 0

    init {
        this.system = system.id
        this.tier = tier
        market = Global.getFactory().createMarket(MARKET_PREFIX + Misc.genUID(), "Roider Base", 3)
        market.size = 3
        market.isHidden = true
        market.surveyLevel = MarketAPI.SurveyLevel.FULL
        market.factionId = factionId
        market.addCondition(Conditions.POPULATION_3)
        market.addIndustry(Industries.POPULATION)
        market.addIndustry(Industries.SPACEPORT)
        market.addIndustry(Industries.HEAVYBATTERIES)
        market.addIndustry(Industries.PATROLHQ)
        market.addIndustry(Industries.REFINING)
        market.addIndustry(RoiderIndustries.DIVES)
//        market.addSubmarket(RoiderIds.Roider_Submarkets.FRINGE_OPEN_MARKET)
//        market.addSubmarket(RoiderIds.Roider_Submarkets.FRINGE_BLACK_MARKET)
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE)
        market.tariff.modifyFlat("default_tariff", market.faction.tariffFraction)
        val weights: LinkedHashMap<LocationType, Float> = LinkedHashMap<LocationType, Float>()
        weights[LocationType.IN_ASTEROID_BELT] = 10f
        weights[LocationType.IN_ASTEROID_FIELD] = 10f
        weights[LocationType.IN_RING] = 10f
        weights[LocationType.IN_SMALL_NEBULA] = 10f
        weights[LocationType.GAS_GIANT_ORBIT] = 10f
        weights[LocationType.PLANET_ORBIT] = 10f
        weights[LocationType.STAR_ORBIT] = 1f
        val locs: WeightedRandomPicker<EntityLocation> =
            getLocations(null, system, null, 100f, weights)
        val loc = locs.pick()
        if (loc == null) {
            endImmediately()
            entity = null
        } else {
            val added = addNonSalvageEntity(system, loc, Entities.MAKESHIFT_STATION, factionId)
            if (added?.entity == null) {
                endImmediately()
                entity = null
            } else {
                entity = added.entity
                val name = generateName()
                if (name == null) {
                    endImmediately()
                } else {
                    market.name = name
                    entity!!.name = name


//		boolean down = false;
//		if (entity.getOrbitFocus() instanceof PlanetAPI) {
//			PlanetAPI planet = (PlanetAPI) entity.getOrbitFocus();
//			if (!planet.isStar()) {
//				down = true;
//			}
//		}
//		if (down) {
//			convertOrbitPointingDown(entity);
//		}
                    convertOrbitWithSpin(entity, -5f)
                    market.primaryEntity = entity
                    entity!!.market = market
                    entity!!.sensorProfile = 1f
                    entity!!.isDiscoverable = true
                    entity!!.detectedRangeMod.modifyFlat("gen", 5000f)
                    market.econGroup = market.id
                    market.memoryWithoutUpdate.set(DecivTracker.NO_DECIV_KEY, true)
                    market.reapplyIndustries()
                    val dives = market.getIndustry(RoiderIndustries.DIVES)
//                    if (dives is DivesController && !dives.canMine()) {
//                        endImmediately()
//                    } else {
//                        PortsideBarData.getInstance().addEvent(Roider_RoiderBaseRumorBarEvent(this))
//                    }
                }
            }
        }
    }

    //if (true) return false;
    override fun isHidden(): Boolean {
        return if (super.isHidden()) true else timestamp == null
    }

    //if (true) return false;
    fun getSystem(): StarSystemAPI {
        return Global.getSector().getStarSystem(system)
    }

    protected fun pickStationType(): String? {
        val stations = WeightedRandomPicker<String>()
        if (factionForUIColors.custom.has(Factions.CUSTOM_PIRATE_BASE_STATION_TYPES)) {
            try {
                val json: JSONObject =
                    factionForUIColors.custom.getJSONObject(Factions.CUSTOM_PIRATE_BASE_STATION_TYPES)
                for (key in JSONObject.getNames(json)) {
                    stations.add(key, json.optDouble(key, 0.0).toFloat())
                }
            } catch (e: JSONException) {
                stations.clear()
            }
        }
        if (stations.isEmpty) {
            stations.add(Industries.ORBITALSTATION, 5f)
            stations.add(Industries.ORBITALSTATION_MID, 3f)
            stations.add(Industries.ORBITALSTATION_HIGH, 1f)
        }

        //stations.add(Industries.STARFORTRESS, 100000f);
        return stations.pick()
    }

    private val stationIndustry: Industry?
        get() {
            for (curr in market.industries) {
                if (curr.spec.hasTag(Industries.TAG_STATION)) {
                    return curr
                }
            }
            return null
        }

    private fun updateStationIfNeeded() {
        if (matchedStationToTier == tier) return
        matchedStationToTier = tier
        monthsAtCurrentTier = 0
        var stationInd: Industry? = stationIndustry
        var currIndId: String? = null
        if (stationInd != null) {
            currIndId = stationInd.id
            market.removeIndustry(stationInd.id, null, false)
            stationInd = null
        }
        if (currIndId == null) {
            currIndId = pickStationType()
        }
        if (currIndId == null) return
        market.addIndustry(currIndId)
        stationInd = stationIndustry
        if (stationInd == null) return
        stationInd.finishBuildingOrUpgrading()
        val fleet = Misc.getStationFleet(entity) ?: return
        val members: List<FleetMemberAPI> = fleet.fleetData.membersListCopy
        if (members.isEmpty()) return
        fleet.inflateIfNeeded()
        val station: FleetMemberAPI = members[0]
        val picker: WeightedRandomPicker<Int> = WeightedRandomPicker<Int>()
        var index = 1 // index 0 is station body
        for (slotId in station.variant.moduleSlots) {
            val mv: ShipVariantAPI = station.variant.getModuleVariant(slotId)
            if (Misc.isActiveModule(mv)) {
                picker.add(index, 1f)
            }
            index++
        }
        var removeMult = 0f
        removeMult = when (tier) {
            RoiderBaseTier.TIER_1_1MODULE, RoiderBaseTier.TIER_2_1MODULE -> 0.67f
            RoiderBaseTier.TIER_3_2MODULE -> 0.33f
            RoiderBaseTier.TIER_4_3MODULE, RoiderBaseTier.TIER_5_3MODULE -> 0f
        }
        var remove = (picker.items.size * removeMult).roundToInt()
        if (remove < 1 && removeMult > 0) remove = 1
        if (remove >= picker.items.size) {
            remove = picker.items.size - 1
        }
        for (i in 0 until remove) {
            val pick = picker.pickAndRemove()
            if (pick != null) {
                station.status.setHullFraction(pick, 0f)
                station.status.setDetached(pick, true)
                station.status.setPermaDetached(pick, true)
            }
        }
    }

    fun getAddedListenerTo(): CampaignFleetAPI? {
        return addedListenerTo
    }

    private var addedListenerTo: CampaignFleetAPI? = null
    override fun advanceImpl(amount: Float) {
        //makeKnown();
        val days = Global.getSector().clock.convertToDays(amount)
        //days *= 1000f;
        //Global.getSector().getCurrentLocation().getName()
        //entity.getContainingLocation().getName()
        if (playerVisibleTimestamp == null && entity?.isInCurrentLocation == true && isHidden) {
            makeKnown()
            sendUpdateIfPlayerHasIntel(DISCOVERED_PARAM, false)
        }


        //System.out.println("Name: " + market.getName());

//        if (getPlayerVisibleTimestamp() != null && bountyData == null) setBounty();
        if (!sentBountyUpdate && bountyData != null &&
            (Global.getSector().intelManager.isPlayerInRangeOfCommRelay
                    || !isHidden && DebugFlags.SEND_UPDATES_WHEN_NO_COMM)
        ) {
            makeKnown()
            sendUpdateIfPlayerHasIntel(bountyData, false)
            sentBountyUpdate = true
        }
        val fleet = Misc.getStationFleet(market)
        if (fleet != null && addedListenerTo !== fleet) {
            if (addedListenerTo != null) {
                addedListenerTo!!.removeEventListener(this)
            }
            fleet.addEventListener(this)
            addedListenerTo = fleet
        }
        monthlyInterval.advance(days)
        if (monthlyInterval.intervalElapsed()) {
            checkForTierChange()
        }

//		if (bountyData == null && target != null) {
//			setBounty();
//		}
        if (bountyData != null) {
            val canEndBounty: Boolean = entity?.isInCurrentLocation == false
            bountyData!!.bountyElapsedDays += days
            if (bountyData!!.bountyElapsedDays > bountyData!!.bountyDuration && canEndBounty) {
                endBounty()
            }
        }

        //elapsedDays += days;
//		if (elapsedDays >= duration && !isDone()) {
//			endAfterDelay();
//			boolean current = market.getContainingLocation() == Global.getSector().getCurrentLocation();
//			sendUpdateIfPlayerHasIntel(new Object(), !current);
//			return;
//		}
        updateStationIfNeeded()
    }

    protected fun checkForTierChange() {
        if (bountyData != null) return
        if (entity?.isInCurrentLocation == true) return
        val minMonths = Global.getSettings().getFloat("pirateBaseMinMonthsForNextTier")
        if (monthsAtCurrentTier > minMonths) {
            val prob = (monthsAtCurrentTier - minMonths) * 0.1f
            if (Math.random().toFloat() < prob) {
                val next = getNextTier(tier)
                if (next != null) {
                    tier = next
                    updateStationIfNeeded()
                    monthsAtCurrentTier = 0
                    return
                }
            }
        }
        monthsAtCurrentTier++
    }

    private fun getNextTier(tier: RoiderBaseTier?): RoiderBaseTier? {
        when (tier) {
            RoiderBaseTier.TIER_1_1MODULE -> return RoiderBaseTier.TIER_2_1MODULE
            RoiderBaseTier.TIER_2_1MODULE -> return RoiderBaseTier.TIER_3_2MODULE
            RoiderBaseTier.TIER_3_2MODULE -> return RoiderBaseTier.TIER_4_3MODULE
            RoiderBaseTier.TIER_4_3MODULE -> return RoiderBaseTier.TIER_5_3MODULE
            RoiderBaseTier.TIER_5_3MODULE -> return null
            else -> {}
        }
        return null
    }

    private fun getPrevTier(tier: RoiderBaseTier?): RoiderBaseTier? {
        when (tier) {
            RoiderBaseTier.TIER_1_1MODULE -> return null
            RoiderBaseTier.TIER_2_1MODULE -> return RoiderBaseTier.TIER_1_1MODULE
            RoiderBaseTier.TIER_3_2MODULE -> return RoiderBaseTier.TIER_2_1MODULE
            RoiderBaseTier.TIER_4_3MODULE -> return RoiderBaseTier.TIER_3_2MODULE
            RoiderBaseTier.TIER_5_3MODULE -> return RoiderBaseTier.TIER_4_3MODULE
            else -> {}
        }
        return null
    }

    @JvmOverloads
    fun makeKnown(text: TextPanelAPI? = null) {
//		entity.setDiscoverable(null);
//		entity.setSensorProfile(null);
//		entity.getDetectedRangeMod().unmodify("gen");
        if (playerVisibleTimestamp == null) {
            Global.getSector().intelManager.removeIntel(this)
            Global.getSector().intelManager.addIntel(this, text == null, text)
        }
    }

    override fun getTimeRemainingFraction(): Float {
        return 1f - elapsedDays / duration
    }

    override fun notifyEnding() {
        super.notifyEnding()
        log.info(String.format("Removing indie roider base at [%s]", getSystem().name))
        Global.getSector().listenerManager.removeListener(this)
        Misc.removeRadioChatter(market)
        if (entity?.starSystem != null) {
            Misc.getStationFleet(entity)?.despawn()
            entity!!.market = null
            market.connectedEntities.remove(entity)
            Misc.fadeAndExpire(entity)
            //            entity.getStarSystem().removeEntity(entity);
        }
        Global.getSector().economy.removeMarket(market)
        Global.getSector().economy.removeUpdateListener(this)
        market.advance(0f)
    }

    private var result: PersonBountyIntel.BountyResult? = null
    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: CampaignEventListener.FleetDespawnReason?, param: Any?) {
        if (Helper.anyNull(fleet, reason)) return
        if (isEnding) return

        //CampaignFleetAPI station = Misc.getStationFleet(market); // null here since it's the skeleton station at this point
        if (addedListenerTo != null && fleet === addedListenerTo) {
            Misc.fadeAndExpire(entity)
            endAfterDelay()
            result = PersonBountyIntel.BountyResult(PersonBountyIntel.BountyResultType.END_OTHER, 0, null)
            if (reason == CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE &&
                param is BattleAPI
            ) {
                val battle: BattleAPI = param
                if (battle.isPlayerInvolved) {
                    var payment = 0
                    if (bountyData != null) {
                        payment = (bountyData!!.baseBounty * battle.playerInvolvementFraction).toInt()
                    }
                    if (payment > 0) {
                        Global.getSector().playerFleet.cargo.credits.add(payment.toFloat())
                        val impact = CoreReputationPlugin.CustomRepImpact()
                        impact.delta = bountyData!!.repChange * battle.playerInvolvementFraction
                        if (impact.delta < 0.01f) impact.delta = 0.01f
                        val rep: ReputationActionResponsePlugin.ReputationAdjustmentResult = Global.getSector().adjustPlayerReputation(
                            CoreReputationPlugin.RepActionEnvelope(
                                CoreReputationPlugin.RepActions.CUSTOM,
                                impact, null, null, false, true
                            ),
                            bountyData!!.bountyFaction?.id
                        )
                        result = PersonBountyIntel.BountyResult(
                            PersonBountyIntel.BountyResultType.END_PLAYER_BOUNTY,
                            payment,
                            rep
                        )
                    } else {
                        result = PersonBountyIntel.BountyResult(
                            PersonBountyIntel.BountyResultType.END_PLAYER_NO_REWARD,
                            0,
                            null
                        )
                    }
                }
            }
//            var sendUpdate =
//                DebugFlags.SEND_UPDATES_WHEN_NO_COMM || result.type != PersonBountyIntel.BountyResultType.END_OTHER ||
//                        Global.getSector().intelManager.isPlayerInRangeOfCommRelay
            val sendUpdate = true
            if (sendUpdate) {
                sendUpdateIfPlayerHasIntel(result, false)
            }
            Roider_RoiderBaseManager.instance.incrDestroyed()
            Roider_RoiderBaseManager.markRecentlyUsedForBase(getSystem())
        }
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}
    override fun runWhilePaused(): Boolean { return false }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode) {
        val h: Color = Misc.getHighlightColor()
        val g: Color = Misc.getGrayColor()
        val pad = 3f
        val opad = 10f
        var initPad = pad
        if (mode == IntelInfoPlugin.ListInfoMode.IN_DESC) initPad = opad
        val tc: Color = getBulletColorForMode(mode)
        bullet(info)
        val isUpdate = getListInfoParam() != null
        if (bountyData != null && result == null) {
            if (getListInfoParam() !== BOUNTY_EXPIRED_PARAM) {
                if (isUpdate || mode != IntelInfoPlugin.ListInfoMode.IN_DESC) {
                    val faction: FactionAPI? = bountyData!!.bountyFaction
                    info.addPara(
                        "Bounty faction: " + faction?.displayName, initPad, tc,
                        faction?.baseUIColor, faction?.displayName
                    )
                    initPad = 0f
                }
                info.addPara("%s reward", initPad, tc, h, Misc.getDGSCredits(bountyData!!.baseBounty))
                addDays(info, "remaining", bountyData!!.bountyDuration - bountyData!!.bountyElapsedDays, tc)
            }
        }
        if (result != null && bountyData != null) {
            when (result!!.type) {
                PersonBountyIntel.BountyResultType.END_PLAYER_BOUNTY -> {
                    info.addPara("%s received", initPad, tc, h, Misc.getDGSCredits(result!!.payment.toFloat()))
                    CoreReputationPlugin.addAdjustmentMessage(
                        result!!.rep.delta, bountyData!!.bountyFaction, null,
                        null, null, info, tc, isUpdate, 0f
                    )
                }

                PersonBountyIntel.BountyResultType.END_TIME -> {}
                PersonBountyIntel.BountyResultType.END_OTHER -> {}
                else -> {}
            }
        }
        unindent(info)
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode) {
        val c: Color = getTitleColor(mode)
        info.addPara(name, c, 0f)
        addBulletPoints(info, mode)
    }

    override fun getSortString(): String {
        return "Roider Base" // extern
    }

    override fun getName(): String {
//		String base = Misc.ucFirst(getFactionForUIColors().getPersonNamePrefix());
        val base = "Independent Roider"
        if (getListInfoParam() === bountyData && bountyData != null) {
            return "$base Base - Bounty Posted"
        } else if (getListInfoParam() === BOUNTY_EXPIRED_PARAM) {
            return "$base Base - Bounty Expired"
        }
        if (result != null) {
            if (result!!.type == PersonBountyIntel.BountyResultType.END_PLAYER_BOUNTY) {
                return "$base Base - Bounty Completed"
            } else if (result!!.type == PersonBountyIntel.BountyResultType.END_PLAYER_NO_REWARD) {
                return "$base Base - Destroyed"
            }
        }
        val name: String = market.name
        if (isEnding) {
            //return "Base Abandoned - " + name;
            return "$base Base - Abandoned"
        }
        if (getListInfoParam() === DISCOVERED_PARAM) {
            return "$base Base - Discovered"
        }
        return if (entity?.isDiscoverable == true) {
            "$base Base - Exact Location Unknown"
        } else "$base Base - $name" // extern
    }

    override fun getFactionForUIColors(): FactionAPI {
        return market.faction
    }

    override fun getSmallDescriptionTitle(): String {
        return name
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val h: Color = Misc.getHighlightColor()
        val g: Color = Misc.getGrayColor()
        val tc: Color = Misc.getTextColor()
        val pad = 3f
        val opad = 10f

        //info.addPara(getName(), c, 0f);

        //info.addSectionHeading(getName(), Alignment.MID, 0f);
        val faction: FactionAPI = market.faction
        info.addImage(faction.logo, width, 128f, opad)
        val has: String = faction.displayNameHasOrHave
        info.addPara(
            Misc.ucFirst(faction.displayNameWithArticle) + " " + has +
                    " established a base in the " + // extern
                    market.containingLocation.nameWithLowercaseType + ". " +
                    "The base serves as a staging area for local mining operations.",
            opad, faction.baseUIColor, faction.displayNameWithArticleWithoutArticle
        )
        if (entity?.isDiscoverable == false) {
            when (tier) {
                RoiderBaseTier.TIER_1_1MODULE -> info.addPara(
                    "It has very limited defensive capabilities " +
                            "and is protected by a few fleets.", opad // extern
                )

                RoiderBaseTier.TIER_2_1MODULE -> info.addPara(
                    "It has limited defensive capabilities " +
                            "and is protected by a small number of fleets.", opad
                )

                RoiderBaseTier.TIER_3_2MODULE -> info.addPara(
                    "It has fairly well-developed defensive capabilities " +
                            "and is protected by a considerable number of fleets.", opad
                )

                RoiderBaseTier.TIER_4_3MODULE -> info.addPara(
                    "It has very well-developed defensive capabilities " +
                            "and is protected by a large number of fleets.", opad
                )

                RoiderBaseTier.TIER_5_3MODULE -> info.addPara(
                    "It has very well-developed defensive capabilities " +
                            "and is protected by a large number of fleets. Both the " +
                            "base and the fleets have elite-level equipment, at least by roider standards.", opad
                )
            }
        } else {
            info.addPara("You have not yet discovered the exact location or capabilities of this base.", opad)
        }
        info.addSectionHeading(
            "Recent events",
            faction.baseUIColor, faction.darkUIColor, Alignment.MID, opad
        )
        if (bountyData != null) {
            info.addPara(
                Misc.ucFirst(bountyData!!.bountyFaction!!.displayNameWithArticle) + " " +
                        bountyData!!.bountyFaction!!.displayNameHasOrHave +
                        " posted a bounty for the destruction of this base.",
                opad, bountyData!!.bountyFaction!!.baseUIColor,
                bountyData!!.bountyFaction!!.displayNameWithArticleWithoutArticle
            )
            if (result != null && result!!.type == PersonBountyIntel.BountyResultType.END_PLAYER_BOUNTY) {
                info.addPara("You have successfully completed this bounty.", opad)
            }
            addBulletPoints(info, IntelInfoPlugin.ListInfoMode.IN_DESC)
        }
        if (result != null) {
            if (result!!.type == PersonBountyIntel.BountyResultType.END_PLAYER_NO_REWARD) {
                info.addPara("You have destroyed this base.", opad)
            } else if (result!!.type == PersonBountyIntel.BountyResultType.END_OTHER) {
                info.addPara("It is rumored that this base is no longer operational.", opad)
            }
        }
    }

    override fun getIcon(): String {
        return Global.getSettings().getSpriteName("intel", "roider_base")
    }

    
    override fun getIntelTags(map: SectorMapAPI): Set<String> {
        val tags: MutableSet<String> = super.getIntelTags(map)
        if (bountyData != null) {
            tags.add(Tags.INTEL_BOUNTY)
        }
        tags.add(Tags.INTEL_EXPLORATION)
        tags.add(market.factionId)
        if (bountyData != null) {
            tags.add(bountyData!!.bountyFaction?.id ?: "")
        }
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI): SectorEntityToken {
        //return market.getPrimaryEntity();
        return if (market.primaryEntity.isDiscoverable) {
            getSystem().center
        } else market.primaryEntity
    }

    protected fun generateName(): String? {
        MarkovNames.loadIfNeeded()
        for (i in 0..9) {
            val gen = MarkovNames.generate(null)
            if (gen != null) {
                var test: String = gen.name
                if (test.lowercase(Locale.getDefault()).startsWith("the ")) continue
                val p = pickPostfix()
                if (p.isNotEmpty()) {
                    test += " $p"
                }
                if (test.length > 22) continue
                return test
            }
        }
        return null
    }

    protected fun pickPostfix(): String {
        val post: WeightedRandomPicker<String> = WeightedRandomPicker<String>()
        //		post.add("Asylum");
        post.add("Astrome")
        post.add("Barrage")
        //		post.add("Briganderie");
        post.add("Camp")
        post.add("Cover")
        post.add("Citadel")
        post.add("Den")
        post.add("Donjon")
        post.add("Depot")
        post.add("Fort")
        post.add("Freehold")
        post.add("Freeport")
        post.add("Freehaven")
        post.add("Free Orbit")
        post.add("Galastat")
        post.add("Garrison")
        post.add("Harbor")
        post.add("Haven")
        //		post.add("Headquarters");
//		post.add("Hideout");
//		post.add("Hideaway");
        post.add("Hold")
        //		post.add("Lair");
        post.add("Locus")
        post.add("Main")
        post.add("Mine Depot")
        post.add("Nexus")
        post.add("Orbit")
        post.add("Port")
        post.add("Post")
        post.add("Presidio")
        //		post.add("Prison");
        post.add("Platform")
        //		post.add("Corsairie");
        post.add("Refuge")
        post.add("Retreat")
        post.add("Refinery")
        post.add("Shadow")
        post.add("Safehold")
        post.add("Starhold")
        post.add("Starport")
        post.add("Stardock")
        post.add("Sanctuary")
        post.add("Station")
        post.add("Spacedock")
        post.add("Tertiary")
        post.add("Terminus")
        post.add("Terminal")
        //		post.add("Tortuga");
        post.add("Ward")
        //		post.add("Warsat");
        return post.pick()
    }

    override fun commodityUpdated(commodityId: String) {
        val com: CommodityOnMarketAPI = market.getCommodityData(commodityId)
        var curr = 0
        val modId: String = market.id
        val mod: MutableStat.StatMod? = com.availableStat.getFlatStatMod(modId)
        if (mod != null) {
            curr = mod.value.roundToInt()
        }
        val a: Int = com.available - curr
        val d: Int = com.maxDemand
        if (d > a) {
            //int supply = Math.max(1, d - a - 1);
            val supply = Math.max(1, d - a)
            com.availableStat.modifyFlat(modId, supply.toFloat(), "Brought in by roiders") // extern
        }
    }

    override fun economyUpdated() {
        var fleetSizeBonus = 1f
        var qualityBonus = 0f
        var light = 0
        var medium = 0
        var heavy = 0
        when (tier) {
            RoiderBaseTier.TIER_1_1MODULE -> {
                qualityBonus = 0f
                fleetSizeBonus = 0.2f
            }

            RoiderBaseTier.TIER_2_1MODULE -> {
                qualityBonus = 0.2f
                fleetSizeBonus = 0.3f
                light = 2
            }

            RoiderBaseTier.TIER_3_2MODULE -> {
                qualityBonus = 0.3f
                fleetSizeBonus = 0.4f
                light = 2
                medium = 1
            }

            RoiderBaseTier.TIER_4_3MODULE -> {
                qualityBonus = 0.4f
                fleetSizeBonus = 0.5f
                light = 2
                medium = 2
            }

            RoiderBaseTier.TIER_5_3MODULE -> {
                qualityBonus = 0.5f
                fleetSizeBonus = 0.75f
                light = 2
                medium = 2
                heavy = 2
            }
        }
        market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).modifyFlatAlways(
            market.id, qualityBonus,
            "Development level" // extern
        )
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlatAlways(
            market.id,
            fleetSizeBonus,
            "Development level"
        )
        val modId: String = market.id
        market.stats.dynamic.getMod(Stats.PATROL_NUM_LIGHT_MOD).modifyFlat(modId, light.toFloat())
        market.stats.dynamic.getMod(Stats.PATROL_NUM_MEDIUM_MOD).modifyFlat(modId, medium.toFloat())
        market.stats.dynamic.getMod(Stats.PATROL_NUM_HEAVY_MOD).modifyFlat(modId, heavy.toFloat())
    }

    override fun isEconomyListenerExpired(): Boolean {
        return isEnded
    }

    fun getMarket(): MarketAPI {
        return market
    }

    private fun setBounty() {
        bountyData = BaseBountyData()
        var base = 100000f
        when (tier) {
            RoiderBaseTier.TIER_1_1MODULE -> {
                base = Global.getSettings().getFloat("pirateBaseBounty1") // extern?
                bountyData!!.repChange = 0.02f
            }

            RoiderBaseTier.TIER_2_1MODULE -> {
                base = Global.getSettings().getFloat("pirateBaseBounty2")
                bountyData!!.repChange = 0.05f
            }

            RoiderBaseTier.TIER_3_2MODULE -> {
                base = Global.getSettings().getFloat("pirateBaseBounty3")
                bountyData!!.repChange = 0.06f
            }

            RoiderBaseTier.TIER_4_3MODULE -> {
                base = Global.getSettings().getFloat("pirateBaseBounty4")
                bountyData!!.repChange = 0.07f
            }

            RoiderBaseTier.TIER_5_3MODULE -> {
                base = Global.getSettings().getFloat("pirateBaseBounty5")
                bountyData!!.repChange = 0.1f
            }
        }
        base /= 2f
        bountyData!!.baseBounty = base * (0.9f + Math.random().toFloat() * 0.2f)
        bountyData!!.baseBounty = ((bountyData!!.baseBounty / 10000).toInt() * 10000).toFloat()
        val picker: WeightedRandomPicker<FactionAPI> = WeightedRandomPicker<FactionAPI>()
        picker.add(Global.getSector().getFaction(Factions.PIRATES))
        //		for (MarketAPI curr : Global.getSector().getEconomy().getMarkets(target)) {
//			if (curr.getFaction().isPlayerFaction()) continue;
//			if (affectsMarket(curr)) {
//				picker.add(curr.getFaction(), (float) Math.pow(2f, curr.getSize()));
//			}
//		}
        val faction: FactionAPI? = picker.pick()
        if (faction == null) {
            bountyData = null
            return
        }
        bountyData!!.bountyFaction = faction
        bountyData!!.bountyDuration = 180f
        bountyData!!.bountyElapsedDays = 0f
        Misc.makeImportant(entity, "baseBounty") // extern
        sentBountyUpdate = false
        //		makeKnown();
//		sendUpdateIfPlayerHasIntel(bountyData, false);
    }

    private var sentBountyUpdate = false
    private fun endBounty() {
        sendUpdateIfPlayerHasIntel(BOUNTY_EXPIRED_PARAM, false)
        bountyData = null
        sentBountyUpdate = false
        Misc.makeUnimportant(entity, "baseBounty") // extern
    }

    fun getEntity(): SectorEntityToken? {
        return entity
    }
}