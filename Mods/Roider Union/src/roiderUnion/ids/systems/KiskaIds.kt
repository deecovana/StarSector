package roiderUnion.ids.systems

import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.*

object KiskaIds {
    const val SYSTEM_ID = "roider_kiska"
    val SYSTEM_NAME = ExternalStrings.KISKA
    val AGE = StarAge.YOUNG

    val JP_INNER = EntityIds("roider_kiska_inner_jump", ExternalStrings.INNER_JP)
    val JP_OUTER = EntityIds("roider_kiska_outer_jump", ExternalStrings.FRINGE_SYSTEM_JP)

    val DRUZHININS = EntityIds(
        "roider_druzhininsAnchorage",
        ExternalStrings.DRUZHININS,
        RoiderIds.Entities.MIDLINE_2,
        RoiderFactions.ROIDER_UNION
    )
    val THREE_SISTERS_STATION = EntityIds(
        "roider_threeSisters",
        ExternalStrings.THREE_SISTERS,
        RoiderIds.Entities.MINING_STATION,
        Factions.INDEPENDENT
    )
    val ZIN_SHIPYARD = EntityIds(
        "roider_zinStation",
        ExternalStrings.ZIN,
        RoiderIds.Entities.SIDE_STATION_0,
        Factions.TRITACHYON
    )

    val NAV_BUOY = EntityIds("roider_sp1", null, Entities.NAV_BUOY_MAKESHIFT, RoiderFactions.ROIDER_UNION)
    val COMM_RELAY = EntityIds("roider_sp2", null, Entities.COMM_RELAY_MAKESHIFT, RoiderFactions.ROIDER_UNION)

    val LORAN = EntityIds("roider_loran", ExternalStrings.LORAN, RoiderPlanets.GAS_GIANT)
    val GEE = EntityIds("roider_gee", ExternalStrings.GEE, RoiderPlanets.BARREN)
}