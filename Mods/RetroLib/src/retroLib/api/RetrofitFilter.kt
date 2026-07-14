package retroLib.api

import retroLib.RetrofitData

interface RetrofitFilter {
    val adjuster: RetrofitAdjuster
    fun allow(data: RetrofitData?): Boolean
}