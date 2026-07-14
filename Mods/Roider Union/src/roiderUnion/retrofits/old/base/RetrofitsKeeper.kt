package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.thoughtworks.xstream.XStream
import org.json.JSONException
import org.json.JSONObject
import roiderUnion.helpers.Memory
import roiderUnion.helpers.Helper
import roiderUnion.ids.MemoryKeys
import roiderUnion.helpers.Settings
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

/**
 * Author: SafariJohn
 */
class RetrofitsKeeper {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.alias("roider_retData", RetrofitData::class.java)
            RetrofitData.aliasAttributes(x)
        }

        const val OLD_CSV = "data/retrofit/retrofits.csv"
        const val CONFIG_CSV = "data/config/modFiles/roider_retrofits.csv"

        const val BUY_MULT = "shipBuyPriceMult"
        const val SELL_MULT = "shipSellPriceMult"

        const val ID = "id"
        const val FITTER = "fitter"
        const val SOURCE = "source"
        const val TARGET = "target"
        const val COST = "cost"
        const val TIME = "time"
        const val REPUTATION = "reputation"
        const val COMMISSION = "commission"

        fun getRetrofits(verifier: RetrofitVerifier, vararg fitters: String): List<RetrofitData> {
            return (Memory.get(MemoryKeys.RETROFIT_KEEPER, { it is RetrofitsKeeper }, { RetrofitsKeeper() }) as RetrofitsKeeper)
                .getRetrofits(verifier, *fitters)
        }

        fun isFittedHere(
            availableFitters: String,
            vararg activeFitters: String
        ): Boolean {
            val fitters = availableFitters.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return fitters.any { activeFitters.any { active -> active.trim { it <= ' ' } == it.trim { it <= ' ' } } }
        }

        fun calculateCost(
            sourceHull: String?,
            targetHull: String?, market: MarketAPI?
        ): Double {
            var results: Double = calculateBuyPrice(targetHull, market)
            results -= calculateSellPrice(sourceHull, market)
            results *= Settings.CONVERSION_PRICE_MULT.toDouble()
            val roundingThreshold = results >= 100000
            return if (roundingThreshold) Helper.roundToThousands(results) else Helper.roundToHundreds(results)
        }

        private fun calculateBuyPrice(hullId: String?, market: MarketAPI?): Double {
            if (Helper.anyNull(hullId, market)) return 0.0
            val tariff = 1f + market!!.tariff.modifiedValue
            val baseValue = Helper.settings?.getHullSpec(hullId)?.baseValue ?: 0f
            val buyMult = Helper.settings?.getFloat(BUY_MULT) ?: 1f
            return (baseValue * buyMult * tariff).toDouble()
        }

        private fun calculateSellPrice(hullId: String?, market: MarketAPI?): Double {
            if (Helper.anyNull(hullId, market)) return 0.0
            val tariff = 1f + market!!.tariff.modifiedValue
            val sourceValue = Helper.settings?.getHullSpec(hullId)?.baseValue ?: 0f
            val sellMult = Helper.settings?.getFloat(SELL_MULT) ?: 1f
            val sellPrice: Float = sourceValue * sellMult
            return (sellPrice - sellPrice * (tariff - 1f)).toDouble()
        }

        fun getReputation(repInt: Int): RepLevel {
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

        fun alreadyLoaded(retrofits: List<RetrofitData>, id: String): Boolean {
            return retrofits.any { it.id == id }
        }
    }

    @Transient
    private var allData: MutableList<RetrofitData>? = null

    init {
        initAllData()
    }

    private fun initAllData() {
        allData = mutableListOf()
        loadCSV(CONFIG_CSV, allData!!)
        loadCSV(OLD_CSV, allData!!)
    }

    fun getRetrofits(verifier: RetrofitVerifier, vararg fitters: String): List<RetrofitData> {
        if (allData == null) {
            initAllData()
        }
        val retrofits =  allData!!.filter { isFittedHere(it.fitter, *fitters) }.map { verifier.verifyData(it) }
        return retrofits.filterNotNull().map { it }
    }

    private fun loadCSV(csvLoc: String, retrofits: MutableList<RetrofitData>) {
        val csv = try {
            Global.getSettings().getMergedSpreadsheetDataForMod(ID, csvLoc, Helper.modId)
        } catch (ex: IOException) {
            Logger.getLogger(RetrofitsKeeper::class.java.name).log(Level.WARNING, null, ex)
            return
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsKeeper::class.java.name).log(Level.WARNING, null, ex)
            return
        } catch (ex: RuntimeException) {
            Logger.getLogger(RetrofitsKeeper::class.java.name).log(Level.INFO, "Could not find $csvLoc")
            return
        }
        try {
            for (i in 0 until csv.length()) {
                val o: JSONObject = csv.getJSONObject(i)
                val id: String = o.getString(ID)
                if (alreadyLoaded(retrofits, id)) continue
                val fitters: String = o.getString(FITTER)
                val source: String = o.getString(SOURCE)
                val target: String = o.getString(TARGET)
                try {
                    Global.getSettings().getHullSpec(source)
                    Global.getSettings().getHullSpec(target)
                } catch (ex: Exception) {
                    continue
                }
                val cost: Double = try {
                    o.getInt(COST).toDouble()
                } catch (ex: JSONException) {
                    calculateCost(source, target, null)
                }
                val time = max(0.0, o.getDouble(TIME))
                val rep: RepLevel = getReputation(o.getInt(REPUTATION))
                val commission: Boolean = o.getBoolean(COMMISSION)
                retrofits.add(RetrofitData(id, fitters, source, target, cost, time, rep, commission))
            }
        } catch (ex: JSONException) {
            Logger.getLogger(RetrofitsKeeper::class.java.name).log(Level.SEVERE, null, ex)
        }
    }
}