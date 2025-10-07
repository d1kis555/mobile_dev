package com.example.myapplication.data.cbr

import retrofit2.http.GET
import retrofit2.http.Query

interface CBRService {
    @GET("scripts/xml_metall.asp")
    suspend fun getMetalsRaw(
        @Query("date_req1") dateFrom: String, // dd/MM/yyyy
        @Query("date_req2") dateTo: String    // dd/MM/yyyy
    ): String
}
