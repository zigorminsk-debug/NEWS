package com.techpulse.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Локальное хранилище избранного (SharedPreferences + JSON).
 * Переживает перезапуск приложения.
 */
class BookmarksStore(context: Context) {

    private val prefs = context.getSharedPreferences("techpulse_bookmarks", Context.MODE_PRIVATE)

    private val items = mutableListOf<FeedItem>()

    init {
        try {
            val raw = prefs.getString(KEY, null)
            if (!raw.isNullOrBlank()) {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val image = o.optString("imageUrl")
                    items += FeedItem(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        link = o.optString("link"),
                        summary = o.optString("summary"),
                        imageUrl = if (image.isNullOrBlank()) null else image,
                        publishedAt = o.optLong("publishedAt"),
                        sourceId = o.optString("sourceId"),
                        sourceName = o.optString("sourceName"),
                        sourceColorHex = o.optLong("sourceColorHex")
                    )
                }
            }
        } catch (_: Exception) {
            // Повреждённые данные — начинаем с пустого списка
        }
    }

    val current: List<FeedItem> get() = items.toList()

    fun isBookmarked(item: FeedItem): Boolean = items.any { it.id == item.id }

    /** Переключает закладку. Возвращает true, если статья добавлена в избранное. */
    fun toggle(item: FeedItem): Boolean {
        val index = items.indexOfFirst { it.id == item.id }
        val nowBookmarked: Boolean
        if (index >= 0) {
            items.removeAt(index)
            nowBookmarked = false
        } else {
            items.add(0, item)
            nowBookmarked = true
        }
        persist()
        return nowBookmarked
    }

    private fun persist() {
        try {
            val array = JSONArray()
            items.forEach { item ->
                array.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("title", item.title)
                        .put("link", item.link)
                        .put("summary", item.summary)
                        .put("imageUrl", item.imageUrl ?: "")
                        .put("publishedAt", item.publishedAt)
                        .put("sourceId", item.sourceId)
                        .put("sourceName", item.sourceName)
                        .put("sourceColorHex", item.sourceColorHex)
                )
            }
            prefs.edit().putString(KEY, array.toString()).apply()
        } catch (_: Exception) {
            // Ошибка сохранения не должна ломать UI
        }
    }

    companion object {
        private const val KEY = "bookmarked_items"
    }
}
