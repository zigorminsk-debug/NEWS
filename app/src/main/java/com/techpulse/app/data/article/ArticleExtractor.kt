package com.techpulse.app.data.article

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Извлекает из веб-страницы «читаемую» статью (упрощённый readability):
 * заголовок, обложку и поток блоков (абзацы, подзаголовки, цитаты, картинки).
 * Это позволяет показывать статьи внутри приложения и переводить их.
 */
object ArticleExtractor {

    sealed class Block {
        data class Heading(val text: String) : Block()
        data class Paragraph(val text: String) : Block()
        data class Quote(val text: String) : Block()
        data class Image(val url: String) : Block()
    }

    data class Article(
        val url: String,
        val title: String,
        val siteName: String?,
        val heroImageUrl: String?,
        val blocks: List<Block>
    )

    /** Текстовое содержимое блока (null для картинок). */
    fun blockText(block: Block): String? = when (block) {
        is Block.Heading -> block.text
        is Block.Paragraph -> block.text
        is Block.Quote -> block.text
        is Block.Image -> null
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0 Safari/537.36 TechPulse/1.0"
    private const val MAX_BLOCKS = 140
    private const val MAX_BLOCK_CHARS = 2500
    private const val MIN_ARTICLE_CHARS = 240

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val JUNK_AT_START = Regex(
        "(?i)^(подпис|поддерж|читайте также|читать далее|©|copyright|all rights|" +
            "subscribe|sign in|sign up|follow us|advertisement|cookie|read more|share this)"
    )

    suspend fun load(url: String): Article = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru,en;q=0.8")
            .build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Пустой ответ")
        }
        parse(url, html)
    }

    fun parse(url: String, html: String): Article {
        val doc = Jsoup.parse(html, url)

        val title = meta(doc, "og:title")
            ?: doc.selectFirst("h1")?.text()?.clean()?.takeIf { it.length >= 4 }
            ?: doc.title().orEmpty().clean()

        val siteName = meta(doc, "og:site_name") ?: meta(doc, "application-name")
        val hero = metaContent(doc, "og:image")
            ?.let { toAbsolute(doc, it) }
            ?.takeIf { it.startsWith("http") }

        // Вырезаем всё, что не является текстом статьи
        doc.select(
            "script, style, noscript, iframe, form, aside, nav, svg, button, input, " +
                "select, textarea, footer, [role=banner], [role=navigation], " +
                ".ads, .ad, .advertisement, .social, .share, .sharing, " +
                ".comments, #comments, .related, .promo, .newsletter, .subscribe"
        ).remove()

        val root = pickContentRoot(doc) ?: doc.body() ?: throw IOException("Нет содержимого")
        val blocks = extractBlocks(root, hero, title)
        val totalLength = blocks.sumOf { blockText(it)?.length ?: 0 }
        if (totalLength < MIN_ARTICLE_CHARS) {
            throw IOException("Не удалось извлечь текст статьи")
        }
        return Article(url, title, siteName, hero, blocks.take(MAX_BLOCKS))
    }

    // ------------------------------------------------------------------
    // Выбор контейнера статьи
    // ------------------------------------------------------------------

    private fun pickContentRoot(doc: Document): Element? {
        val candidates = doc.select(
            "article, main, [role=main], .post-content, .entry-content, .article-content, " +
                ".article-body, .post-body, .story-content, .content-body, .tm-article-body, " +
                "#article-body, #content, .content"
        )
        var best: Element? = null
        var bestScore = 0
        for (el in candidates) {
            val score = scoreElement(el)
            if (score > bestScore) {
                bestScore = score
                best = el
            }
        }
        if (best != null && bestScore >= 200) return best
        val body = doc.body() ?: return null
        return if (scoreElement(body) >= 200) body else null
    }

    private fun scoreElement(el: Element): Int {
        var score = 0
        for (p in el.select("p")) {
            val len = p.text().length
            if (len > 40) score += len
        }
        return score
    }

    // ------------------------------------------------------------------
    // Сборка блоков
    // ------------------------------------------------------------------

    private fun extractBlocks(root: Element, hero: String?, title: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val seenImages = HashSet<String>()
        if (hero != null) seenImages += hero
        var titleConsumed = false

        for (el in root.select("p, h1, h2, h3, h4, blockquote, li, img")) {
            if (blocks.size >= MAX_BLOCKS) break
            if (hasAncestor(el, "blockquote")) continue
            when (el.tagName()) {
                "p", "li" -> {
                    if (el.tagName() == "li" && hasAncestor(el, "li")) continue
                    val prefix = if (el.tagName() == "li") "• " else ""
                    addText(el, prefix, blocks)?.let { blocks += Block.Paragraph(it) }
                }

                "blockquote" -> {
                    val text = el.text().clean()
                    if (text.length >= 25) blocks += Block.Quote(text.trunc())
                }

                "h1", "h2", "h3", "h4" -> {
                    val text = el.text().clean()
                    // Первый заголовок обычно дублирует title статьи
                    if (!titleConsumed && text.equals(title, ignoreCase = true)) {
                        titleConsumed = true
                        continue
                    }
                    if (text.length in 3..200) blocks += Block.Heading(text)
                }

                "img" -> {
                    val src = toAbsolute(
                        el,
                        el.attr("src").ifBlank { el.attr("data-src") }.ifBlank { el.attr("data-original") }
                    )
                    val width = el.attr("width").toIntOrNull() ?: 999
                    if (
                        src.startsWith("http") &&
                        seenImages.add(src) &&
                        width >= 120 &&
                        !src.contains("sprite", true) &&
                        !src.contains("avatar", true)
                    ) {
                        blocks += Block.Image(src)
                    }
                }
            }
        }
        return blocks
    }

    /** Готовит текст абзаца; null — абзац мусорный/слишком короткий. */
    private fun addText(el: Element, prefix: String, blocks: List<Block>): String? {
        if (!el.hasText()) return null
        val text = el.text().clean()
        if (text.length < 40) return null
        if (JUNK_AT_START.containsMatchIn(text)) return null
        if (linkDensity(el) > 0.65f) return null
        val full = (prefix + text).trunc()
        val lastText = blocks.lastOrNull()?.let { blockText(it) }
        if (full == lastText) return null
        return full
    }

    private fun linkDensity(el: Element): Float {
        val all = el.text().length
        if (all == 0) return 0f
        var inLinks = 0
        for (a in el.select("a")) inLinks += a.text().length
        return inLinks.toFloat() / all
    }

    private fun hasAncestor(el: Element, tag: String): Boolean {
        var node = el.parent()
        while (node != null) {
            if (node.tagName() == tag) return true
            node = node.parent()
        }
        return false
    }

    // ------------------------------------------------------------------
    // Утилиты
    // ------------------------------------------------------------------

    private fun meta(doc: Document, property: String): String? =
        doc.selectFirst("meta[property=$property], meta[name=$property]")
            ?.attr("content")?.clean()?.takeIf { it.isNotBlank() }

    private fun metaContent(doc: Document, property: String): String? =
        doc.selectFirst("meta[property=$property], meta[name=$property]")?.attr("content")

    private fun toAbsolute(doc: Document, url: String): String = absolutize(doc.baseUri(), url)

    private fun toAbsolute(el: Element, url: String): String = absolutize(el.baseUri(), url)

    private fun absolutize(baseUri: String, url: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("http")) return url
        return try {
            java.net.URI(baseUri).resolve(url).toString()
        } catch (_: Exception) {
            url
        }
    }

    private fun String.clean(): String =
        replace("\\s+".toRegex(), " ").trim()

    private fun String.trunc(): String =
        if (length > MAX_BLOCK_CHARS) take(MAX_BLOCK_CHARS) else this
}
