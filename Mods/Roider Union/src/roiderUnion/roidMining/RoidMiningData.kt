package roiderUnion.roidMining

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import roiderUnion.helpers.Helper
import roiderUnion.ids.hullmods.RoiderHullmods
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

object RoidMiningData {
    const val RICHNESS_PATH = "data/config/modFiles/roider_rezRichness.csv"
    const val ID = "id"
    const val MIN_ROIDS = "minRoids"
    const val MAX_ROIDS = "maxRoids"
    const val ICE_MULT = "iceMult"
    const val DUST_MULT = "dustMult"
    const val ROCK_MULT = "rockMult"
    const val METAL_MULT = "metalMult"
    const val PLUTON_MULT = "plutonMult"

    @JvmField
    val ZONE_DATA = mutableMapOf<String, RoidMiningHelper.ZoneData>()
    init {
        ZONE_DATA[RoidMiningHelper.ASTEROID] = RoidMiningHelper.ZoneData(10f, 15f, 25f, 15f, 5f)
        ZONE_DATA[RoidMiningHelper.ICE_ZONE] = RoidMiningHelper.ZoneData(20f, 10f, 0f, 0f, 1f)
        ZONE_DATA[RoidMiningHelper.RING_ZONE] = RoidMiningHelper.ZoneData(10f, 15f, 5f, 0f, 1f)
    }

    @JvmField
    val SPECIAL_MINING_WEAPON_COVERAGE = mutableMapOf<String, Float>()

    @JvmField
    val FULL_COVERAGE_HULLMODS = mutableListOf(
        RoiderHullmods.MINING_EQUIPMENT,
        RoiderHullmods.ICE_HARVESTER
    )

    @JvmField
    val WEAPON_DATA = mutableListOf<MiningWeaponSpec>()
    init {
        val csv = try {
            Helper.settings?.getMergedSpreadsheetDataForMod("id", "data/config/modFiles/roider_mining.csv", Helper.modId)
        } catch (e: Exception) {
            null
        }
        if (csv != null) {
            try {
                for (i in 0 until csv.length()) {
                    val o = csv.getJSONObject(i) ?: continue
                    val id = o.getString(RoidMiningHelper.ID) ?: continue
                    val ice = getFloat(o, RoidMiningHelper.ICE) ?: 0f
                    val dust = getFloat(o, RoidMiningHelper.DUST) ?: 0f
                    val rock = getFloat(o, RoidMiningHelper.ROCK) ?: 0f
                    val metal = getFloat(o, RoidMiningHelper.METAL) ?: 0f
                    val pluton = getFloat(o, RoidMiningHelper.PLUTON) ?: 0f
                    WEAPON_DATA += MiningWeaponSpec(id, ice, dust, rock, metal, pluton)
                }
            } catch (ex: JSONException) {
                Logger.getLogger(RoidMiningData::class.java.name).log(Level.SEVERE, null, ex)
            }
        }
    }

    private fun getFloat(o: JSONObject, id: String): Float? {
        return try {
            o.getDouble(id)
        } catch (ex: JSONException) {
            return null
        }.toFloat()
    }

    @JvmField
    val REZ_RICHES = mutableMapOf<String, RoidMiningHelper.RoidRichness>()
    init {
        val csv: JSONArray? = try {
            Helper.settings?.getMergedSpreadsheetDataForMod(ID, RICHNESS_PATH, Helper.modId)
        } catch (ex: IOException) {
            Logger.getLogger(RoidMiningData::class.java.name).log(Level.WARNING, null, ex)
            null
        } catch (ex: JSONException) {
            Logger.getLogger(RoidMiningData::class.java.name).log(Level.WARNING, null, ex)
            null
        } catch (ex: RuntimeException) {
            Logger.getLogger(RoidMiningData::class.java.name).log(Level.INFO, "Could not find $RICHNESS_PATH")
            null
        }
        if (csv != null) {
            try {
                for (i in 0 until csv.length()) {
                    val o = csv.getJSONObject(i) ?: continue
                    val id = o.getString(ID)
                    if (id.isEmpty()) continue
                    val minRoids = o.getInt(MIN_ROIDS)
                    val maxRoids = o.getInt(MAX_ROIDS)
                    val iceMult = getFloat(o, ICE_MULT) ?: 1f
                    val dustMult = getFloat(o, DUST_MULT) ?: 1f
                    val rockMult = getFloat(o, ROCK_MULT) ?: 1f
                    val metalMult = getFloat(o, METAL_MULT) ?: 1f
                    val plutonMult = getFloat(o, PLUTON_MULT) ?: 1f
                    REZ_RICHES[id] = RoidMiningHelper.RoidRichness(minRoids, maxRoids, iceMult, dustMult, rockMult, metalMult, plutonMult)
                }
            } catch (ex: JSONException) {
                Logger.getLogger(RoidMiningData::class.java.name).log(Level.SEVERE, null, ex)
            }
        }
    }
}