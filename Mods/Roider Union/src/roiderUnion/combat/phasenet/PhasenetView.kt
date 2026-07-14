package roiderUnion.combat.phasenet

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State
import com.fs.starfarer.api.util.IntervalUtil
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIds
import roiderUnion.helpers.Settings
import java.awt.Color
import java.util.*

class PhasenetView(private val model: PhasenetModel) {
    companion object {
        private const val TOKEN_HULL_NAME = "\$hullName"
        private const val TOKEN_SPEED = "\$speed"

        private const val PHASE_TIME = 0.2f
        private const val PHASE_OPACITY = 0.2f
        private const val PHASENET_SLOT = "PN"
        private const val ARC_SOUND = "roider_system_phasenet_arc"
        private const val ARC_START_SOUND = "roider_system_phasenet_arc_start"
        private const val GLOW_1 = "_glow1"
        private const val GLOW_2 = "_glow2"
        private const val GLOW_1_SPRITE_ID = "wrecker_glow1"
        private const val GLOW_2_SPRITE_ID = "wrecker_glow2"
        private val GLOW_1_COLOR = Color(255, 175, 255, 255)
        private val GLOW_2_COLOR = Color(255, 0, 255, 150)
        private val AFTERIMAGE_COLOR = Color(255, 175, 255, 100)
    }

    private var hasSpawnedFadeInGlow = false
    private var hasSpawnedArcs = false
    private val blinkInterval = IntervalUtil(0.5f, 0.5f)
    private val arcInterval = IntervalUtil(0.05f, 2f)
    private val arcSlotsFired = ArrayList<WeaponSlotAPI>()

    fun getInfoText(): String {
        return when (model.infoState) {
            PhasenetInfoState.ACTIVE -> ExternalStrings.SYSTEM_ACTIVE
            PhasenetInfoState.COOLDOWN -> ExternalStrings.SYSTEM_NONE
            PhasenetInfoState.READY -> ExternalStrings.SYSTEM_READY
            PhasenetInfoState.OUT_OF_RANGE -> ExternalStrings.SYSTEM_OUT_OF_RANGE
            PhasenetInfoState.NO_TARGET -> ExternalStrings.SYSTEM_NO_TARGET
        }
    }

    fun getStatusData(index: Int): ShipSystemStatsScript.StatusData? {
        if (model.target == null || index != 0) return null
        return if (model.target is ShipAPI) {
            val text = ExternalStrings.PHASENET_SHIP_STATUS
                .replace(TOKEN_HULL_NAME, model.hullName)
                .replace(TOKEN_SPEED, model.statusSpeed.toString())
            ShipSystemStatsScript.StatusData(text, false)
        } else {
            val text = ExternalStrings.PHASENET_ASTEROID_STATUS
                .replace(TOKEN_SPEED, model.statusSpeed.toString())
            ShipSystemStatsScript.StatusData(text, false)
        }
    }
    
    fun showEffects(stats: MutableShipStatsAPI, state: State, effectLevel: Float) {
        val ship = stats.entity as? ShipAPI ?: return
        val amount = Helper.combatEngine?.elapsedInLastFrame ?: return
        val shipTarget: ShipAPI? = if (model.target is ShipAPI) model.target as ShipAPI else null
        renderJitter(ship, effectLevel)
        renderFades(ship, state)
        renderAllArcs(ship, state)
        blinkInterval.advance(amount)
        if (blinkInterval.intervalElapsed() && shipTarget != null) {
            renderAfterimage(shipTarget)
        }
        arcInterval.advance(amount)
        if (arcInterval.intervalElapsed() && state != State.OUT) {
            renderArcsRandomly(ship)
        }
    }

    fun resetEffects() {
        hasSpawnedFadeInGlow = false
        hasSpawnedArcs = false
    }

