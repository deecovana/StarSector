package roiderUnion.roidMining

class RoidPollResult {
    private val roidCounts = mutableMapOf(
        Pair(RoidType.ICE, 0),
        Pair(RoidType.DUST, 0),
        Pair(RoidType.ROCK, 0),
        Pair(RoidType.METAL, 0),
        Pair(RoidType.PLUTON, 0),
        Pair(RoidType.NULL, 0)
    )
    val percents: Map<RoidType, Float>
        get() {
            val total = roidCounts.map { it.value }.sum()
            return roidCounts.map { Pair(it.key, (it.value / total.toFloat())) }.toMap()
        }
    private val values = mutableMapOf<RoidType, MutableList<Float>>(
        Pair(RoidType.ICE, mutableListOf()),
        Pair(RoidType.DUST, mutableListOf()),
        Pair(RoidType.ROCK, mutableListOf()),
        Pair(RoidType.METAL, mutableListOf()),
        Pair(RoidType.PLUTON, mutableListOf()),
        Pair(RoidType.NULL, mutableListOf())
    )
    val averageValues: Map<RoidType, Float>
        get() {
            return values.map { Pair(it.key, it.value.average().toFloat()) }.toMap()
        }
    fun minValue(type: RoidType): Float = values[type]?.minOrNull() ?: 0f
    fun maxValue(type: RoidType): Float = values[type]?.maxOrNull() ?: 0f
    fun addRoid(type: RoidType, value: Float) {
        roidCounts[type] = roidCounts[type]!! + 1
        values[type]!! += value
    }
}