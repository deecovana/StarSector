package roiderUnion.nomads.minefields

import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.lazywizard.lazylib.MathUtils
import roiderUnion.helpers.Settings
import roiderUnion.ids.Aliases

/**
 * Author: SafariJohn
 */
class MajorLootStashPlugin : BaseCustomEntityPlugin() {
    companion object {
        const val CELL_SIZE = 88
        const val MIN_MINE_SIZE = 2f
        const val MAX_MINE_SIZE = 6f
        const val MIN_MINES = 5
        const val MAX_MINES = 40
        const val MINE_CAPACITY_DIVISOR = 4f
        const val RENDER_PADDING = 100f

        fun alias(x: XStream) {
            x.alias(Aliases.MAJLOOT, MajorLootStashPlugin::class.java)
        }
    }

    @Transient
    private lateinit var manager: MinefieldEntityItemManager

    override fun init(entity: SectorEntityToken, pluginParams: Any?) {
        super.init(entity, pluginParams)
        readResolve()
    }

    fun readResolve(): Any {
        manager = MinefieldEntityItemManager(entity)
        manager.category = Settings.LOOT_STASH_MINE_IMAGES
        manager.key = Settings.LOOT_STASH_MINE
        manager.glowCategory = Settings.LOOT_STASH_MINE_IMAGES
        manager.glowKey = Settings.LOOT_STASH_MINE_GLOW
        manager.cellSize = CELL_SIZE
        manager.minSize = MIN_MINE_SIZE
        manager.maxSize = MAX_MINE_SIZE
        return this
    }

    override fun advance(amount: Float) {
        if (manager.isFadedOut) {
            Misc.fadeAndExpire(entity)
            return
        }
        if (entity.isInCurrentLocation) {
            val totalCapacity: Float = entity.radius
            manager.numPieces = MathUtils.clamp((totalCapacity / MINE_CAPACITY_DIVISOR).toInt(), MIN_MINES, MAX_MINES)
        }
        manager.advance(amount)
    }

    override fun getRenderRange(): Float = entity.radius + RENDER_PADDING

    override fun render(layer: CampaignEngineLayers, viewport: ViewportAPI) = manager.render(layer, viewport)

    fun fadeOut() = manager.fadeOut()
    fun isFadedOut(): Boolean = manager.isFadedOut
}