package roiderUnion.ids.systems

import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Planets
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import roiderUnion.ids.Backgrounds
import roiderUnion.ids.EntityIds
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.RoiderIds

object AttuIds {
    val SYSTEM_NAME = ExternalStrings.ATTU
    const val BACKGROUND = Backgrounds.STARS_1
    val PRIMARY = EntityIds("roider_attu", ExternalStrings.ATTU_PRIMARY, StarTypes.ORANGE)
    val SECONDARY = EntityIds("roider_agattu", ExternalStrings.AGATTU, StarTypes.BROWN_DWARF)

    val JP_INNER = EntityIds("roider_attu_inner_jump", ExternalStrings.INNER_SYSTEM_JP)
    val JP_OUTER = EntityIds("roider_attu_outer_jump", ExternalStrings.OUTER_SYSTEM_JP)

    val COMM_RELAY = EntityIds("roider_sp1", null, Entities.COMM_RELAY_MAKESHIFT, Factions.HEGEMONY)

    val HOLTZ = EntityIds(
        "roider_attuStation",
        ExternalStrings.HOLTZ,
        RoiderIds.Entities.MINING_STATION,
        Factions.HEGEMONY
    )

    val MOFFET = EntityIds("roider_moffet", ExternalStrings.MOFFET, Planets.PLANET_LAVA)
}