package com.example.myapplication.data.cbr

import retrofit2.http.GET
import retrofit2.http.Query

interface CBRService {
    // Пример: /scripts/xml_metall.asp?date_req1=01/10/2025&date_req2=08/10/2025
    @GET("scripts/xml_metall.asp")
    suspend fun getMetals(
        @Query("date_req1") dateFrom: String, // dd/MM/yyyy
        @Query("date_req2") dateTo: String    // dd/MM/yyyy
    ): MetallResponse
}
