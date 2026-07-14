package roiderUnion.ids.systems

import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.*

object AtkaIds {
    val SYSTEM_NAME = ExternalStrings.ATKA
    const val BACKGROUND = Backgrounds.STARS_1
    val PRIMARY = EntityIds("roider_atka_solo", ExternalStrings.ATKA_PRIMARY, StarTypes.RED_DWARF)

    val JP_INNER = EntityIds("roider_atka_inner_jump", ExternalStrings.INNER_SYSTEM_JP)
    val JP_OUTER = EntityIds("roider_atka_outer_jump", ExternalStrings.OUTER_SYSTEM_JP)
    val JP_FRINGE = EntityIds("roider_atka_fringe_jump", ExternalStrings.FRINGE_SYSTEM_JP)

    val SP_KOROVIN_L4 = EntityIds("roider_sp1", null, Entities.COMM_RELAY_MAKESHIFT, RoiderFactions.ROIDER_UNION)
    val SP_OUTER = EntityIds("roider_sp2", null, Entities.NAV_BUOY_MAKESHIFT, Factions.PIRATES)

    val KOROVIN = EntityIds(
        "roider_korovin",
        ExternalStrings.KOROVIN,
        RoiderPlanets.LAVA_MINOR,
        RoiderFactions.ROIDER_UNION
    )
    val ROCKPIPER_PERCH = EntityIds(
        "roider_rockpiperPerch",
        ExternalStrings.ROCKPIPER_PERCH,
        RoiderIds.Entities.ROCKPIPER_PERCH,
        RoiderFactions.ROIDER_UNION
    )
    val COLD_ROCK = EntityIds(
        "roider_coldRock",
        ExternalStrings.COLD_ROCK,
        RoiderPlanets.FROZEN_3,
        Factions.PIRATES
    )
    val COLD_ROCK_BASTION = EntityIds(
        "roider_coldRock_bastion",
        ExternalStrings.COLD_ROCK_BASTION,
        RoiderIds.Entities.SIDE_STATION_6,
        Factions.PIRATES
    )

    val AKUTAN = EntityIds("roider_unalaskaI", ExternalStrings.AKUTAN, RoiderPlanets.GAS_GIANT)
    val KALEKHTA = EntityIds("roider_kalekhta", ExternalStrings.KALEKHTA, RoiderPlanets.ICE_GIANT)
    val PRIEST = EntityIds("roider_priest", ExternalStrings.PRIEST, RoiderPlanets.BARREN)
    val MAKUSHIN = EntityIds("roider_makushin", ExternalStrings.MAKUSHIN, RoiderPlanets.CRYOVOLCANIC)

    val POINT_CHEERFUL = EntityIds("", ExternalStrings.POINT_CHEERFUL, Terrain.ASTEROID_BELT)
    val OUTER_RIM = EntityIds("", ExternalStrings.ATKA_OUTER_RIM, Terrain.ASTEROID_BELT)
}