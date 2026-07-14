package roiderUnion.fleets.nomads.motherFleets

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.ext.campaign.addShip
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.FleetsHelper
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import roiderUnion.ids.Variants

object MotherFleetSpawner {
    const val HEAVY_FP = 110f
    const val LIGHT_FP = 50f

    @JvmField
    val ARGOS_VARIANTS = listOf(
        Variants.ARGOS_BALANCED,
        Variants.ARGOS_OUTDATED,
        Variants.ARGOS_SUPPORT,
        Variants.ARGOS_ELITE,
    )
    @JvmField
    val ARGOS_GOOD_VARIANTS = listOf(
        Variants.ARGOS_BALANCED,
        Variants.ARGOS_SUPPORT,
        Variants.ARGOS_ELITE,
    )

    fun spawnFleet(route: RouteManager.RouteData?): CampaignFleetAPI? {
        if (route == null) return null
        val type = route.extra?.fleetType ?: return null
        val result = createFleet(route, route.factionId, type) ?: return null
        if (type == RoiderFleetTypes.NOMAD_MOTHER_FLEET) addArgos(result, route.factionId)
        addTags(result, route.source, route.custom as List<*>)
        return result
    }

    private fun addArgos(fleet: CampaignFleetAPI, factionId: String) {
        val variant = when (factionId) {
            Factions.INDEPENDENT -> getRandomArgosVariant()
            Factions.PIRATES -> Variants.ARGOS_OUTDATED
            RoiderFactions.ROIDER_UNION -> getRandomGoodArgosVariant()
            else -> getRandomGoodArgosVariant()
        }
        fleet.addShip(variant, FleetMemberType.SHIP)
    }

    private fun getRandomGoodArgosVariant(): String {
        val rand = Misc.random.nextInt(ARGOS_GOOD_VARIANTS.size)
        return ARGOS_GOOD_VARIANTS[rand]
    }

    private fun getRandomArgosVariant(): String {
        val rand = Misc.random.nextInt(ARGOS_VARIANTS.size)
        return ARGOS_VARIANTS[rand]
    }

    private fun addTags(fleet: CampaignFleetAPI, id: String, offerings: List<*>) {
        Memory.set(MemoryKeys.APR_RETROFITTING, true, fleet)
        Memory.setFlag(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, id, fleet, Short.MAX_VALUE.toFloat())
        Memory.setFlag(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, id, fleet, Short.MAX_VALUE.toFloat())
        Memory.set(MemoryKeys.APR_OFFERINGS, offerings, fleet)
        if (!fleet.faction.getCustomBoolean(Factions.CUSTOM_OFFERS_COMMISSIONS)) {
            Memory.set(MemoryKeys.APR_IGNORE_COM, true, fleet)
        }
        if (fleet.faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
            Memory.set(MemoryKeys.APR_IGNORE_REP, true, fleet)
        }
        if (fleet.faction.getCustomBoolean(Factions.CUSTOM_ALLOWS_TRANSPONDER_OFF_TRADE)) {
            Memory.set(MemoryKeys.APR_IGNORE_TRANSPONDER, true, fleet)
        }
    }

    private fun createFleet(route: RouteManager.RouteData, faction: String, type: String): CampaignFleetAPI? {
        val source = route.market ?: return null
        val weights = getWeights(type)
        val fp = getFP(type)
        val params = FleetsHelper.createFleetParams(
            Helper.random,
            weights,
            fp,
            0f,
            null,
            source,
            null,
            faction,
            type
        )
        val factionParams = FleetsHelper.getStandardFactionParams(faction)
        val result = FleetsHelper.createMultifactionFleet(
            params,
            faction,
            *factionParams
        )
        result.setFaction(faction, true)
        result.name = Helper.sector?.getFaction(faction)?.getFleetTypeName(type) ?: ExternalStrings.DEBUG_NULL
        return result
    }

    private fun getWeights(type: String): Map<FleetsHelper.Category, FleetsHelper.Weight> {
        return when (type) {
            RoiderFleetTypes.NOMAD_MOTHER_FLEET -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID),
                    Pair(FleetsHelper.Category.UTILITY, FleetsHelper.Weight.LOW),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW),
                    Pair(FleetsHelper.Category.LINER, FleetsHelper.Weight.LOW)
                )
            }
            RoiderFleetTypes.NOMAD_MINI_MOTHER_FLEET -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW),
                    Pair(FleetsHelper.Category.LINER, FleetsHelper.Weight.LOW)
                )
            } else -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW),
                )
            }
        }
    }

    private fun getFP(type: String): Float {
        return when (type) {
            RoiderFleetTypes.NOMAD_MOTHER_FLEET -> HEAVY_FP + Misc.random.nextFloat() * HEAVY_FP
            RoiderFleetTypes.NOMAD_MINI_MOTHER_FLEET -> LIGHT_FP + Misc.random.nextFloat() * LIGHT_FP
            else -> LIGHT_FP
        }
    }
}