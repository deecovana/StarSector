package roiderUnion.world

import roiderUnion.ids.EntityIds
import roiderUnion.world.SectorGenHelper

class EntityLayout(
    ids: EntityIds,
    val radius: Float = 0f,
    val angle: Float = 0f,
    val orbitRadius: Float = 0f,
    val orbitDays: Float = SectorGenHelper.calculateOrbitDays(orbitRadius),
    val coronaRadius: Float = 0f
) {
    val id = ids.id
    val name = ids.name
    val type = ids.type
    val faction = ids.faction
}