package com.example.myapplication.data.cbr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object GoldRepository {

    private const val BASE_URL = "https://www.cbr.ru/"
    private const val PREFS = "gold_prefs"
    private const val PREF_RATE = "gold_rate_rub_per_g"

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("ru"))

    private val okHttp by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android) MyApplication/1.0 (GoldWidget)")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(ScalarsConverterFactory.create()) // сырой XML
            .build()
    }

    private val api by lazy { retrofit.create(CBRService::class.java) }

    /**
     * Возвращает последнюю (за 7 дней) цену золота ЦБ (руб/г).
     * Парсим сырой XML регулярками; берём последнюю запись Code="1".
     */
    suspend fun loadTodayGoldRubPerGram(context: Context): Double? = withContext(Dispatchers.IO) {
        try {
            val cal = Calendar.getInstance()
            val dateTo = dateFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val dateFrom = dateFormat.format(cal.time)

            val xml = api.getMetalsRaw(dateFrom, dateTo)

            val recordRegex = Regex(
                pattern = """<Record[^>]*Code\s*=\s*"1"[^>]*>.*?</Record>""",
                options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val buyRegex = Regex(
                pattern = """<Buy>(.*?)</Buy>""",
                options = setOf(RegexOption.DOT_MATCHES_ALL)
            )
            val sellRegex = Regex(
                pattern = """<Sell>(.*?)</Sell>""",
                options = setOf(RegexOption.DOT_MATCHES_ALL)
            )

            val lastMatch = recordRegex.findAll(xml).toList().lastOrNull()
            val recordXml = lastMatch?.value

            val rawNumber = when {
                recordXml == null -> null
                buyRegex.find(recordXml) != null -> buyRegex.find(recordXml)!!.groupValues[1]
                sellRegex.find(recordXml) != null -> sellRegex.find(recordXml)!!.groupValues[1]
                else -> null
            }

            val parsed = rawNumber
                ?.replace(" ", "")
                ?.replace(",", ".")
                ?.toDoubleOrNull()

            if (parsed != null) saveCachedRate(context, parsed)
            parsed ?: getCachedRate(context)
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
