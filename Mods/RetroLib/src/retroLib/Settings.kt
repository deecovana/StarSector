package retroLib

import org.magiclib.util.MagicSettings

object Settings {
    const val MAGIC_ID = "RetroLib"
    const val STARSHIP_LEGENDS = "sun_starship_legends"
    const val SHIP_PICK_COLUMNS = 7
    const val SHIP_PICK_ICON_SIZE = 58f

    private val loaded = HashMap<String, Any>()

    val FRAME_SOURCES_BLACKLIST = RetrofitsLoader.loadFrameBlacklist("data/config/retroLib/frameSourcesBlacklist.csv")
    val FRAME_TARGETS_BLACKLIST = RetrofitsLoader.loadFrameBlacklist("data/config/retroLib/frameTargetsBlacklist.csv")

    val DEV_MODE_RETROFITTER = getBoolean("devModeRetrofitter")
    val CONVERSION_PRICE_MULT = getFloat("conversionPriceMult")
    val BUY_MULT = Helper.settings?.getFloat("shipBuyPriceMult") ?: 1f
    val SELL_MULT = Helper.settings?.getFloat("shipSellPriceMult") ?: 1f
    val HAS_STARSHIP_LEGENDS = Helper.settings?.modManager?.isModEnabled(STARSHIP_LEGENDS) == true

    private fun saveAndReturn(id: String, result: Any): Any {
        loaded[id] = result
        return result
    }

    private fun getInt(id: String): Int = if (loaded.containsKey(id)) loaded[id] as Int
    else saveAndReturn(id, MagicSettings.getInteger(MAGIC_ID, id)) as Int

    private fun getFloat(id: String): Float = if (loaded.containsKey(id)) loaded[id] as Float
    else saveAndReturn(id, MagicSettings.getFloat(MAGIC_ID, id)) as Float

    private fun getBoolean(id: String): Boolean = if (loaded.containsKey(id)) loaded[id] as Boolean
    else saveAndReturn(id, MagicSettings.getBoolean(MAGIC_ID, id)) as Boolean
}