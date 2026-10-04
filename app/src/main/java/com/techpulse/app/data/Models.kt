package com.techpulse.app.data

/** Новость в ленте. */
data class FeedItem(
    val id: String,
    val title: String,
    val link: String,
    /** Краткая аннотация — очищенный от HTML текст описания. */
    val summary: String,
    val imageUrl: String?,
    /** Epoch millis публикации. */
    val publishedAt: Long,
    val sourceId: String,
    val sourceName: String,
    /** Цвет-метка источника (ARGB). */
    val sourceColorHex: Long,
    /** Перевод заголовка на русский (подставляется фоново после загрузки ленты). */
    val translatedTitle: String? = null,
    /** Перевод аннотации на русский. */
    val translatedSummary: String? = null
)

/** RSS-источник IT-новостей. */
data class NewsSource(
    val id: String,
    val name: String,
    val rssUrl: String,
    val siteUrl: String,
    val colorHex: Long,
    val lang: String
)

/** Полезный IT-ресурс для каталога. */
data class ItResource(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val category: String,
    val lang: String
)
