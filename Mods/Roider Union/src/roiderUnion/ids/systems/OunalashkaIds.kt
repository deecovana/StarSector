package roiderUnion.ids.systems

import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.*

object OunalashkaIds {
    val SYSTEM_NAME = ExternalStrings.OUNALASHKA
    val AGE = StarAge.AVERAGE
    const val BACKGROUND = Backgrounds.NEBULA_2

    val PRIMARY = EntityIds("roider_ounalashka", ExternalStrings.OUNALASHKA_PRIMARY, StarTypes.RED_DWARF)

    val JP = EntityIds("roider_ounalashka_jump", ExternalStrings.INNER_SYSTEM_JP)

    val MAGGIES = EntityIds(
        "roider_maggies",
        ExternalStrings.MAGGIES,
        RoiderIds.Entities.SIDE_STATION_5,
        Factions.PIRATES
    )
    val AMAKNAK = EntityIds("roider_amaknak", ExternalStrings.AMAKNAK, RoiderPlanets.BARREN_DESERT)
    val DUTCH_HARBOR = EntityIds(
        "roider_dutchHarbor",
        ExternalStrings.DUTCH_HARBOR,
        RoiderIds.Entities.SIDE_STATION_6,
        Factions.INDEPENDENT
    )

    val OGLODAK = EntityIds("roider_oglodak", ExternalStrings.OGLODAK, RoiderPlanets.GAS_GIANT)
    val ROUND = EntityIds("roider_oglodakI", ExternalStrings.ROUND, RoiderPlanets.BARREN)

    val COMM_RELAY = EntityIds("roider_sp1", null, Entities.COMM_RELAY_MAKESHIFT, Factions.INDEPENDENT)

    val GATE = EntityIds("roider_ounalashka_gate", ExternalStrings.OUNALASHKA_GATE, Entities.INACTIVE_GATE)
}