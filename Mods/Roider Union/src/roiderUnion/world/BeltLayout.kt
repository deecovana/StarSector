package roiderUnion.world

class BeltLayout(
    val orbitRadius: Float,
    val width: Float,
    val minDays: Float = SectorGenHelper.calculateOrbitDays(orbitRadius),
    val maxDays: Float = minDays * 1.5f,
    val name: String? = null
)