package retroLib

import com.fs.starfarer.api.campaign.RepLevel
import org.json.JSONException
import org.json.JSONObject
import retroLib.api.FittersToTagsConvertor
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

object RetrofitsLoader {
    const val DELIMITER = ","
    const val ROUNDING_THRESHOLD = 100000

    const val ID = "id"
    const val FITTER = "fitter"
    const val TAGS = "tags"
    const val SOURCE = "source"
    const val TARGET = "target"
    const val COST = "cost"
    const val TIME = "time"
    const val REPUTATION = "reputation"
    const val COMMISSION = "commission"

    fun loadCSV(csvLoc: String, retrofits: MutableList<RetrofitData>, modId: String) {
        val csv = try {
            Helper.settings?.getMergedSpreadsheetDataForMod(ID, csvLoc, modId)
        } catch (ex: IOException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.WARNING, null, ex)
            return
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.WARNING, null, ex)
            return
        } catch (ex: RuntimeException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.INFO, "Could not find $csvLoc")
            return
        } ?: return

        try {
            for (i in 0 until csv.length()) {
                val o: JSONObject = csv.getJSONObject(i)
                val id: String = o.getString(ID)
                if (retrofits.any { it.id == id }) continue
                val tags = mutableSetOf(*o.getString(TAGS).split(DELIMITER).map { it.trim() }.toTypedArray())
                val source: String = o.getString(SOURCE)
                val target: String = o.getString(TARGET)
                try {
                    if (!tags.contains(RetroLib_Tags.FRAME_HULL)) Helper.settings?.getHullSpec(source)
                    Helper.settings?.getHullSpec(target)
                } catch (ex: Exception) {
                    try {
                        Helper.settings?.getFighterWingSpec(source)
                        Helper.settings?.getFighterWingSpec(target)
                        tags += RetroLib_Tags.FIGHTER_WING
                    } catch (ex: Exception) {
                        continue
                    }
                }
                val cost: Double? = try {
                    o.getInt(COST).toDouble()
                } catch (ex: JSONException) {
                    null
                }
                val time = max(0.0, o.getDouble(TIME))
                val rep: RepLevel = getReputation(o.getInt(REPUTATION))
                val commission: Boolean = o.getBoolean(COMMISSION)
                retrofits.add(RetrofitData(id, source, target, tags, cost, time, rep, commission))
            }

        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.SEVERE, null, ex)
        }
    }

    fun loadOldCSV(csvLoc: String, retrofits: MutableList<RetrofitData>, modId: String, convertors: List<FittersToTagsConvertor>) {
        val csv = try {
            Helper.settings?.getMergedSpreadsheetDataForMod(ID, csvLoc, modId)
        } catch (ex: IOException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.WARNING, null, ex)
            return
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.WARNING, null, ex)
            return
        } catch (ex: RuntimeException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.INFO, "Could not find $csvLoc")
            return
        } ?: return

        try {
            for (i in 0 until csv.length()) {
                val o: JSONObject = csv.getJSONObject(i)
                val id: String = o.getString(ID)
                if (retrofits.any { it.id == id }) continue
                val fitters: String = o.getString(FITTER)
                val tags = convertFitters(fitters, convertors)
                val source: String = o.getString(SOURCE)
                val target: String = o.getString(TARGET)
                try {
                    if (!tags.contains(RetroLib_Tags.FRAME_HULL)) Helper.settings?.getHullSpec(source)
                    Helper.settings?.getHullSpec(target)
                } catch (ex: Exception) {
                    continue
                }
                val cost: Double = try {
                    o.getInt(COST).toDouble()
                } catch (ex: JSONException) {
                    Helper.calculateCost(source, target, null)
                }
                val time = max(0.0, o.getDouble(TIME))
                val rep: RepLevel = getReputation(o.getInt(REPUTATION))
                val commission: Boolean = o.getBoolean(COMMISSION)
                retrofits.add(RetrofitData(id, source, target, tags, cost, time, rep, commission))
            }
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.SEVERE, null, ex)
        }
    }

    private fun convertFitters(fitters: String, convertors: List<FittersToTagsConvertor>): Set<String> {
        val result = mutableSetOf<String>()
        for (convertor in convertors) {
            result.addAll(convertor.convert(*fitters.split(DELIMITER).map { it.trim() }.toTypedArray()))
        }
        return result
    }

    fun getReputation(repInt: Int?): RepLevel {
        return when (repInt) {
            4 -> RepLevel.COOPERATIVE
            3 -> RepLevel.FRIENDLY
            2 -> RepLevel.WELCOMING
            1 -> RepLevel.FAVORABLE
            0 -> RepLevel.NEUTRAL
            -1 -> RepLevel.SUSPICIOUS
            -2 -> RepLevel.INHOSPITABLE
            -3 -> RepLevel.HOSTILE
            -4 -> RepLevel.VENGEFUL
            else -> RepLevel.NEUTRAL
        }
    }

    fun loadFrameBlacklist(path: String): Set<String> {
        val id = "hull_id"
        val csv = try {
            Helper.settings?.getMergedSpreadsheetDataForMod(
                id,
                path,
                Helper.modId
            )
        } catch (ex: IOException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.WARNING, null, ex)
            return emptySet()
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.WARNING, null, ex)
            return emptySet()
        } catch (ex: RuntimeException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.INFO, "Could not find $path")
            return emptySet()
        } ?: return emptySet()

        val result = mutableSetOf<String>()
        try {
            for (i in 0 until csv.length()) {
                val o: JSONObject = csv.getJSONObject(i)
                result += o.getString(id)
            }
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsLoader::class.java.name).log(Level.SEVERE, null, ex)
        }
        return result
    }
}