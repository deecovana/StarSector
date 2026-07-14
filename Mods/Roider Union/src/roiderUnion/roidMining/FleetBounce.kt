package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import org.magiclib.terrain.MagicAsteroidBeltTerrainPlugin
import org.magiclib.util.MagicTxt
import roiderUnion.helpers.Helper
import java.awt.Color

class FleetBounce(private val asteroid: SectorEntityToken, private val fleet: CampaignFleetAPI, target: FleetMemberAPI?, damageMult: Float, impactAngle: Float) : EveryFrameScript {
    companion object {
        const val DURATION_SECONDS = 0.2f
    }

    private var elapsed = 0f
    private val dV: Vector2f

    init {
        val dealDamage = target != null
        val forceMod = fleet.commanderStats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_FORCE)
            ?: MutableStat(1f)
        val mult = Misc.getFleetRadiusTerrainEffectMult(fleet)
        AsteroidBouncer.playEffects(fleet, asteroid, dealDamage, mult)
        if (fleet.isInCurrentLocation) {
            if (dealDamage) applyDamage(target!!, damageMult)
            if (!dealDamage && fleet.isPlayerFleet) {
                Helper.sector?.campaignUI?.addMessage(
                    MagicTxt.getString("af_damage3") + " test",
                    Misc.getNegativeHighlightColor()
                )
            }
        }
        dV = Misc.getUnitVectorAtDegreeAngle(impactAngle)
        val impact = (asteroid.velocity.length() + fleet.velocity.length()) * 2f * forceMod.modifiedValue
        val minImpact = (fleet.acceleration + asteroid.velocity.length()) * forceMod.modifiedValue
        dV.scale(impact.coerceAtLeast(minImpact))
        dV.scale(1f / DURATION_SECONDS)
    }

    private fun applyDamage(target: FleetMemberAPI, damageMult: Float) {
        Misc.applyDamage(
            target,
            null,
            damageMult.coerceAtLeast(1f),
            true,
            "asteroid_impact",
            MagicTxt.getString("af_damage1") + " test",
            true,
            null,
            target.shipName + MagicTxt.getString("af_damage2")
        )
    }

    override fun advance(amount: Float) {
        fleet.orbit = null
        val v = fleet.velocity
        fleet.setVelocity(v.x + dV.x * amount, v.y + dV.y * amount)
        elapsed += amount
    }

    override fun isDone(): Boolean {
        val durationMod = fleet.commanderStats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DURATION)
                ?: MutableStat(1f)
        return elapsed >= DURATION_SECONDS * durationMod.modifiedValue
    }

    override fun runWhilePaused(): Boolean = false
}