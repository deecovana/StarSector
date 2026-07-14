package scripts.campaign.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.BaseEventManager
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import roiderUnion.world.SectorGen
import java.util.*

class Roider_RoiderBaseManager : BaseEventManager() {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_RoiderBaseManager::class.java, "start", "s")
            x.aliasAttribute(Roider_RoiderBaseManager::class.java, "extraDays", "e")
            x.aliasAttribute(Roider_RoiderBaseManager::class.java, "numDestroyed", "d")
            x.aliasAttribute(Roider_RoiderBaseManager::class.java, "random", "r")
        }

        const val KEY = "\$roider_roiderBaseManager"
        const val RUMOR_HERE = "\$roider_baseRumorHere"
        val baseInterval = 10f
        const val CHECK_PROB = 0.5f
        val instance: Roider_RoiderBaseManager
            get() {
                val test = Global.getSector().memoryWithoutUpdate[KEY]
                return test as Roider_RoiderBaseManager
            }

        fun genBaseUseTimeout(): Float {
            return 120f + 60f * Math.random().toFloat()
        }

        fun markRecentlyUsedForBase(system: StarSystemAPI?) {
            if (system != null && system.center != null) {
                system.center.memoryWithoutUpdate
                    .set(PirateBaseManager.RECENTLY_USED_FOR_BASE, true, genBaseUseTimeout())
            }
        }
    }

    private var start: Long = 0
    var extraDays = 0f
    var numDestroyed = 0
    private var random = Random()

    init {
        Global.getSector().memoryWithoutUpdate[KEY] = this
        start = Global.getSector().clock.timestamp
    }

    override fun getMinConcurrent(): Int {
        return 0
    }

    override fun getMaxConcurrent(): Int {
        return 0
    }

    override fun createEvent(): EveryFrameScript? {
        if (random.nextFloat() < CHECK_PROB) return null
        val system: StarSystemAPI = SectorGen.pickSystemForRoiderBase() ?: return null
        val tier: Roider_RoiderBaseIntelV2.RoiderBaseTier = pickTier()
        val factionId = Factions.INDEPENDENT
        var intel: Roider_RoiderBaseIntelV2? = Roider_RoiderBaseIntelV2(system, factionId, tier)
        intel!!.init()
        if (intel.isDone) intel = null
        return intel
    }

    val daysSinceStart: Float
        get() {
            var days = Global.getSector().clock.getElapsedDaysSince(start) + extraDays
            if (Misc.isFastStartExplorer()) {
                days += 180f - 30f
            } else if (Misc.isFastStart()) {
                days += 180f + 60f
            }
            return days
        }

    /**
     * 0 at six months (depending on start option chosen), goes up to 1 two years later.
     * @return
     */
    val standardTimeFactor: Float
        get() {
            var timeFactor = (instance.daysSinceStart - 180f) / (365f * 2f)
            if (timeFactor < 0) timeFactor = 0f
            if (timeFactor > 1) timeFactor = 1f
            return timeFactor
        }

    protected fun pickTier(): Roider_RoiderBaseIntelV2.RoiderBaseTier {
        var days = daysSinceStart
        days += (numDestroyed * 200).toFloat()
        val picker = WeightedRandomPicker<Roider_RoiderBaseIntelV2.RoiderBaseTier>()
        if (days < 360) {
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_1_1MODULE, 10f)
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_2_1MODULE, 10f)
        } else if (days < 720f) {
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_2_1MODULE, 10f)
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_3_2MODULE, 10f)
        } else if (days < 1080f) {
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_3_2MODULE, 10f)
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_4_3MODULE, 10f)
        } else {
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_3_2MODULE, 10f)
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_4_3MODULE, 10f)
            picker.add(Roider_RoiderBaseIntelV2.RoiderBaseTier.TIER_5_3MODULE, 10f)
        }


//		if (true) {
//			picker.clear();
//			picker.add(PirateBaseTier.TIER_1_1MODULE, 10f);
//			picker.add(PirateBaseTier.TIER_2_1MODULE, 10f);
//			picker.add(PirateBaseTier.TIER_3_2MODULE, 10f);
//			picker.add(PirateBaseTier.TIER_4_3MODULE, 10f);
//			picker.add(PirateBaseTier.TIER_5_3MODULE, 10f);
//		}
        return picker.pick()
    }

    fun incrDestroyed() {
        numDestroyed++
    }
}