package com.example.myapplication.data.cbr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.text.SimpleDateFormat
import java.util.*

object GoldRepository {

    private const val BASE_URL = "https://www.cbr.ru/"
    private const val PREFS = "gold_prefs"
    private const val PREF_RATE = "gold_rate_rub_per_g"

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("ru"))

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
    }
    private val api by lazy { retrofit.create(CBRService::class.java) }

    /**
     * Возвращает курс золота (учетная цена ЦБ) в рублях за грамм (Double).
     * Берём последнюю запись за сегодня (Code=1).
     * Значение чистим от пробелов и запятой.
     */
    suspend fun loadTodayGoldRubPerGram(context: Context): Double? = withContext(Dispatchers.IO) {
        try {
            val today = Date()
            val d = dateFormat.format(today)
            val resp = api.getMetals(d, d)
            val gold = resp.records.orEmpty().lastOrNull { it.code == 1 }
            val text = gold?.buy ?: gold?.sell
            val parsed = text
                ?.replace(" ", "")
                ?.replace(",", ".")
                ?.toDoubleOrNull()
            if (parsed != null) {
                saveCachedRate(context, parsed)
            }
            parsed
        } catch (_: Exception) {
            getCachedRate(context)
        }
    }

    fun saveCachedRate(context: Context, rate: Double) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(PREF_RATE, rate.toFloat())
            .apply()
    }

    fun getCachedRate(context: Context): Double? {
        val f = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(PREF_RATE, Float.NaN)
        return if (f.isNaN()) null else f.toDouble()
    }
}
