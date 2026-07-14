package roiderUnion.world

import org.lazywizard.lazylib.MathUtils
import roiderUnion.ids.RingTexture
import java.awt.Color

class RingLayout(
    texture: RingTexture,
    index: Int,
    val middle: Float,
    val orbitDays: Float = SectorGenHelper.calculateOrbitDays(middle),
    val color: Color = Color.WHITE
) {
    val category = texture.category
    val texture = texture.textureId
    val textureWidth = texture.width
    val index = MathUtils.clamp(index, 0, texture.maxIndex)
    val width: Float = textureWidth
}