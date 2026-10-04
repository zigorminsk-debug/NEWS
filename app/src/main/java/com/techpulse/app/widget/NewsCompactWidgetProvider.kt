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
import com.techpulse.app.ui.TimeAgo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Компактный виджет «Топ-новость» (2×2): самая свежая новость ленты.
 * Тап — статья открывается во встроенном ридере приложения; ↻ — обновить.
 */
class NewsCompactWidgetProvider : AppWidgetProvider() {

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
                    // Нет сети — перерисуем из кэша
                } finally {
                    WidgetRefresh.notifyAll(context.applicationContext)
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.techpulse.app.widget.REFRESH_COMPACT"

        private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
            val views = WidgetRefresh.baseViews(context, R.layout.widget_news_compact)

            val rows = NewsWidgetContent.load(context, 1)
            val top = rows.firstOrNull()

            if (top == null) {
                views.setTextViewText(R.id.widget_headline, "Откройте TechPulse, чтобы загрузить новости")
                views.setTextViewText(R.id.widget_source_text, "")

                val openApp = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        openApp,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                views.setTextViewText(R.id.widget_headline, top.title)
                views.setTextViewText(
                    R.id.widget_source_text,
                    "● ${top.sourceName.uppercase()} · ${TimeAgo.format(top.publishedAt)}"
                )
                views.setTextColor(R.id.widget_source_text, top.sourceColor)

                // Тап по виджету — открыть статью в ридере приложения
                val openArticle = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_OPEN_URL, top.link)
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        openArticle,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            // Кнопка ↻
            val refresh = WidgetRefresh.headerRefreshIntent(
                context, NewsCompactWidgetProvider::class.java, ACTION_REFRESH
            )
            views.setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 10_000,
                    refresh,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            return views
        }

        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            for (id in ids) {
                manager.updateAppWidget(id, buildViews(context, id))
            }
        }

        fun refreshWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = WidgetRefresh.appWidgetIds(context, NewsCompactWidgetProvider::class.java)
            if (ids.isEmpty()) return
            updateWidgets(context, manager, ids)
        }
    }
}
