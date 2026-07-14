package roiderUnion.world

class FieldLayout(
    val name: String?,
    val minRadius: Float,
    val maxRadius: Float,
    val angle: Float,
    val orbitRadus: Float,
    val orbitDays: Float = SectorGenHelper.calculateOrbitDays(orbitRadus)
)