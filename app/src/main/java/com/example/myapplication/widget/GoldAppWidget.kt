package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.myapplication.R
import com.example.myapplication.data.cbr.GoldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class GoldAppWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.example.myapplication.ACTION_REFRESH_GOLD"
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it, isLoading = true) }
        // подгружаем свежий курс и обновляем
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refresh(context)
        }
    }

    private fun refresh(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            updateAll(context, isLoading = true)
            val rate = GoldRepository.loadTodayGoldRubPerGram(context)
            updateAll(context, rate)
        }
    }

    private fun updateAll(context: Context, rate: Double? = null, isLoading: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, GoldAppWidget::class.java))
        ids.forEach { updateOne(context, manager, it, rate, isLoading) }
    }

    private fun updateOne(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        rate: Double? = null,
        isLoading: Boolean = false
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_gold)

        val intent = Intent(context, GoldAppWidget::class.java).apply {
            action = ACTION_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.goldBall, pi)

        val text = when {
            isLoading -> "…"
            rate != null -> "₽ " + DecimalFormat("#,###").format(rate)
            else -> "нет сети"
        }
        views.setTextViewText(R.id.tvGold, text)

        manager.updateAppWidget(id, views)
    }
}
