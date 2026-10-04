package com.techpulse.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.techpulse.app.R
import com.techpulse.app.data.NewsRepository
import com.techpulse.app.ui.TimeAgo

/** Общий помощник обновления всех виджетов TechPulse. */
object WidgetRefresh {

    /** Перестроить содержимое всех установленных виджетов. */
    fun notifyAll(context: Context) {
        NewsListWidgetProvider.refreshWidgets(context.applicationContext)
        NewsCompactWidgetProvider.refreshWidgets(context.applicationContext)
    }

    fun updatedLabel(context: Context): String {
        val at = NewsRepository.cacheUpdatedAt(context)
        return if (at > 0) "обновлено: ${TimeAgo.format(at)}" else ""
    }

    fun appWidgetIds(context: Context, providerClass: Class<*>): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, providerClass))

    fun headerRefreshIntent(context: Context, providerClass: Class<*>, action: String): Intent =
        Intent(context, providerClass).setAction(action)

    fun baseViews(context: Context, layoutId: Int): RemoteViews =
        RemoteViews(context.packageName, layoutId).apply {
            setTextViewText(R.id.widget_updated, updatedLabel(context))
        }
}
