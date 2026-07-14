package roiderUnion.nomads.minefields

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.GenericFieldItemSprite
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import roiderUnion.helpers.Helper
import java.awt.Color

/**
 * Author: SafariJohn
 *
 * Still too many magic numbers in this class.
 */
class MinefieldItemSprite(
    entity: SectorEntityToken?,
    spriteCat: String?, spriteKey: String?,
    glowCat: String?, glowKey: String?,
    cellSize: Float, size: Float, spawnRadius: Float
) : GenericFieldItemSprite(entity, spriteCat, spriteKey, cellSize, size, spawnRadius) {
    companion object {
        const val GLOW_FREQUENCY = 0.3f // on/off cycles per second
        const val COLOUR_CHANGE_RANGE = 300f
        const val MAX_FREQUENCY = 2f
        const val MAX_ROTATION = 5f
        const val MIN_ROTATION = -5f
        const val STARTING_FACING_MAX = 360f
        val GLOW_COLOR = Color(255, 30, 0, 255)
        val FADE_COLOR = Color(20, 255, 20, 255)
    }

    private var pause = false
    private var phase = 0f
    private var freqMult = 1f
    private var rotation: Float

    init {
        freqMult = Helper.random.nextFloat() * MAX_FREQUENCY
        rotation = MAX_ROTATION - Helper.random.nextFloat() * (MAX_ROTATION - MIN_ROTATION)
        facing = Helper.random.nextFloat() * STARTING_FACING_MAX
        glow = Helper.settings?.getSprite(glowCat, glowKey)

        val cols = 1
        val rows = 1
        val cellX = Helper.random.nextFloat() * cols
        val cellY = Helper.random.nextFloat() * rows
        val ctw = sprite.textureWidth / cols.toFloat()
        val cth = sprite.textureHeight / rows.toFloat()
        if (glow != null) {
            glow.setTexX(cellX * ctw)
            glow.setTexY(cellY * cth)
            glow.setTexWidth(ctw)
            glow.setTexHeight(cth)
            glow.setSize(size, size)
            glow.color = GLOW_COLOR
        }
    }

    fun fadeOut() {
        fader.fadeOut()
        glow.color = FADE_COLOR
    }

    override fun render(alphaMult: Float) {
        var aMult = alphaMult
        if (aMult <= 0) return
        val lightSource = entity.lightSource
        if (lightSource != null && entity.lightColor != null) {
            sprite.color = entity.lightColor
        } else {
            sprite.color = Color.white
        }
        sprite.angle = facing - 90f
        sprite.setNormalBlend()
        sprite.alphaMult = aMult * fader.brightness
        sprite.renderAtCenter(loc.x, loc.y)
        aMult *= entity.sensorFaderBrightness
        aMult *= entity.sensorContactFaderBrightness
        if (aMult <= 0f) return
        if (lightSource != null && !entity.lightSource.hasTag(Tags.AMBIENT_LS)) {
            val w = shadowMask.width * 1.41f

            // clear out destination alpha in area we care about
            GL11.glColorMask(false, false, false, true)
            GL11.glPushMatrix()
            GL11.glTranslatef(loc.x, loc.y, 0f)
            Misc.renderQuadAlpha(0 - w / 2f - 1f, 0 - w / 2f - 1f, w + 2f, w + 2f, Misc.zeroColor, 0f)
            GL11.glPopMatrix()
            sprite.setBlendFunc(GL11.GL_ONE, GL11.GL_ZERO)
            sprite.renderAtCenter(loc.x, loc.y)
            val lightDir = Misc.getAngleInDegreesStrict(entity.location, lightSource.location)
            shadowMask.alphaMult = aMult
            shadowMask.angle = lightDir
            shadowMask.setBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA)
            shadowMask.renderAtCenter(loc.x, loc.y)
            GL11.glColorMask(true, true, true, false)
            shadowMask.setBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA)
            shadowMask.renderAtCenter(loc.x, loc.y)
        }

        //if(!pause){
        var glowAlpha = 0f
        if (phase < 0.5f) glowAlpha = phase * 2f
        if (phase >= 0.5f) glowAlpha = 1f - (phase - 0.5f) * 2f
        val glowAngle1 = (phase * 1.3f % 1 - 0.5f) * 12f
        val glowAngle2 = (phase * 1.9f % 1 - 0.5f) * 12f

//            glow.setSize(radius*2, radius*2);
        glow.setSize(width, height)
        glow.alphaMult = aMult * glowAlpha * fader.brightness
        glow.setAdditiveBlend()
        glow.angle = facing - 90f + glowAngle1
        glow.renderAtCenter(loc.x, loc.y)
        glow.angle = facing - 90f + glowAngle2
        glow.alphaMult = aMult * glowAlpha * 0.5f * fader.brightness
        glow.renderAtCenter(loc.x, loc.y)
        // }
    }

    override fun advance(days: Float) {
        super.advance(days)
        val amount = Global.getSector().clock.convertToSeconds(days)
        facing += rotation * amount

        //---------
        phase += amount * GLOW_FREQUENCY * freqMult
        while (phase > 1) {
            pause = !pause
            phase--
        }
    }
}