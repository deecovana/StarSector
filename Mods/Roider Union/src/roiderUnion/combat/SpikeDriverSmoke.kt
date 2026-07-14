package roiderUnion.combat

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import java.awt.Color

/**
 * Spawns extra smoke when a Spike Driver fires or is on cooldown
 *
 * Modified by SafariJohn to not spawn certain effects on certain weapon slots for certain hulls
 *
 * @author Nicke535
 */
class SpikeDriverSmoke : EveryFrameWeaponEffectPlugin {
    companion object {
        private const val DEFAULT = "default"
        private const val EXHAUST = "EXHAUST"
        private const val EXHAUST_PUFF = "EXHAUST_PUFF"

        // These are the rear effects
        private val REAR_EFFECTS: MutableList<String> = ArrayList()

        init {
            REAR_EFFECTS.add(EXHAUST_PUFF)
            REAR_EFFECTS.add(EXHAUST)
        }

        /*

        HOW TO USE:
        USED_IDS specifies which IDs to use for the rest of the script; any ID is valid EXCEPT the unique ID DEFAULT. Each ID should only be used once on the same weapon
        The script will spawn one particle "system" for each ID in this list, with the specific attributes of that ID.

        All the different Maps<> specify the attributes of each of the particle "systems"; they MUST have something defined as DEFAULT, and can have specific fields for specific IDs
        in the USED_IDS list; any field not filled in for a specific ID will revert to DEFAULT instead.

    */
        private val USED_IDS: MutableList<String> = ArrayList()

        init {
            USED_IDS.add(EXHAUST_PUFF)
            USED_IDS.add(EXHAUST)
        }

        //The amount of particles spawned immediately when the weapon reaches full charge level
        //  -For projectile weapons, this is when the projectile is actually fired
        //  -For beam weapons, this is when the beam has reached maximum brightness
        private val ON_SHOT_PARTICLE_COUNT: MutableMap<String, Int> = HashMap()

        init {
            ON_SHOT_PARTICLE_COUNT[DEFAULT] = 5
        }

        //How many particles are spawned each second the weapon is firing, on average
        private val PARTICLES_PER_SECOND: MutableMap<String, Float> = HashMap()

        init {
            PARTICLES_PER_SECOND[DEFAULT] = 0f
            PARTICLES_PER_SECOND[EXHAUST] = 25f
        }

        //Does the PARTICLES_PER_SECOND field get multiplied by the weapon's current chargeLevel?
        private val AFFECTED_BY_CHARGELEVEL: MutableMap<String, Boolean> = HashMap()

        init {
            AFFECTED_BY_CHARGELEVEL[DEFAULT] = false
            AFFECTED_BY_CHARGELEVEL[EXHAUST] = true
        }

        //Does the PARTICLE_SIZE fields get multiplied by the weapon's current chargeLevel?
        private val SIZE_AFFECTED_BY_CHARGELEVEL: MutableMap<String, Boolean> = HashMap()

        init {
            SIZE_AFFECTED_BY_CHARGELEVEL[DEFAULT] = false
            SIZE_AFFECTED_BY_CHARGELEVEL[EXHAUST] = true
        }

        //When are the particles spawned (only used for PARTICLES_PER_SECOND)? Valid values are "CHARGEUP", "FIRING", "CHARGEDOWN", "READY" (not on cooldown or firing) and "COOLDOWN".
        //  Multiple of these values can be combined via "-" inbetween; "CHARGEUP-CHARGEDOWN" is for example valid
        private val PARTICLE_SPAWN_MOMENT: MutableMap<String, String> = HashMap()

        init {
            PARTICLE_SPAWN_MOMENT[DEFAULT] = "FIRING"
            PARTICLE_SPAWN_MOMENT[EXHAUST] = "CHARGEDOWN-COOLDOWN"
        }

        //If this is set to true, the particles spawn with regard to *barrel*, not *center*. Only works for ALTERNATING barrel types on weapons: for LINKED barrels you
        //  should instead set up their coordinates manually with PARTICLE_SPAWN_POINT_TURRET and PARTICLE_SPAWN_POINT_HARDPOINT
        private val SPAWN_POINT_ANCHOR_ALTERNATION: MutableMap<String, Boolean> = HashMap()

        init {
            SPAWN_POINT_ANCHOR_ALTERNATION[DEFAULT] = false
        }

        //The position the particles are spawned (or at least where their arc originates when using offsets) compared to their weapon's center [or shot offset, see
        //SPAWN_POINT_ANCHOR_ALTERNATION above], if the weapon is a turret (or HIDDEN)
        private val PARTICLE_SPAWN_POINT_TURRET: MutableMap<String, Vector2f> = HashMap()

        init {
            PARTICLE_SPAWN_POINT_TURRET[DEFAULT] =
                Vector2f(22f, 0f)
            PARTICLE_SPAWN_POINT_TURRET[EXHAUST_PUFF] = Vector2f(0f, -12f)
            PARTICLE_SPAWN_POINT_TURRET[EXHAUST] = Vector2f(0f, -12f)
        }

        //The position the particles are spawned (or at least where their arc originates when using offsets) compared to their weapon's center [or shot offset, see
        //SPAWN_POINT_ANCHOR_ALTERNATION above], if the weapon is a hardpoint
        private val PARTICLE_SPAWN_POINT_HARDPOINT: MutableMap<String, Vector2f> = HashMap()

        init {
            PARTICLE_SPAWN_POINT_HARDPOINT[DEFAULT] = Vector2f(22f, 0f)
            PARTICLE_SPAWN_POINT_HARDPOINT[EXHAUST_PUFF] = Vector2f(0f, -16f)
            PARTICLE_SPAWN_POINT_HARDPOINT[EXHAUST] = Vector2f(0f, -16f)
        }

        //Which kind of particle is spawned (valid values are "EXPLOSION", "SMOOTH", "BRIGHT" and "SMOKE")
        private val PARTICLE_TYPE: MutableMap<String, String> = HashMap()

        init {
            PARTICLE_TYPE[DEFAULT] = "BRIGHT"
            PARTICLE_TYPE[EXHAUST] = "BRIGHT"
        }

        //What color does the particles have?
        private val PARTICLE_COLOR: MutableMap<String, Color> = HashMap()

        init {
            PARTICLE_COLOR[DEFAULT] =
                Color(255, 225, 225, 215)
        }

        //What's the smallest size the particles can have?
        private val PARTICLE_SIZE_MIN: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_SIZE_MIN[DEFAULT] = 5f
            PARTICLE_SIZE_MIN[EXHAUST] = 5f
        }

