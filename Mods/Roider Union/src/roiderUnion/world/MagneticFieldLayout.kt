package roiderUnion.world

class MagneticFieldLayout(
    val innerRadius: Float,
    val outerRadius: Float,
    orbitDays: Float? = null
) {
    val width = outerRadius - innerRadius
    val middle = innerRadius + width / 2f
    val orbitDays: Float

    init {
        this.orbitDays = orbitDays ?: SectorGenHelper.calculateOrbitDays(middle)
    }
}
