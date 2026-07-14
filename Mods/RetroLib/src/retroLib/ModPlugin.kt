package retroLib

import com.fs.starfarer.api.BaseModPlugin


class ModPlugin: BaseModPlugin() {
    companion object {
        @Transient
        var hasNexerelin = false

        @Transient
        var hasStarshipLegends = false
    }

    override fun onApplicationLoad() {
        if (!Helper.isModEnabled(ModIds.LAZY_LIB)) throw RuntimeException("RetroLib requires LazyLib!") // extern
        if (!Helper.isModEnabled(ModIds.MAGIC_LIB)) throw RuntimeException("RetroLib requires MagicLib!")
        hasNexerelin = Helper.isModEnabled(ModIds.NEXERELIN)
        hasStarshipLegends = Helper.isModEnabled(ModIds.STARSHIP_LEGENDS)
    }

    override fun onAboutToStartGeneratingCodex() {
//        CodexDataV2.WITH_GAME_MECHANICS_CAT = true
    }

    override fun onCodexDataGenerated() {
        // Create codex entry on retrofitting/conversions
    }
}