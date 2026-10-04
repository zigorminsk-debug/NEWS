package com.techpulse.app.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

/**
 * Простой и устойчивый парсер RSS 2.0 / Atom / RDF.
 * Понимает <item> и <entry>, собирает title, link, description/summary,
 * дату публикации и обложку (enclosure / media:content / первая <img>).
 */
object RssParser {

    data class RawItem(
        val title: String,
        val link: String,
        val descriptionHtml: String,
        val imageUrl: String?,
        val publishedAt: Long
    )

    fun parse(xml: String): List<RawItem> {
        val items = mutableListOf<RawItem>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(xml.byteInputStream(), "UTF-8")

            var inItem = false
            var title = ""
            var link = ""
            var description = ""
            var image: String? = null
            var date = 0L

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name?.lowercase(Locale.US) ?: ""
                        if (name == "item" || name == "entry") {
                            inItem = true
                            title = ""
                            link = ""
                            description = ""
                            image = null
                            date = 0L
                        } else if (inItem) {
                            when (name) {
                                "title" -> title = readText(parser)

                                "link" -> {
                                    // Atom: <link rel="alternate" href="..."/>, RSS: текст внутри
                                    val rel = parser.getAttributeValue(null, "rel")
                                    val href = parser.getAttributeValue(null, "href")
                                    when {
                                        !href.isNullOrBlank() && link.isBlank() ->
                                            if (rel == null || rel == "alternate") link = href.trim()

                                        href.isNullOrBlank() -> {
                                            val text = readText(parser).trim()
                                            if (text.startsWith("http") && link.isBlank()) link = text
                                        }
                                    }
                                }

                                "description", "summary", "content", "content:encoded",
                                "media:description", "xhtml:body" -> {
                                    val text = readText(parser)
                                    // Берём самое полное описание
                                    if (text.length > description.length) description = text
                                }

                                "pubdate", "published", "updated", "date",
                                "dc:date", "atom:updated", "issued" -> {
                                    val parsed = parseDate(readText(parser))
                                    if (parsed > date) date = parsed
                                }

                                "enclosure", "media:content", "media:thumbnail" -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank() && url.startsWith("http") && image == null) {
                                        image = url.trim()
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name?.lowercase(Locale.US) ?: ""
                        if (name == "item" || name == "entry") {
                            if (title.isNotBlank() && link.startsWith("http")) {
                                items += RawItem(
                                    title = title.trim(),
                                    link = link.trim(),
                                    descriptionHtml = description,
                                    imageUrl = image,
                                    publishedAt = date
                                )
                            }
                            inItem = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            // Повреждённый фид — возвращаем то, что успели распарсить.
        }
        return items
    }

    /** Читает текст внутри текущего тега (включая вложенные теги и CDATA). */
    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        try {
            var ev = parser.next()
            while (ev != XmlPullParser.END_TAG && ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.TEXT || ev == XmlPullParser.CDSECT) {
                    parser.text?.let { sb.append(it) }
                }
                ev = parser.next()
            }
        } catch (_: Exception) {
            // Игнорируем проблемы вложенности — вернём что есть
        }
        return sb.toString()
    }

    /**
     * Парсит даты в форматах, встречающихся в RSS/Atom:
     * RFC 822 ("Tue, 01 Oct 2024 12:00:00 GMT") и ISO 8601 ("2024-10-01T12:00:00+03:00").
     */
    fun parseDate(raw: String): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0L

        // ISO 8601 со смещением или Z
        try {
            return OffsetDateTime.parse(s).toInstant().toEpochMilli()
        } catch (_: Exception) {
        }

        // ISO 8601 без смещения — считаем UTC
        try {
            return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
        }

        // RFC 822 и прочие
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm z",
            "EEE, dd MMM yyyy HH:mm Z",
            "dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                val date: Date? = format.parse(s)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }
}
