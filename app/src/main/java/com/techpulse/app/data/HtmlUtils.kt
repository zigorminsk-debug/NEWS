package com.techpulse.app.data

/**
 * Утилиты для очистки HTML из RSS-описаний:
 * получение чистого текста аннотации и первой картинки.
 */
object HtmlUtils {

    private val NAMED_ENTITIES: Map<String, String> = mapOf(
        "&nbsp;" to " ",
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&#39;" to "'",
        "&laquo;" to "«",
        "&raquo;" to "»",
        "&mdash;" to "—",
        "&ndash;" to "–",
        "&hellip;" to "…",
        "&rsquo;" to "’",
        "&lsquo;" to "‘",
        "&ldquo;" to "“",
        "&rdquo;" to "”",
        "&times;" to "×",
        "&middot;" to "·",
        "&rarr;" to "→",
        "&bull;" to "•"
    )

    /** Первая картинка из HTML (тег img). Учитывает src, data-src и srcset. */
    fun firstImageUrl(html: String): String? {
        val imgIndex = html.indexOf("<img", ignoreCase = true)
        if (imgIndex < 0) return null
        val rest = html.substring(imgIndex, minOf(html.length, imgIndex + 4000))
        // Порядок важен: сначала обычный src, затем ленивые data-src, затем srcset
        val patterns = listOf(
            Regex("\\ssrc\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("\\sdata-src\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("\\ssrcset\\s*=\\s*[\"']([^\"'\\s]+)", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(rest) ?: continue
            val url = match.groupValues[1]
            if (url.startsWith("http")) return url
        }
        return null
    }

    /** HTML → обычный текст: без тегов, с раскрытыми сущностями. */
    fun toText(html: String): String {
        if (html.isBlank()) return ""
        var s = html

        // Убираем скрипты, стили и комментарии целиком
        s = Regex("(?is)<(script|style)[^>]*>.*?</(script|style)>").replace(s, " ")
        s = Regex("(?s)<!--.*?-->").replace(s, " ")

        // Переводы строк и абзацы превращаем в пробелы
        s = Regex("(?i)<br\\s*/?>").replace(s, " ")
        s = Regex("(?i)</(p|div|li|h[1-6]|tr|blockquote)>").replace(s, " ")

        // Вырезаем все остальные теги
        s = Regex("(?s)<[^>]*>").replace(s, " ")

        // Раскрываем HTML-сущности (двухпроходно — для двойного кодирования)
        repeat(2) {
            s = Regex("&#(\\d+);").replace(s) { m ->
                m.groupValues[1].toIntOrNull()?.takeIf { it in 1..0xFFFF }?.toChar()?.toString() ?: ""
            }
            s = Regex("&#x([0-9a-fA-F]+);").replace(s) { m ->
                m.groupValues[1].toIntOrNull(16)?.takeIf { it in 1..0xFFFF }?.toChar()?.toString() ?: ""
            }
            NAMED_ENTITIES.forEach { (k, v) -> s = s.replace(k, v) }
        }

        // Схлопываем пробельные символы
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }
}
