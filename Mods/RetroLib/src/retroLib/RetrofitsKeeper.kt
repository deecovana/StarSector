package retroLib

import retroLib.api.FittersToTagsConvertor
import retroLib.api.RetrofitFilter

class RetrofitsKeeper {
    companion object {
        const val LIB_CSV = "data/config/retroLib/retrofits.csv"
        const val CONFIG_CSV = "data/config/modFiles/roider_retrofits.csv"
        const val OLD_CSV = "data/retrofit/retrofits.csv"

        val DEFAULT_CONVERTOR = object : FittersToTagsConvertor {
            override fun convert(vararg fitters: String): Set<String> = setOf()
        }

        val CONVERTORS = mutableListOf(DEFAULT_CONVERTOR)

        fun getRetrofits(filter: RetrofitFilter): List<RetrofitData> {
            return Helper.getRetrofitManager().getRetrofits(filter)
        }
    }

    @Transient
    private var retrofitData: MutableList<RetrofitData>? = null

    init {
        initRetrofits()
    }

    private fun initRetrofits() {
        retrofitData = mutableListOf()
        RetrofitsLoader.loadCSV(LIB_CSV, retrofitData!!, Helper.modId)
        RetrofitsLoader.loadOldCSV(CONFIG_CSV, retrofitData!!, Helper.modId, CONVERTORS)
        RetrofitsLoader.loadOldCSV(OLD_CSV, retrofitData!!, Helper.modId, CONVERTORS)
    }

    fun getRetrofits(filter: RetrofitFilter): List<RetrofitData> {
        if (retrofitData == null) initRetrofits()
        return retrofitData!!.filter { filter.allow(it) }.map { filter.adjuster.getAdjustedData(it) }
    }
}