    /**
     * Copied visuals code with permission from MesoTroniK's tiandong_fluxOverrideStats.java
     */
    private fun renderJitter(ship: ShipAPI, effectLevel: Float) {
        var glowDeco1: WeaponAPI? = null
        var glowDeco2: WeaponAPI? = null
        for (weapon in ship.allWeapons) {
            if (weapon.id.endsWith(GLOW_1) && weapon.id.startsWith(RoiderIds.PREFIX)) {
                glowDeco1 = weapon
            }
            if (weapon.id.endsWith(GLOW_2) && weapon.id.startsWith(RoiderIds.PREFIX)) {
                glowDeco2 = weapon
            }
        }
        ship.setJitter(ship, GLOW_1_COLOR, effectLevel / 30f, 2, 20f)
        ship.setJitterUnder(ship, GLOW_1_COLOR, effectLevel / 8f, 4, 30f)
        ship.setJitter(ship, GLOW_2_COLOR, effectLevel / 30f, 6, 30f)
        ship.setJitterUnder(ship, GLOW_2_COLOR, effectLevel / 8f, 10, 50f)
        if (glowDeco1 != null && glowDeco1.animation != null) {
            glowDeco1.sprite.setAdditiveBlend()
            glowDeco1.sprite.color = GLOW_1_COLOR
            glowDeco1.animation.alphaMult = effectLevel
            glowDeco1.animation.frame = 1
            renderWithJitter(glowDeco1.sprite, 0f, 0f, 10f, 1)
        }
        if (glowDeco2 != null && glowDeco2.animation != null) {
            glowDeco2.sprite.setAdditiveBlend()
            glowDeco2.sprite.color = GLOW_2_COLOR
            glowDeco2.animation.alphaMult = effectLevel
            glowDeco2.animation.frame = 1
            renderWithJitter(glowDeco2.sprite, 0f, 0f, 10f, 1)
        }
    }

    private fun renderFades(ship: ShipAPI, state: State) {
        if (state == State.IN && !hasSpawnedFadeInGlow) {
            hasSpawnedFadeInGlow = true
            val sprite1 = Helper.settings?.getSprite(Settings.GRAPHICS_COMBAT, GLOW_1_SPRITE_ID) ?: return
            val sprite2 = Helper.settings?.getSprite(Settings.GRAPHICS_COMBAT, GLOW_2_SPRITE_ID) ?: return
            renderFadeUpAndDown(sprite1, sprite2, ship)
            renderFadeUp(sprite1, sprite2, ship)
        }
    }

    private fun renderFadeUpAndDown(sprite1: SpriteAPI, sprite2: SpriteAPI, ship: ShipAPI) {
        MagicRender.objectspace(
            sprite1, ship, Vector2f(0f, 0f), Vector2f(0f, 0f),
            Vector2f(sprite1.width, sprite1.height), Vector2f(0f, 0f),
            180f, 0f, true, GLOW_1_COLOR, true,
            2f, 1f,
            0f, 0f,
            0f,
            PHASE_TIME * 0.666f, 0f, PHASE_TIME * 0.333f,
            true, CombatEngineLayers.ABOVE_SHIPS_LAYER
        )
        MagicRender.objectspace(
            sprite2, ship, Vector2f(0f, 0f), Vector2f(0f, 0f),
            Vector2f(sprite2.width, sprite2.height), Vector2f(0f, 0f),
            180f, 0f, true, GLOW_2_COLOR, true,
            2f, 1f,
            0f, 0f,
            0f,
            PHASE_TIME * 0.666f, 0f, PHASE_TIME * 0.333f,
            true, CombatEngineLayers.ABOVE_SHIPS_LAYER
        )
    }

    private fun renderFadeUp(sprite1: SpriteAPI, sprite2: SpriteAPI, ship: ShipAPI) {
        MagicRender.objectspace(
            sprite1, ship, Vector2f(0f, 0f), Vector2f(0f, 0f),
            Vector2f(sprite1.width, sprite1.height), Vector2f(0f, 0f),
            180f, 0f, true, GLOW_1_COLOR, true,
            2f, 1f,
            0f, 0f,
            0f,
            PHASE_TIME, 0f, 0.1f,
            true, CombatEngineLayers.ABOVE_SHIPS_LAYER
        )
        MagicRender.objectspace(
            sprite2, ship, Vector2f(0f, 0f), Vector2f(0f, 0f),
            Vector2f(sprite2.width, sprite2.height), Vector2f(0f, 0f),
            180f, 0f, true, GLOW_2_COLOR, true,
            2f, 1f,
            0f, 0f,
            0f,
            PHASE_TIME, 0f, 0.1f,
            true, CombatEngineLayers.ABOVE_SHIPS_LAYER
        )
    }

