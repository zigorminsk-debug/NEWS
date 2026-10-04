package com.techpulse.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.techpulse.app.MainActivity
import com.techpulse.app.R
import com.techpulse.app.data.NewsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Виджет «Лента новостей» (4×4): заголовок TECHPULSE, время обновления,
 * кнопка ↻ и прокручиваемый список свежих новостей.
 * Тап по новости открывает статью во встроенном ридере приложения.
 */
class NewsListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    NewsRepository(context.applicationContext).fetchAll()
                } catch (_: Exception) {
                    // Нет сети — просто перерисуем виджеты из кэша
                } finally {
                    WidgetRefresh.notifyAll(context.applicationContext)
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.techpulse.app.widget.REFRESH_LIST"

        private fun buildViews(context: Context, appWidgetId: Int): RemoteViews =
            WidgetRefresh.baseViews(context, R.layout.widget_news_list).apply {
                // Список подключаем к RemoteViewsService
                val adapterIntent = Intent(context, NewsWidgetService::class.java)
                setRemoteAdapter(appWidgetId, R.id.widget_list, adapterIntent)
                setEmptyView(R.id.widget_list, R.id.widget_empty)

                // Шаблон PendingIntent для строк списка (extras подставляются в getViewAt)
                val template = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setPendingIntentTemplate(
                    R.id.widget_list,
                    PendingIntent.getActivity(
                        context,
                        21,
                        template,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                )

                // Клик по шапке — открыть приложение
                val openApp = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        20,
                        openApp,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )

                // Кнопка ↻ — обновить ленту из сети
                val refresh = WidgetRefresh.headerRefreshIntent(
                    context, NewsListWidgetProvider::class.java, ACTION_REFRESH
                )
                setOnClickPendingIntent(
                    R.id.widget_refresh,
                    PendingIntent.getBroadcast(
                        context,
                        22,
                        refresh,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            for (id in ids) {
                manager.updateAppWidget(id, buildViews(context, id))
            }
        }

        /** Обновить все экземпляры виджета (шапка + перечитать список). */
        fun refreshWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = WidgetRefresh.appWidgetIds(context, NewsListWidgetProvider::class.java)
            if (ids.isEmpty()) return
            updateWidgets(context, manager, ids)
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }
    }
}
