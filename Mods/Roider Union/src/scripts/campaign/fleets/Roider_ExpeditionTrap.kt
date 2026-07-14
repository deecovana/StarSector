package scripts.campaign.fleets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.ai.FleetAIFlags
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.SpecialCreationContext
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.SpecialCreator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction.SalvageSpecialData
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction.SalvageSpecialPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import scripts.campaign.fleets.Roider_ExpeditionTrap
import scripts.campaign.fleets.Roider_ExpeditionTrap.Roider_ExpeditionTrapCreator
import scripts.campaign.fleets.Roider_ExpeditionTrap.Roider_ExpeditionTrapData
import scripts.campaign.fleets.expeditions.Roider_ExpeditionFleetFactory
import java.util.*

class Roider_ExpeditionTrap : BaseSalvageSpecial() {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_ExpeditionTrap::class.java, "data", "d")
            x.alias("roider_expTrapData", Roider_ExpeditionTrapData::class.java)
            Roider_ExpeditionTrapData.aliasAttributes(x)
        }

        fun makeFleetInterceptPlayer(
            fleet: CampaignFleetAPI,
            makeAggressive: Boolean, makeLowRepImpact: Boolean,
            interceptDays: Float
        ) {
            makeFleetInterceptPlayer(fleet, makeAggressive, makeLowRepImpact, true, interceptDays)
        }

        fun makeFleetInterceptPlayer(
            fleet: CampaignFleetAPI,
            makeAggressive: Boolean, makeLowRepImpact: Boolean,
            makeHostile: Boolean, interceptDays: Float
        ) {
            val playerFleet = Global.getSector().playerFleet
            if (fleet.ai == null) {
                fleet.ai = Global.getFactory().createFleetAI(fleet)
                fleet.setLocation(fleet.location.x, fleet.location.y)
            }
            if (makeAggressive) {
                val expire = fleet.memoryWithoutUpdate.getExpire(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE)
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true] = Math.max(expire, interceptDays)
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE_ONE_BATTLE_ONLY, true] =
                    Math.max(expire, interceptDays)
            }
            if (makeHostile) {
                fleet.memoryWithoutUpdate.unset(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE)
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE, true] = interceptDays
            }
            fleet.memoryWithoutUpdate[FleetAIFlags.PLACE_TO_LOOK_FOR_TARGET, Vector2f(playerFleet.location)] =
                interceptDays
            if (makeLowRepImpact) {
                Misc.makeLowRepImpact(playerFleet, "ttSpecial")
            }
            if (fleet.ai is ModularFleetAIAPI) {
                (fleet.ai as ModularFleetAIAPI).tacticalModule.target = playerFleet
            }
            fleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, playerFleet, interceptDays, null)
        }
    }

    class Roider_ExpeditionTrapCreator(
        private val random: Random,
        private val chance: Float,
        private val fleetType: String?,
        private val faction: String,
        private val source: String,
        private val minPts: Int,
        private val maxPts: Int,
        major: Boolean
    ) : SpecialCreator {
        companion object {
            fun aliasAttributes(x: XStream) {
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "random", "r")
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "faction", "f")
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "source", "s")
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "minPts", "mi")
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "maxPts", "ma")
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "chance", "c")
                x.aliasAttribute(Roider_ExpeditionTrapCreator::class.java, "fleetType", "t")
            }
        }

        override fun createSpecial(entity: SectorEntityToken, context: SpecialCreationContext): Any {
            val data = Roider_ExpeditionTrapData()
            data.prob = chance
            data.nearbyFleetFaction = faction
            data.useAllFleetsInRange = true
            if (fleetType != null) {
                var combatPoints = minPts + random.nextInt(maxPts - minPts + 1)
                combatPoints *= 5
                val params = FleetParamsV3(
                    Global.getSector().economy.getMarket(source),
                    entity.locationInHyperspace,
                    faction,
                    null,
                    fleetType,
                    combatPoints.toFloat(),  // combatPts
                    0f,  // freighterPts
                    0f,  // tankerPts
                    0f,  // transportPts
                    0f,  // linerPts
                    0f,  // utilityPts
                    0f // qualityMod
                )
                data.params = params
            }
            return data
        }
    }

    class Roider_ExpeditionTrapData : SalvageSpecialData {
        var prob = 0.5f
        var fleetId: String? = null
        var nearbyFleetFaction: String? = null
        var useClosestFleetInRange: Boolean? = null
        var useAllFleetsInRange: Boolean? = null
        var params: FleetParamsV3? = null
        var minRange = 2500f
        var maxRange = 5000f
        var major = false

        constructor() {}
        constructor(params: FleetParamsV3?) {
            this.params = params
        }

        constructor(prob: Float, params: FleetParamsV3?) {
            this.prob = prob
            this.params = params
        }

        override fun createSpecialPlugin(): SalvageSpecialPlugin {
            return Roider_ExpeditionTrap()
        }

        companion object {
            fun aliasAttributes(x: XStream) {
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "prob", "p")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "fleetId", "f")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "nearbyFleetFaction", "n")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "useClosestFleetInRange", "c")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "useAllFleetsInRange", "a")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "params", "pa")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "minRange", "mi")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "maxRange", "ma")
                x.aliasAttribute(Roider_ExpeditionTrapData::class.java, "major", "m")
            }
        }
    }

    private var data: Roider_ExpeditionTrapData? = null
    override fun init(dialog: InteractionDialogAPI, specialData: Any) {
        super.init(dialog, specialData)
        data = specialData as Roider_ExpeditionTrapData
        initEntityLocation()
    }

    private fun initEntityLocation() {
        if (data!!.major) {
            addText(
                "Near the \$shortName, sensors pick up a mine left over from the minefield. "
                        + "It appears to have been disabled by the same signal as the others."
            )
        }
        if (random.nextFloat() > data!!.prob) {
            if (random.nextFloat() > 0.5f) {
                addText(
                    "Your salvage crews discover a transmitter set to send a signal when " +
                            "tripped by an alarm system, but it doesn't appear to be functional. " +
                            "Closer examination indicates it was probably set many cycles ago."
                )
            } else {
                addText(
                    "Your salvage crews discover a transmitter set to send a signal when " +
                            "tripped by an alarm system. The alarm went off as intended, but the transmitter " +
                            "was fried by a power surge before it could do its job."
                )
            }
            isDone = true
            setEndWithContinue(true)
            setShowAgain(false)
            return
        }
        if (entity is PlanetAPI) {
            addText(
                "As your salvage crews begin their work, a transmitter hidden somewhere planetside " +
                        "sends out an encrypted, broadwave signal. Whatever destination it's meant for, " +
                        "it has to be nearby."
            )
        } else {
            addText(
                "As your salvage crews begin their work, a transmitter inside the \$shortName " +
                        "sends out an encrypted, broadwave signal. Whatever destination it's meant for, " +
                        "it has to be nearby."
            )
        }
        transmitterActivated()
        isDone = true
        setEndWithContinue(true)
        setShowAgain(false)
    }

    private fun transmitterActivated() {
        if (data == null) return
        if (entity == null) return
        if (data!!.fleetId != null) {
            val found = Global.getSector().getEntityById(data!!.fleetId)
            if (found is CampaignFleetAPI) {
                val fleet = found
                val flagship = fleet.flagship
                var makeAggressive = false
                if (flagship != null) {
                    makeAggressive = flagship.variant.hasHullMod(HullMods.AUTOMATED)
                }
                makeFleetInterceptPlayer(fleet, makeAggressive, true, 30f)
            }
            return
        }
        if (data!!.useAllFleetsInRange != null && data!!.useAllFleetsInRange!!) {
            var foundSomeFleets = false
            for (fleet in entity.containingLocation.fleets) {
                if (data!!.nearbyFleetFaction != null &&
                    data!!.nearbyFleetFaction != fleet.faction.id
                ) {
                    continue
                }
                if (fleet.faction.isPlayerFaction) continue
                if (fleet.isStationMode) continue
                if (fleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET)) continue
                val dist = Misc.getDistance(fleet.location, entity.location)
                if (dist < data!!.maxRange) {
                    val flagship = fleet.flagship
                    var makeAggressive = false
                    if (flagship != null) {
                        makeAggressive = flagship.variant.hasHullMod(HullMods.AUTOMATED)
                    }
                    makeFleetInterceptPlayer(fleet, makeAggressive, true, 30f)
                    foundSomeFleets = true
                }
            }
            if (foundSomeFleets) return
        }
        if (data!!.useClosestFleetInRange != null && data!!.useClosestFleetInRange!!) {
            var closest: CampaignFleetAPI? = null
            var minDist = Float.MAX_VALUE
            for (fleet in entity.containingLocation.fleets) {
                if (data!!.nearbyFleetFaction != null &&
                    data!!.nearbyFleetFaction != fleet.faction.id
                ) {
                    continue
                }
                if (fleet.faction.isPlayerFaction) continue
                if (fleet.isStationMode) continue
                if (fleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_TRADE_FLEET)) continue
                val dist = Misc.getDistance(fleet.location, entity.location)
                if (dist < data!!.maxRange && dist < minDist) {
                    closest = fleet
                    minDist = dist
                }
            }
            if (closest != null) {
                val flagship = closest.flagship
                var makeAggressive = false
                if (flagship != null) {
                    makeAggressive = flagship.variant.hasHullMod(HullMods.AUTOMATED)
                }
                makeFleetInterceptPlayer(closest, makeAggressive, true, 30f)
                return
            }
        }
        if (data?.params != null) {
            val range = data!!.minRange + random.nextFloat() * (data!!.maxRange - data!!.minRange)
            val loc = Misc.getPointAtRadius(entity.location, range)
            val type = data!!.params!!.fleetType
            val source = data!!.params!!.source
            val pirate = data!!.params!!.factionId == Factions.INDEPENDENT && random.nextBoolean()
            val fleet = Roider_ExpeditionFleetFactory.createExpedition(
                type,
                entity.locationInHyperspace,
                source,
                pirate,
                random
            )
            if (fleet == null || fleet.isEmpty) return
            fleet.isTransponderOn = false
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PIRATE] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_LOW_REP_IMPACT] = true
            entity.containingLocation.addEntity(fleet)
            fleet.setLocation(loc.x, loc.y)
            val flagship = fleet.flagship
            var makeAggressive = false
            if (flagship != null) {
                makeAggressive = flagship.variant.hasHullMod(HullMods.AUTOMATED)
            }
            makeFleetInterceptPlayer(fleet, makeAggressive, true, 30f)


//			SectorEntityToken despawnLoc = entity.getContainingLocation().createToken(20000, 0);
//			fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, despawnLoc, 10000f);
            Misc.giveStandardReturnToSourceAssignments(fleet, false)
            return
        }
    }

    override fun optionSelected(optionText: String, optionData: Any) {
        super.optionSelected(optionText, optionData)
    }
}