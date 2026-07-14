package roiderUnion.nomads.minefields

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.GenericFieldItemManager

/**
 * Author: SafariJohn
 */
class MinefieldEntityItemManager(entity: SectorEntityToken?) : GenericFieldItemManager(entity) {
    companion object {
        const val DEFAULT_CELL_SIZE = 88
        const val DEFAULT_MIN_MINE_SIZE = 8f
        const val DEFAULT_MAX_MINE_SIZE = 24f
        const val SPAWN_RADIUS_MULT = 0.75f
    }

    var glowCategory: String? = null
    var glowKey: String? = null
    var fade = false

    init {
        cellSize = DEFAULT_CELL_SIZE
        minSize = DEFAULT_MIN_MINE_SIZE
        maxSize = DEFAULT_MAX_MINE_SIZE
    }

    override fun addPiecesToMax() {
        if (fade) return
        while (items.size < numPieces) {
            val size = minSize + (maxSize - minSize) * Math.random().toFloat()
            items.add(
                MinefieldItemSprite(
                    entity, category, key, glowCategory, glowKey, cellSize.toFloat(), size,
                    entity.radius * SPAWN_RADIUS_MULT
                )
            )
        }
    }

    fun fadeOut() {
        fade = true
        for (item in items) {
            (item as MinefieldItemSprite).fadeOut()
        }
    }

    val isFadedOut: Boolean
        get() = items?.all { it.isDone } ?: false
}