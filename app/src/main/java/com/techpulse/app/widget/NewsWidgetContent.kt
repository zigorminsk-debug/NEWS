package com.techpulse.app.widget

import android.content.Context
import com.techpulse.app.data.translate.Translator
import org.json.JSONArray
import java.io.File

/**
 * Данные для виджетов: читает кэш ленты (тот же файл, что пишет NewsRepository)
 * и подставляет переведённые заголовки из кэша переводов.
 */
object NewsWidgetContent {

    data class Row(
        val title: String,
        val sourceName: String,
        val sourceColor: Int,
        val publishedAt: Long,
        val link: String
    )

    fun load(context: Context, limit: Int): List<Row> {
        val file = File(context.applicationContext.filesDir, "feed_cache.json")
        if (!file.exists()) return emptyList()
        return try {
            Translator.init(context)
            val array = JSONArray(file.readText())
            val rows = ArrayList<Row>(minOf(limit, array.length()))
            val count = minOf(limit, array.length())
            for (i in 0 until count) {
                val o = array.getJSONObject(i)
                val title = o.optString("title")
                val link = o.optString("link")
                if (title.isBlank() || link.isBlank()) continue
                val shownTitle = Translator.cachedText(title, "ru") ?: title
                rows += Row(
                    title = shownTitle,
                    sourceName = o.optString("sourceName"),
                    sourceColor = o.optLong("sourceColorHex", 0xFF00E5FF).toInt(),
                    publishedAt = o.optLong("publishedAt"),
                    link = link
                )
            }
            rows
        } catch (_: Exception) {
            emptyList()
        }
    }
}
