package scripts.campaign.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.EconomyAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.procgen.MarkovNames
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.MemoryKeys
import java.awt.Color
import java.util.*
import kotlin.collections.LinkedHashMap

class Roider_RoiderHQBaseIntel(system: StarSystemAPI, factionId: String?) : BaseIntelPlugin(), EveryFrameScript,
    FleetEventListener, EconomyAPI.EconomyUpdateListener {
    private val system: String
    private val market: MarketAPI
    private val entity: SectorEntityToken?
    private var elapsedDays = 0f
    private var duration = 45f
    private val monthlyInterval: IntervalUtil = IntervalUtil(20f, 40f)
    fun init() {
        if (isDone) return
        Global.getSector().economy.addMarket(market, true)
        log.info(String.format("Added roider HQ base in [%s]", getSystem().name))
        Global.getSector().intelManager.addIntel(this, true)
        timestamp = null
        Global.getSector().listenerManager.addListener(this)
        Global.getSector().economy.addUpdateListener(this)
    }

    override fun isHidden(): Boolean {
        return if (super.isHidden()) true else timestamp == null
    } 

    fun getSystem(): StarSystemAPI {
        return Global.getSector().getStarSystem(system)
    }

    fun getAddedListenerTo(): CampaignFleetAPI? {
        return addedListenerTo
    }

    private var addedListenerTo: CampaignFleetAPI? = null

    init {
        this.system = system.id
        market = Global.getFactory().createMarket("roider_fringeHQBase_" + Misc.genUID(), "Roider Base", 4)
        market.size = 4
        market.isHidden = true
        market.factionId = Factions.INDEPENDENT
        market.surveyLevel = MarketAPI.SurveyLevel.FULL
        market.factionId = factionId
        market.addCondition(Conditions.POPULATION_4)
        market.addIndustry(Industries.POPULATION)
        market.addIndustry(Industries.SPACEPORT)
        market.addIndustry(Industries.HEAVYBATTERIES)
        // Battlestation added later
        market.addIndustry(Industries.REFINING)
        market.addIndustry(RoiderIndustries.UNION_HQ)
        market.addIndustry(RoiderIndustries.SHIPWORKS)
        market.addSubmarket(Submarkets.SUBMARKET_OPEN)
        market.addSubmarket(Submarkets.SUBMARKET_BLACK)
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE)
        market.tariff.modifyFlat("default_tariff", market.faction.tariffFraction)

        val weights = LinkedHashMap<LocationType, Float>()
        weights[LocationType.IN_ASTEROID_BELT] = 10f
        weights[LocationType.IN_ASTEROID_FIELD] = 10f
        weights[LocationType.IN_RING] = 10f
        weights[LocationType.IN_SMALL_NEBULA] = 10f
        weights[LocationType.GAS_GIANT_ORBIT] = 10f
        weights[LocationType.PLANET_ORBIT] = 10f
        weights[LocationType.STAR_ORBIT] = 1f
        val locs = BaseThemeGenerator.getLocations(null, system, null, 100f, weights)
        val loc = locs.pick()
        if (loc == null) {
            endImmediately()
            entity = null
        } else {
            val added = BaseThemeGenerator.addNonSalvageEntity(system, loc, Entities.MAKESHIFT_STATION, factionId)
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
                    entity.setName(name)


//		boolean down = false;
//		if (entity.getOrbitFocus() instanceof PlanetAPI) {
//			PlanetAPI planet = (PlanetAPI) entity.getOrbitFocus();
//			if (!planet.isStar()) {
//				down = true;
//			}
//		}
//		if (down) {
//			BaseThemeGenerator.convertOrbitPointingDown(entity);
//		}
                    BaseThemeGenerator.convertOrbitWithSpin(entity, -5f)
                    market.primaryEntity = entity
                    entity.setMarket(market)

//		entity.setSensorProfile(1f);
                    entity.setDiscoverable(true)
                    //		entity.getDetectedRangeMod().modifyFlat("gen", 5000f);
                    market.econGroup = market.id
                    market.memoryWithoutUpdate.set(DecivTracker.NO_DECIV_KEY, true)
                    market.addIndustry(Industries.BATTLESTATION)
                    market.reapplyIndustries()
                    market.memoryWithoutUpdate.set(MemoryKeys.FRINGE_HQ, true)

//        Roider_Dives dives = (Roider_Dives) market.getIndustry(Roider_Ids.Industries.UNION_HQ);
//        if (!dives.canMine()) {
//			endImmediately();
//            return;
//        }

//		PortsideBarData.getInstance().addEvent(new PirateBaseRumorBarEvent(this));
                }
            }
        }
    }

    override fun advanceImpl(amount: Float) {
        if (playerVisibleTimestamp == null && entity?.isInCurrentLocation == true && isHidden) {
            makeKnown()
            sendUpdateIfPlayerHasIntel(DISCOVERED_PARAM, false)
            market.econGroup = null
            market.isHidden = false
            entity?.isDiscoverable = false
        }
        val fleet: CampaignFleetAPI = Misc.getStationFleet(market)
        if (fleet != null && addedListenerTo !== fleet) {
            if (addedListenerTo != null) {
                addedListenerTo!!.removeEventListener(this)
            }
            fleet.addEventListener(this)
            addedListenerTo = fleet
        }
    }

    @JvmOverloads
    fun makeKnown(text: TextPanelAPI? = null) {
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
        log.info(String.format("Removing roider base at [%s]", getSystem().name))
        Global.getSector().listenerManager.removeListener(this)
        Global.getSector().economy.removeMarket(market)
        Global.getSector().economy.removeUpdateListener(this)
        Misc.removeRadioChatter(market)
        market.advance(0f)
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: CampaignEventListener.FleetDespawnReason?, param: Any?) {
        if (isEnding) return

        //CampaignFleetAPI station = Misc.getStationFleet(market); // null here since it's the skeleton station at this point
        if (addedListenerTo != null && fleet === addedListenerTo) {
            Misc.fadeAndExpire(entity)
            endAfterDelay()
            Roider_RoiderBaseManager.instance.incrDestroyed()
            Roider_RoiderBaseManager.markRecentlyUsedForBase(getSystem())
        }
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}
    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun createIntelInfo(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?) {
        val c: Color = getTitleColor(mode)
        info?.addPara(name, c, 0f)
    }

    override fun getSortString(): String {
        val base: String = Misc.ucFirst(factionForUIColors.personNamePrefix)
        return "$base Base"
    }

    override fun getName(): String {
        val name: String = market.name
        val base = "Roider Union"
        if (isEnding) {
            //return "Base Abandoned - " + name;
            return "$base Base - Abandoned"
        }
        if (getListInfoParam() === DISCOVERED_PARAM) {
            return "$base Base - Discovered"
        }
        return if (entity?.isDiscoverable == true) {
            "$base Base - Exact Location Unknown"
        } else "$base Base - $name"
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
        val local = "regional"
        info.addPara(
            Misc.ucFirst(faction.displayNameWithArticle) + " " + has +
                    " established a base in the " +
                    market.containingLocation.nameWithLowercaseType + ". " +
                    "The base serves as a staging area for " + local + " mining operations.",
            opad, faction.baseUIColor, faction.displayNameWithArticleWithoutArticle
        )
        if (entity?.isDiscoverable == false) {
            info.addPara(
                "It has very well-developed defensive capabilities " +
                        "and is protected by a large number of fleets.", opad
            )
        } else {
            info.addPara("You have not yet discovered the exact location or capabilities of this base.", opad)
        }
    }

    override fun getIcon(): String {
        return Global.getSettings().getSpriteName("intel", "roider_base")
    }

    //return market.getFaction().getCrest();
    override fun getIntelTags(map: SectorMapAPI): Set<String> {
        val tags: MutableSet<String> = super.getIntelTags(map)
        tags.add(Tags.INTEL_EXPLORATION)
        tags.add(RoiderFactions.ROIDER_UNION)
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI): SectorEntityToken {
        //return market.getPrimaryEntity();
        return if (market.primaryEntity.isDiscoverable) {
            getSystem().center
        } else market.primaryEntity
    }

    private fun generateName(): String? {
        MarkovNames.loadIfNeeded()
        for (i in 0..9) {
            val gen = MarkovNames.generate(null)
            if (gen != null) {
                var test: String = gen.name
                if (test.lowercase(Locale.getDefault()).startsWith("the ")) continue
                val p = pickPostfix()
                if (p != null && !p.isEmpty()) {
                    test += " $p"
                }
                if (test.length > 22) continue
                return test
            }
        }
        return null
    }

    private fun pickPostfix(): String {
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
            curr = Math.round(mod.value)
        }
        var avWithoutPenalties = Math.round(com.availableStat.baseValue).toInt()
        for (m in com.availableStat.flatMods.values) {
            if (m.value < 0) continue
            avWithoutPenalties += Math.round(m.value).toInt()
        }
        val a = avWithoutPenalties - curr
        val d: Int = com.maxDemand
        if (d > a) {
            //int supply = Math.max(1, d - a - 1);
            val supply = Math.max(1, d - a)
            com.availableStat.modifyFlat(modId, supply.toFloat(), "Brought in by roiders")
        }
    }

    override fun economyUpdated() {}

    override fun isEconomyListenerExpired(): Boolean {
        return isEnded
    }

    fun getMarket(): MarketAPI {
        return market
    }

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_RoiderHQBaseIntel::class.java, "system", "s")
            x.aliasAttribute(Roider_RoiderHQBaseIntel::class.java, "market", "m")
            x.aliasAttribute(Roider_RoiderHQBaseIntel::class.java, "entity", "e")
            x.aliasAttribute(Roider_RoiderHQBaseIntel::class.java, "elapsedDays", "ed")
            x.aliasAttribute(Roider_RoiderHQBaseIntel::class.java, "duration", "d")
            x.aliasAttribute(Roider_RoiderHQBaseIntel::class.java, "monthlyInterval", "i")
        }

        var DISCOVERED_PARAM = Any()
        var log = Global.getLogger(Roider_RoiderHQBaseIntel::class.java)
    }
}