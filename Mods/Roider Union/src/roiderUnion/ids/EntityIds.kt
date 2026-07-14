package roiderUnion.ids

class EntityIds(
    val id: String,
    val name: String?,
    val type: String = "",
    val faction: String? = null
) {
    companion object {
        val EMPTY = EntityIds("", null)
    }
}