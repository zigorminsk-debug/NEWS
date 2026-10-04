package com.techpulse.app.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.techpulse.app.MainActivity
import com.techpulse.app.R
import com.techpulse.app.ui.TimeAgo

/** RemoteAdapter для списка новостей виджета «Лента новостей». */
class NewsWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {

        private var rows: List<NewsWidgetContent.Row> = emptyList()

        override fun onCreate() = Unit

        /** Вызывается на binder-потоке — можно читать файлы. */
        override fun onDataSetChanged() {
            rows = NewsWidgetContent.load(context, MAX_ROWS)
        }

        override fun getViewAt(position: Int): RemoteViews {
            val row = rows[position]
            return RemoteViews(context.packageName, R.layout.widget_news_row).apply {
                setTextViewText(R.id.widget_row_title, row.title)
                setTextViewText(R.id.widget_row_source, row.sourceName.uppercase())
                setTextColor(R.id.widget_row_source, row.sourceColor)
                setTextColor(R.id.widget_row_dot, row.sourceColor)
                setTextViewText(R.id.widget_row_time, TimeAgo.format(row.publishedAt))

                // Заполнитель для шаблона PendingIntent: открыть статью в приложении
                val fillIn = Intent().putExtra(MainActivity.EXTRA_OPEN_URL, row.link)
                setOnClickFillInIntent(R.id.widget_row_root, fillIn)
            }
        }

        override fun getCount(): Int = rows.size

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = true

        override fun onDestroy() = Unit
    }

    companion object {
        private const val MAX_ROWS = 25
    }
}
