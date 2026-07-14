package roiderUnion.world

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIds.Music
import scripts.campaign.bases.Roider_RoiderBaseIntelV2

/**
 * Author: SafariJohn
 */
class SystemMusicScript : EveryFrameScript {
    companion object {
        fun alias(x: XStream) {
            val jClass = SystemMusicScript::class.java
            x.alias(Aliases.SYSMUSIC,jClass)
            x.aliasAttribute(jClass, "index", "i")
        }
    }

    private var index = 0

    override fun advance(amount: Float) {
        if (index >= (Helper.sector?.starSystems?.size ?: 0)) index = 0
        val system = Helper.sector?.starSystems?.get(index++) ?: return
        val music = Memory.getNullable(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, system, { it is String }, { null }) as? String
        if (music != null && Music.ROIDER_SYSTEM_BG != music) return
        if (isRoiderSystem(system)) {
            Memory.set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, Music.ROIDER_SYSTEM_BG, system)
        } else {
            Memory.unset(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, system)
        }
    }

    private fun isRoiderSystem(system: StarSystemAPI): Boolean {
        var biggest = 0
        var biggestRoider = 0
        Helper.sector?.economy?.getMarkets(system)?.forEach {
            biggest = it.size.coerceAtLeast(biggest)
            val isRoider = it.factionId == RoiderFactions.ROIDER_UNION || it.id.startsWith(Roider_RoiderBaseIntelV2.MARKET_PREFIX)
            if (isRoider) biggestRoider = it.size.coerceAtLeast(biggestRoider)
        }
        return biggest > 0 && biggestRoider == biggest
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true
}