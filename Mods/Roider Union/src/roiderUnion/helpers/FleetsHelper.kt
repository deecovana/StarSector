package roiderUnion.helpers

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ids.RoiderFactions
import java.util.*
import kotlin.collections.ArrayList

object FleetsHelper {
    enum class Category {
        COMBAT,
        FREIGHTER,
        TANKER,
        TRANSPORT,
        LINER,
        UTILITY
    }
    enum class Weight(val base: Float, val variance: Float) {
        EXTREME(80f, 20f),
        HIGH(40f, 20f),
        MID(20f, 10f),
        LOW(5f, 10f),
        NONE(0f, 0f);
    }

    private fun getWeight(random: Random, weight: Weight?): Float {
        if (weight == null) return 0f
        return weight.base + weight.variance * random.nextFloat()
    }

    class MultiFleetFactionParams(val id: String, val weight: Float)

    fun getStandardFactionParams(faction: String): Array<MultiFleetFactionParams> {
        return when (faction) {
            RoiderFactions.ROIDER_UNION -> {
                arrayOf(
                    MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, 1f),
                    MultiFleetFactionParams(RoiderFactions.ROIDER_UNION_BULK, 1f),
                    MultiFleetFactionParams(Factions.INDEPENDENT, 1f)
                )
            }
            Factions.INDEPENDENT -> {
                arrayOf(
                    MultiFleetFactionParams(Factions.INDEPENDENT, 2f),
                    MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, 1f)
                )
            }
            else -> {
                arrayOf(
                    MultiFleetFactionParams(faction, 1f),
                    MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, 1f),
                    MultiFleetFactionParams(Factions.INDEPENDENT, 1f)
                )
            }
        }
    }

    fun createFleetParams(
        random: Random,
        points: Map<Category, Weight>,
        fp: Float,
        qualityMod: Float,
        qualityOverride: Float?,
        source: MarketAPI?,
        locInHyper: Vector2f?,
        factionId: String?,
        fleetType: String?
    ): FleetParamsV3 {
        val results = FleetParamsV3(
            source,
            locInHyper,
            factionId,
            qualityOverride,
            fleetType,
            getWeight(random, points[Category.COMBAT]),
            getWeight(random, points[Category.FREIGHTER]),
            getWeight(random, points[Category.TANKER]),
            getWeight(random, points[Category.TRANSPORT]),
            getWeight(random, points[Category.LINER]),
            getWeight(random, points[Category.UTILITY]),
            qualityMod
        )
        convertFpToPercent(results, fp)
        return results
    }

    /**
     * Multiplies each category's points by targetFP and flattens it so the total FP matches targetFP.
     */
    fun convertFpToPercent(
        params: FleetParamsV3,
        targetFP: Float
    ) {
        val divisor = params.combatPts + params.freighterPts + params.tankerPts
            + params.transportPts + params.linerPts + params.utilityPts
        if (divisor == 0f) return
        params.combatPts *= targetFP / divisor
        params.freighterPts *= targetFP / divisor
        params.tankerPts *= targetFP / divisor
        params.transportPts *= targetFP / divisor
        params.linerPts *= targetFP / divisor
        params.utilityPts *= targetFP / divisor
    }

    fun createMultifactionFleet(params: FleetParamsV3, primaryFaction: String, vararg factions: MultiFleetFactionParams): CampaignFleetAPI {
        val allParams = splitParams(params, *factions)
        val fleet = FleetFactoryV3.createFleet(allParams[0])
        for (p in allParams) {
            if (p.factionId == primaryFaction) continue
            val tempFleet = FleetFactoryV3.createFleet(p)
            if (tempFleet != null && !tempFleet.isEmpty) {
                for (member in tempFleet.membersWithFightersCopy) {
                    if (member.isFighterWing) continue
                    member.captain = null
                    if (member.isFlagship) member.isFlagship = false
                    fleet.fleetData.addFleetMember(member)
                }
            }
        }
        FleetFactoryV3.addCommanderAndOfficers(fleet, params, params.random ?: Helper.random)
        fleet.fleetData.sort()
        return fleet
    }

    private fun splitParams(params: FleetParamsV3, vararg factions: MultiFleetFactionParams): List<FleetParamsV3> {
        val results = ArrayList<FleetParamsV3>()
        var totalWeight = 0f
        factions.forEach { totalWeight += it.weight }

        var isPrimary = true
        for (faction in factions) {
            if (faction.id.isEmpty()) continue
            if (faction.weight == 0f) continue
            val splitParams = buildSplitParams(params, faction.id, totalWeight / faction.weight)
            copyParamsExtra(params, splitParams, isPrimary, totalWeight / faction.weight)
            splitParams.maxNumShips = (getMaxShips(params) * faction.weight / totalWeight).toInt()
            if (isPrimary) {
                splitParams.maxNumShips++ // Fix fraction loss from integer conversion
                isPrimary = false
            }
            results.add(splitParams)
        }

        return results
    }

    private fun getMaxShips(params: FleetParamsV3) = params.maxNumShips ?: Settings.MAX_SHIPS_IN_AI_FLEET

    private fun buildSplitParams(params: FleetParamsV3, faction: String, divisor: Float): FleetParamsV3 {
        val div = if (divisor == 0f) 1f else divisor
        return FleetParamsV3(
            params.source,
            params.locInHyper,
            faction,
            params.qualityOverride,
            params.fleetType,
            params.combatPts / div,
            params.freighterPts / div,
            params.tankerPts / div,
            params.transportPts / div,
            params.linerPts / div,
            params.utilityPts / div,
            params.qualityMod
        )
    }

    private fun copyParamsExtra(sourceParams: FleetParamsV3, destParams: FleetParamsV3, isPrimary: Boolean, divisor: Float) {
        destParams.fleetType = sourceParams.fleetType ?: null
        destParams.aiCores = sourceParams.aiCores ?: null
        destParams.allWeapons = sourceParams.allWeapons ?: null
        destParams.averageSMods = sourceParams.averageSMods ?: null
        destParams.doNotAddShipsBeforePruning = sourceParams.doNotAddShipsBeforePruning ?: null
        destParams.doNotPrune = sourceParams.doNotPrune ?: null
        destParams.doctrineOverride = sourceParams.doctrineOverride ?: null
        destParams.doNotIntegrateAICores = sourceParams.doNotIntegrateAICores
        destParams.flagshipVariant = sourceParams.flagshipVariant ?: null
        destParams.flagshipVariantId = sourceParams.flagshipVariantId ?: null
        destParams.forceAllowPhaseShipsEtc = sourceParams.forceAllowPhaseShipsEtc ?: null
        destParams.ignoreMarketFleetSizeMult = sourceParams.ignoreMarketFleetSizeMult ?: null
        destParams.modeOverride = sourceParams.modeOverride ?: null
        destParams.maxNumShips = if (sourceParams.maxNumShips != null) sourceParams.maxNumShips / divisor.toInt() else null
        destParams.maxShipSize = sourceParams.maxShipSize
        destParams.minShipSize = sourceParams.minShipSize
        destParams.officerLevelBonus = sourceParams.officerLevelBonus
        destParams.officerLevelLimit = sourceParams.officerLevelLimit
        destParams.officerNumberBonus = sourceParams.officerNumberBonus
        destParams.officerNumberMult = sourceParams.officerNumberMult
        destParams.onlyApplyFleetSizeToCombatShips = sourceParams.onlyApplyFleetSizeToCombatShips ?: null
        destParams.onlyRetainFlagship = sourceParams.onlyRetainFlagship ?: null
        destParams.random = sourceParams.random ?: null
        destParams.timestamp = sourceParams.timestamp ?: null
        destParams.treatCombatFreighterSettingAsFraction = sourceParams.treatCombatFreighterSettingAsFraction ?: null
        destParams.withOfficers = sourceParams.withOfficers
        if (isPrimary) {
            destParams.commander = sourceParams.commander ?: null
            destParams.commanderLevelLimit = sourceParams.commanderLevelLimit
            destParams.noCommanderSkills = sourceParams.noCommanderSkills ?: null
        }
    }

}