        //What's the largest size the particles can have?
        private val PARTICLE_SIZE_MAX: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_SIZE_MAX[DEFAULT] = 20f
            PARTICLE_SIZE_MAX[EXHAUST] = 15f
        }

        //What's the lowest velocity a particle can spawn with (can be negative)?
        private val PARTICLE_VELOCITY_MIN: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_VELOCITY_MIN[DEFAULT] = 5f
            PARTICLE_VELOCITY_MIN[EXHAUST] = 100f
        }

        //What's the highest velocity a particle can spawn with (can be negative)?
        private val PARTICLE_VELOCITY_MAX: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_VELOCITY_MAX[DEFAULT] = 50f
            PARTICLE_VELOCITY_MAX[EXHAUST] = 300f
        }

        //The shortest duration a particle will last before completely fading away
        private val PARTICLE_DURATION_MIN: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_DURATION_MIN[DEFAULT] = 0.2f
            PARTICLE_DURATION_MIN[EXHAUST] = 0.05f
        }

        //The longest duration a particle will last before completely fading away
        private val PARTICLE_DURATION_MAX: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_DURATION_MAX[DEFAULT] = 0.85f
            PARTICLE_DURATION_MAX[EXHAUST] = 0.2f
        }

        //The shortest along their velocity vector any individual particle is allowed to spawn (can be negative to spawn behind their origin point)
        private val PARTICLE_OFFSET_MIN: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_OFFSET_MIN[DEFAULT] = 0f
        }

        //The furthest along their velocity vector any individual particle is allowed to spawn (can be negative to spawn behind their origin point)
        private val PARTICLE_OFFSET_MAX: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_OFFSET_MAX[DEFAULT] = 10f
            PARTICLE_OFFSET_MAX[EXHAUST] = 0f
        }

        //The width of the "arc" the particles spawn in; affects both offset and velocity. 360f = full circle, 0f = straight line
        private val PARTICLE_ARC: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_ARC[DEFAULT] = 10f
            PARTICLE_ARC[EXHAUST] = 5f
        }

        //The offset of the "arc" the particles spawn in, compared to the weapon's forward facing.
        //  For example: 90f = the center of the arc is 90 degrees clockwise around the weapon, 0f = the same arc center as the weapon's facing.
        private val PARTICLE_ARC_FACING: MutableMap<String, Float> = HashMap()

        init {
            PARTICLE_ARC_FACING[DEFAULT] = 0f
            PARTICLE_ARC_FACING[EXHAUST_PUFF] = 180f
            PARTICLE_ARC_FACING[EXHAUST] = 180f
        }
    }

    //-----------------------------------------------------------You don't need to touch stuff beyond this point!------------------------------------------------------------
    //These ones are used in-script, so don't touch them!
    private var hasFiredThisCharge = false
    private var currentBarrel = 0
    private var shouldOffsetBarrelExtra = false
    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        if (Helper.anyNull(engine, weapon)) return
        if (engine!!.isPaused) return

        //Determine if this slot prohibits rear effects
        val hullId = weapon!!.ship.hullSpec.hullId
        val weaponSlotId = weapon.slot.id
        var noRearEffects = (PileDriverSmoke.PROHIBITED_REAR.containsKey(hullId)
                && PileDriverSmoke.PROHIBITED_REAR[hullId]?.contains(weaponSlotId) == true)

        //Hidden slots never have rear effects
        noRearEffects = noRearEffects or weapon.slot.isHidden

        //Saves handy variables used later
        var chargeLevel = weapon.chargeLevel
        var sequenceState = "READY"
        if (chargeLevel > 0 && (!weapon.isBeam || weapon.isFiring)) {
            sequenceState = if (chargeLevel >= 1f) {
                "FIRING"
            } else if (!hasFiredThisCharge) {
                "CHARGEUP"
            } else {
                "CHARGEDOWN"
            }
        } else if (weapon.cooldownRemaining > 0) {
            sequenceState = "COOLDOWN"
        }

        //Adjustment for burst beams, since they are a pain
        if (weapon.isBurstBeam && sequenceState.contains("CHARGEDOWN")) {
            chargeLevel = Math.max(
                0f,
                Math.min(
                    Math.abs(weapon.cooldownRemaining - weapon.cooldown) / weapon.spec.derivedStats.burstFireDuration,
                    1f
                )
            )
        }

        //The sequenceStates "CHARGEDOWN" and "COOLDOWN" counts its barrel as 1 earlier than usual, due to code limitations
        shouldOffsetBarrelExtra = sequenceState.contains("CHARGEDOWN") || sequenceState.contains("COOLDOWN")

        //We go through each of our particle systems and handle their particle spawning
        for (ID in USED_IDS) {
            //If prohibited, skip rear effects
            if (noRearEffects && REAR_EFFECTS.contains(ID)) continue

            //Store all the values used for this check, and use default values if we don't have specific values for our ID specified
            //Note that particle count, specifically, is not declared here and is only used in more local if-cases
            var affectedByChargeLevel = AFFECTED_BY_CHARGELEVEL[DEFAULT]!!
            if (AFFECTED_BY_CHARGELEVEL.keys.contains(ID)) {
                affectedByChargeLevel = AFFECTED_BY_CHARGELEVEL[ID]!!
            }
            var sizeAffectedByChargeLevel = SIZE_AFFECTED_BY_CHARGELEVEL[DEFAULT]!!
            if (SIZE_AFFECTED_BY_CHARGELEVEL.keys.contains(ID)) {
                sizeAffectedByChargeLevel = SIZE_AFFECTED_BY_CHARGELEVEL[ID]!!
            }
            var particleSpawnMoment = PARTICLE_SPAWN_MOMENT[DEFAULT]
            if (PARTICLE_SPAWN_MOMENT.keys.contains(ID)) {
                particleSpawnMoment = PARTICLE_SPAWN_MOMENT[ID]
            }
            var spawnPointAnchorAlternation = SPAWN_POINT_ANCHOR_ALTERNATION[DEFAULT]!!
            if (SPAWN_POINT_ANCHOR_ALTERNATION.keys.contains(ID)) {
                spawnPointAnchorAlternation = SPAWN_POINT_ANCHOR_ALTERNATION[ID]!!
            }

            //Here, we only store one value, depending on if we're a hardpoint or not
            var particleSpawnPoint = PARTICLE_SPAWN_POINT_TURRET[DEFAULT]
            if (weapon.slot.isHardpoint) {
                particleSpawnPoint = PARTICLE_SPAWN_POINT_HARDPOINT[DEFAULT]
                if (PARTICLE_SPAWN_POINT_HARDPOINT.keys.contains(ID)) {
                    particleSpawnPoint = PARTICLE_SPAWN_POINT_HARDPOINT[ID]
                }
            } else {
                if (PARTICLE_SPAWN_POINT_TURRET.keys.contains(ID)) {
                    particleSpawnPoint = PARTICLE_SPAWN_POINT_TURRET[ID]
                }
            }
            var particleType = PARTICLE_TYPE[DEFAULT]
            if (PARTICLE_TYPE.keys.contains(ID)) {
                particleType = PARTICLE_TYPE[ID]
            }
            var particleColor = PARTICLE_COLOR[DEFAULT]
            if (PARTICLE_COLOR.keys.contains(ID)) {
                particleColor = PARTICLE_COLOR[ID]
            }
            var particleSizeMin = PARTICLE_SIZE_MIN[DEFAULT]!!
            if (PARTICLE_SIZE_MIN.keys.contains(ID)) {
                particleSizeMin = PARTICLE_SIZE_MIN[ID]!!
            }
            var particleSizeMax = PARTICLE_SIZE_MAX[DEFAULT]!!
            if (PARTICLE_SIZE_MAX.keys.contains(ID)) {
                particleSizeMax = PARTICLE_SIZE_MAX[ID]!!
            }
            var particleVelocityMin = PARTICLE_VELOCITY_MIN[DEFAULT]!!
            if (PARTICLE_VELOCITY_MIN.keys.contains(ID)) {
                particleVelocityMin = PARTICLE_VELOCITY_MIN[ID]!!
            }
            var particleVelocityMax = PARTICLE_VELOCITY_MAX[DEFAULT]!!
            if (PARTICLE_VELOCITY_MAX.keys.contains(ID)) {
                particleVelocityMax = PARTICLE_VELOCITY_MAX[ID]!!
            }
            var particleDurationMin = PARTICLE_DURATION_MIN[DEFAULT]!!
            if (PARTICLE_DURATION_MIN.keys.contains(ID)) {
                particleDurationMin = PARTICLE_DURATION_MIN[ID]!!
            }
            var particleDurationMax = PARTICLE_DURATION_MAX[DEFAULT]!!
            if (PARTICLE_DURATION_MAX.keys.contains(ID)) {
                particleDurationMax = PARTICLE_DURATION_MAX[ID]!!
            }
            var particleOffsetMin = PARTICLE_OFFSET_MIN[DEFAULT]!!
            if (PARTICLE_OFFSET_MIN.keys.contains(ID)) {
                particleOffsetMin = PARTICLE_OFFSET_MIN[ID]!!
            }
            var particleOffsetMax = PARTICLE_OFFSET_MAX[DEFAULT]!!
            if (PARTICLE_OFFSET_MAX.keys.contains(ID)) {
                particleOffsetMax = PARTICLE_OFFSET_MAX[ID]!!
            }
            var particleArc = PARTICLE_ARC[DEFAULT]!!
            if (PARTICLE_ARC.keys.contains(ID)) {
                particleArc = PARTICLE_ARC[ID]!!
            }
            var particleArcFacing = PARTICLE_ARC_FACING[DEFAULT]!!
            if (PARTICLE_ARC_FACING.keys.contains(ID)) {
                particleArcFacing = PARTICLE_ARC_FACING[ID]!!
            }
            //---------------------------------------END OF DECLARATIONS-----------------------------------------

            //First, spawn "on full firing" particles, since those ignore sequence state
            if (chargeLevel >= 1f && !hasFiredThisCharge) {
                //Count spawned particles: only trigger if the spawned particles are more than 0
                var particleCount = ON_SHOT_PARTICLE_COUNT[DEFAULT]!!
                    .toFloat()
                if (ON_SHOT_PARTICLE_COUNT.keys.contains(ID)) {
                    particleCount = ON_SHOT_PARTICLE_COUNT[ID]!!.toFloat()
                }
                if (particleCount > 0) {
                    spawnParticles(
                        engine,
                        weapon,
                        particleCount,
                        particleType,
                        spawnPointAnchorAlternation,
                        particleSpawnPoint,
                        particleColor,
                        particleSizeMin,
                        particleSizeMax,
                        particleVelocityMin,
                        particleVelocityMax,
                        particleDurationMin,
                        particleDurationMax,
                        particleOffsetMin,
                        particleOffsetMax,
                        particleArc,
                        particleArcFacing
                    )
                }
            }

            //Then, we check if we should spawn particles over duration; only spawn if our spawn moment is in the declaration
            if (particleSpawnMoment!!.contains(sequenceState)) {
                //Get how many particles should be spawned this frame
                var particleCount = PARTICLES_PER_SECOND[DEFAULT]!!
                if (PARTICLES_PER_SECOND.keys.contains(ID)) {
                    particleCount = PARTICLES_PER_SECOND[ID]!!
                }
                particleCount *= amount
                if (affectedByChargeLevel && (sequenceState.contains("CHARGEUP") || sequenceState.contains("CHARGEDOWN"))) {
                    particleCount *= chargeLevel
                }
                if (affectedByChargeLevel && sequenceState.contains("COOLDOWN")) {
                    particleCount *= weapon.cooldownRemaining / weapon.cooldown
                }
                if (sizeAffectedByChargeLevel) {
                    particleSizeMin *= chargeLevel
                    particleSizeMax *= chargeLevel
                }

                //Then, if the particle count is greater than 0, we actually spawn the particles
                if (particleCount > 0f) {
                    spawnParticles(
                        engine,
                        weapon,
                        particleCount,
                        particleType,
                        spawnPointAnchorAlternation,
                        particleSpawnPoint,
                        particleColor,
                        particleSizeMin,
                        particleSizeMax,
                        particleVelocityMin,
                        particleVelocityMax,
                        particleDurationMin,
                        particleDurationMax,
                        particleOffsetMin,
                        particleOffsetMax,
                        particleArc,
                        particleArcFacing
                    )
                }
            }
        }

        //If this was our "reached full charge" frame, register that
        if (chargeLevel >= 1f && !hasFiredThisCharge) {
            hasFiredThisCharge = true
        }

        //Increase our current barrel if we have <= 0 chargeLevel OR have ceased firing for now, if we alternate, and have fired at least once since we last increased it
        //Also make sure the barrels "loop around", and reset our hasFired variable
        if (hasFiredThisCharge && (chargeLevel <= 0f || !weapon.isFiring)) {
            hasFiredThisCharge = false
            currentBarrel++

            //We can *technically* have different barrel counts for hardpoints, hiddens and turrets, so take that into account
            var barrelCount = weapon.spec.turretAngleOffsets.size
            if (weapon.slot.isHardpoint) {
                barrelCount = weapon.spec.hardpointAngleOffsets.size
            } else if (weapon.slot.isHidden) {
                barrelCount = weapon.spec.hiddenAngleOffsets.size
            }
            if (currentBarrel >= barrelCount) {
                currentBarrel = 0
            }
        }
    }

    //Shorthand function for actually spawning the particles
    private fun spawnParticles(
        engine: CombatEngineAPI,
        weapon: WeaponAPI,
        count: Float,
        type: String?,
        anchorAlternation: Boolean,
        spawnPoint: Vector2f?,
        color: Color?,
        sizeMin: Float,
        sizeMax: Float,
        velocityMin: Float,
        velocityMax: Float,
        durationMin: Float,
        durationMax: Float,
        offsetMin: Float,
        offsetMax: Float,
        arc: Float,
        arcFacing: Float
    ) {
        //First, ensure we take barrel position into account if we use Anchor Alternation (note that the spawn location is actually rotated 90 degrees wrong, so we invert their x and y values)
        var trueCenterLocation = Vector2f(spawnPoint!!.y, spawnPoint.x)
        var trueArcFacing = arcFacing
        if (anchorAlternation) {
            if (weapon.slot.isHardpoint) {
                trueCenterLocation.x += weapon.spec.hardpointFireOffsets[currentBarrel].x
                trueCenterLocation.y += weapon.spec.hardpointFireOffsets[currentBarrel].y
                trueArcFacing += weapon.spec.hardpointAngleOffsets[currentBarrel]
            } else if (weapon.slot.isTurret) {
                trueCenterLocation.x += weapon.spec.turretFireOffsets[currentBarrel].x
                trueCenterLocation.y += weapon.spec.turretFireOffsets[currentBarrel].y
                trueArcFacing += weapon.spec.turretAngleOffsets[currentBarrel]
            } else {
                trueCenterLocation.x += weapon.spec.hiddenFireOffsets[currentBarrel].x
                trueCenterLocation.y += weapon.spec.hiddenFireOffsets[currentBarrel].y
                trueArcFacing += weapon.spec.hiddenAngleOffsets[currentBarrel]
            }
        }

        //Then, we offset the true position and facing with our weapon's position and facing, while also rotating the position depending on facing
        trueArcFacing += weapon.currAngle
        trueCenterLocation = VectorUtils.rotate(trueCenterLocation, weapon.currAngle, Vector2f(0f, 0f))
        trueCenterLocation.x += weapon.location.x
        trueCenterLocation.y += weapon.location.y

        //Then, we can finally start spawning particles
        var counter = count
        while (Math.random() < counter) {
            //Ticks down the counter
            counter--

            //Gets a velocity for the particle
            val arcPoint = MathUtils.getRandomNumberInRange(trueArcFacing - arc / 2f, trueArcFacing + arc / 2f)
            val velocity = MathUtils.getPointOnCircumference(
                weapon.ship.velocity, MathUtils.getRandomNumberInRange(velocityMin, velocityMax),
                arcPoint
            )

            //Gets a spawn location in the cone, depending on our offsetMin/Max
            val spawnLocation = MathUtils.getPointOnCircumference(
                trueCenterLocation, MathUtils.getRandomNumberInRange(offsetMin, offsetMax),
                arcPoint
            )

            //Gets our duration
            val duration = MathUtils.getRandomNumberInRange(durationMin, durationMax)

            //Gets our size
            val size = MathUtils.getRandomNumberInRange(sizeMin, sizeMax)
            when (type) {
                "SMOOTH" -> engine.addSmoothParticle(spawnLocation, velocity, size, 1f, duration, color)
                "SMOKE" -> engine.addSmokeParticle(spawnLocation, velocity, size, 1f, duration, color)
                "EXPLOSION" -> engine.spawnExplosion(spawnLocation, velocity, color, size, duration)
                else -> engine.addHitParticle(spawnLocation, velocity, size, 10f, duration, color)
            }
        }
    }
}