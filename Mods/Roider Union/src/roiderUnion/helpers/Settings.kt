package roiderUnion.helpers

import org.magiclib.util.MagicSettings

object Settings {
    const val MAGIC_ID = "RoiderUnion"
    const val GRAPHICS_COMBAT = "roider_combat"

    private val loaded = HashMap<String, Any>()

    val MAX_SHIPS_IN_AI_FLEET = getVanillaInt("maxShipsInAIFleet")

    val PHASENET_ICON_NAME = getSpriteName(GRAPHICS_COMBAT,"phasenet")

    val MAX_ACTIVE_NOMADS = getInt("maxNomadFleets")
    val MAX_NOMAD_TRADE_FLEETS = getInt("maxNomadTradeFleets")
    val TRADE_FLEET_INTERVAL = Helper.settings?.getFloat("minEconSpawnIntervalPerMarket") ?: 0f
    val MAX_NOMAD_BASES = getInt("maxNomadBases")
    val NOMAD_BASES_T5 = getInt("maxT5NomadBases")
    val NOMAD_BASES_T4 = getInt("maxT4NomadBases")
    val NOMAD_BASES_T3 = getInt("maxT3NomadBases")
    val NOMAD_BASE_INTERVAL_MAX = getFloat("nomadBaseSpawnIntervalMax")
    val NOMAD_BASE_INTERVAL_MIN = getFloat("nomadBaseSpawnIntervalMin")
    val NOMAD_BASE_FACTION_WEIGHTS = getFloatMap("nomadBaseFactionWeights")

    val MAX_MINING_FLEETS = getInt("maxMiningFleets")

    val UNION_HQ_SHIP_REP_REQ = getFloatMap("roider_unionHQShipRepReq")
    val UNION_HQ_SHIP_COM_NOT_REQ = getStringList("roider_unionHQShipComNotReq")

    val AVG_PATROL_INTERVAL = getVanillaFloat("averagePatrolSpawnInterval")

    @Deprecated("Only used by old retrofitting system.")
    val CONVERSION_PRICE_MULT = getFloat("roider_conversionPriceMult")

    const val LOOT_STASH_MINE_IMAGES = "roider_stashMines"
    const val LOOT_STASH_MINE = "mine4"
    const val LOOT_STASH_MINE_GLOW = "mineGlow"

    private fun saveAndReturn(id: String, result: Any): Any {
        loaded[id] = result
        return result
    }

    private fun getInt(id: String): Int = if (loaded.containsKey(id)) loaded[id] as Int
        else saveAndReturn(id, MagicSettings.getInteger(MAGIC_ID, id)) as Int

    private fun getFloat(id: String): Float = if (loaded.containsKey(id)) loaded[id] as Float
    else saveAndReturn(id, MagicSettings.getFloat(MAGIC_ID, id)) as Float

    private fun getVanillaInt(id: String): Int = if (loaded.containsKey(id)) loaded[id] as Int
    else saveAndReturn(id, Helper.settings?.getInt(id) ?: 1) as Int

    private fun getVanillaFloat(id: String): Float = if (loaded.containsKey(id)) loaded[id] as Float
    else saveAndReturn(id, Helper.settings?.getFloat(id) ?: 1f) as Float

    private fun getSpriteName(category: String, id: String): String = if (loaded.containsKey(id)) loaded[id] as String
        else saveAndReturn(id, Helper.settings?.getSpriteName(category, id) ?: "") as String

    private fun getStringList(id: String): MutableList<String> = if (loaded.containsKey(id)) loaded[id] as MutableList<String>
        else saveAndReturn(id, MagicSettings.getList(MAGIC_ID, id)) as MutableList<String>


    private fun getFloatMap(id: String): MutableMap<String, Float> = if (loaded.containsKey(id)) loaded[id] as MutableMap<String, Float>
        else saveAndReturn(id, MagicSettings.getFloatMap(MAGIC_ID, id)) as MutableMap<String, Float>
}