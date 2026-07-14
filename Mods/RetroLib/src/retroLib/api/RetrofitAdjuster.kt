package retroLib.api

import retroLib.RetrofitData

interface RetrofitAdjuster {
    fun getFilterTestData(data: RetrofitData): RetrofitData
    fun getAdjustedData(data: RetrofitData): RetrofitData
}