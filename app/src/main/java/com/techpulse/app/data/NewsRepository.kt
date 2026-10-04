package com.techpulse.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Репозиторий новостей: параллельно загружает все RSS-источники,
 * объединяет, сортирует по свежести и кэширует в файл для оффлайн-режима.
 */
class NewsRepository(context: Context) {

    private val cacheFile: File = File(context.filesDir, "feed_cache.json")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /** Загружает все источники параллельно. Возвращает объединённую ленту. */
    suspend fun fetchAll(): List<FeedItem> {
        val merged = coroutineScope {
            Sources.NEWS.map { source ->
                async(Dispatchers.IO) { fetchSource(source) }
            }.awaitAll()
                .flatten()
                .distinctBy { it.link }
                .sortedByDescending { it.publishedAt }
                .take(MAX_ITEMS)
        }
        if (merged.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                runCatching { saveCache(merged) }
            }
        }
        return merged
    }

    private fun fetchSource(source: NewsSource): List<FeedItem> {
        return try {
            val request = Request.Builder()
                .url(source.rssUrl)
                .header("User-Agent", USER_AGENT)
                .header(
                    "Accept",
                    "application/rss+xml, application/atom+xml, application/xml, text/xml, */*"
                )
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                RssParser.parse(body).mapNotNull { raw ->
                    val summary = HtmlUtils.toText(raw.descriptionHtml)
                    val cleanTitle = HtmlUtils.toText(raw.title).ifBlank { raw.title }
                    if (cleanTitle.isBlank()) return@mapNotNull null
                    FeedItem(
                        id = raw.link.trim(),
                        title = cleanTitle,
                        link = raw.link.trim(),
                        summary = summary,
                        imageUrl = raw.imageUrl ?: HtmlUtils.firstImageUrl(raw.descriptionHtml),
                        publishedAt = if (raw.publishedAt > 0) raw.publishedAt else System.currentTimeMillis(),
                        sourceId = source.id,
                        sourceName = source.name,
                        sourceColorHex = source.colorHex
                    )
                }
            }
        } catch (_: Exception) {
            // Источник недоступен — просто пропускаем его, лента собирается из остальных
            emptyList()
        }
    }

    /** Кэш последней успешной загрузки (для оффлайн-чтения). */
    suspend fun loadCache(): List<FeedItem> = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists()) return@withContext emptyList()
            val array = JSONArray(cacheFile.readText())
            val result = mutableListOf<FeedItem>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val image = o.optString("imageUrl")
                result += FeedItem(
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
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCache(items: List<FeedItem>) {
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
        cacheFile.writeText(array.toString())
    }

    companion object {
        private const val MAX_ITEMS = 300
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36 TechPulse/1.0"
    }
}