    private fun renderWithJitter(s: SpriteAPI, x: Float, y: Float, maxJitter: Float, numCopies: Int) {
        for (i in 0 until numCopies) {
            val jv = Vector2f()
            jv.x = Math.random().toFloat() * maxJitter - maxJitter / 2f
            jv.y = Math.random().toFloat() * maxJitter - maxJitter / 2f
            s.render(x + jv.x, y + jv.y)
        }
    }

    private fun renderAllArcs(ship: ShipAPI, state: State) {
        if (state == State.IN && !hasSpawnedArcs) {
            hasSpawnedArcs = true
            val arcEmitters: List<WeaponSlotAPI> = findArcSlots(ship)
            for (slot in arcEmitters) {
                renderEmitterArc(ship, slot)
            }
        }
    }

    private fun findArcSlots(ship: ShipAPI): List<WeaponSlotAPI> {
        val arcEmitters: MutableList<WeaponSlotAPI> = ArrayList<WeaponSlotAPI>()
        for (slot in ship.hullSpec.allWeaponSlotsCopy) {
            if (slot.isSystemSlot) {
                if (slot.id.startsWith(PHASENET_SLOT)) arcEmitters.add(slot)
            }
        }
        return arcEmitters
    }

    private fun renderAfterimage(shipTarget: ShipAPI) {
        shipTarget.addAfterimage(
            GLOW_1_COLOR,
            0f,
            0f,
            model.targetVector.x * model.forceMult,
            model.targetVector.y * model.forceMult,
            5f, 0.1f, 0.5f, 0.5f, true,
            false, false
        )
        shipTarget.addAfterimage(
            AFTERIMAGE_COLOR,
            0f,
            0f,
            0f,
            0f,
            5f, 0.1f, 0.5f, 0.1f, true,
            true, true
        )
    }

    private fun renderArcsRandomly(ship: ShipAPI) {
        val arcEmitters: List<WeaponSlotAPI> = findArcSlots(ship)
        val rand = Random()
        var slot: WeaponSlotAPI = arcEmitters[rand.nextInt(arcEmitters.size)]
        if (arcEmitters.size > 1) {
            while (arcSlotsFired.contains(slot)) {
                slot = arcEmitters[rand.nextInt(arcEmitters.size)]
            }
        }
        if (arcSlotsFired.size == arcEmitters.size - 1) {
            arcSlotsFired.clear()
        }
        arcSlotsFired.add(slot)
        renderEmitterArc(ship, slot)
    }

    private fun renderEmitterArc(ship: ShipAPI, slot: WeaponSlotAPI) {
        val slotLoc: Vector2f = slot.computePosition(ship)
        val arc = Helper.combatEngine?.spawnEmpArcVisual(
            slotLoc,
            ship,
            model.focusPoint,
            ship,
            40f,
            GLOW_1_COLOR,
            Color.WHITE
        )
        arc?.coreWidthOverride = 30f
        arc?.setSingleFlickerMode()
        Helper.soundPlayer?.playSound(ARC_START_SOUND, 1f, 1f, slotLoc, ship.velocity)
        if (model.target?.location == null) {
            Helper.soundPlayer?.playSound(ARC_SOUND, 1f, 1f, model.focusPoint, ship.velocity)
        } else {
            val arc2 = Helper.combatEngine?.spawnEmpArcVisual(
                model.focusPoint,
                ship,
                model.target?.location,
                model.target,
                40f,
                GLOW_1_COLOR,
                Color.WHITE
            )
            arc2?.coreWidthOverride = 30f
            Helper.soundPlayer?.playSound(ARC_SOUND, 1f, 1f, model.target?.location, model.target?.velocity)
        }
    }

}
