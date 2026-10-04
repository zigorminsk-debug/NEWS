package com.techpulse.app.data.translate

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Переводчик на базе открытого endpoint'а Google Translate (client=gtx).
 *
 * Особенности:
 *  - мягкий rate-limit, чтобы не получить бан по частоте запросов;
 *  - двухуровневый кэш (память + JSON-файл): повторные слова и абзацы
 *    переводятся мгновенно и без сети;
 *  - устойчивость к сбоям: при ошибке сети возвращает null, вызывающий
 *    код просто показывает оригинал.
 */
object Translator {

    data class WordTranslation(val main: String, val alternatives: List<String>)

    private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
    private const val MIN_INTERVAL_MS = 90L
    private const val MAX_CHARS = 1500

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val rateGate = Mutex()
    private var lastCallAt = 0L

    @Volatile
    private var store: TranslationCache? = null

    @Synchronized
    fun init(context: Context) {
        if (store == null) {
            store = TranslationCache(context.applicationContext)
        }
    }

    // ------------------------------------------------------------------
    // Публичное API
    // ------------------------------------------------------------------

    /** Мгновенный результат из кэша (null — перевода в кэше нет). */
    fun cachedText(text: String, target: String = "ru"): String? =
        store?.getText(text.trim(), target)

    fun cachedWord(word: String, source: String = "en", target: String = "ru"): String? =
        store?.getWord(word.trim().lowercase(), source, target)

    /** Перевод фрагмента текста. null — перевести не удалось / перевод не нужен. */
    suspend fun translateText(text: String, target: String = "ru", source: String = "auto"): String? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return null

        store?.getText(cleaned, target)?.let { return it }

        // Уже на русском — переводить нечего
        if (target == "ru" && isMostlyCyrillic(cleaned)) return null

        val translated = request(cleaned.take(MAX_CHARS), source, target) ?: return null
        if (translated.isBlank() || translated.equals(cleaned, ignoreCase = true)) return null

        store?.putText(cleaned, target, translated)
        return translated
    }

    /** Перевод отдельного слова + альтернативные варианты (словарь). */
    suspend fun translateWord(word: String, source: String = "en", target: String = "ru"): WordTranslation? {
        val key = word.trim().lowercase()
        if (key.isEmpty()) return null

        store?.getWord(key, source, target)?.let { return WordTranslation(it, emptyList()) }

        val raw = requestRaw(key, source, target) ?: return null
        val main = parseMain(raw)?.takeIf { it.isNotBlank() } ?: return null

        store?.putWord(key, source, target, main)
        val alternatives = parseAlternatives(raw)
            .filter { !it.equals(main, ignoreCase = true) && !it.equals(key, ignoreCase = true) }
            .distinct()
            .take(4)
        return WordTranslation(main, alternatives)
    }

    // ------------------------------------------------------------------
    // Детектирование языка
    // ------------------------------------------------------------------

    /** true, если текст преимущественно кириллический (русский). */
    fun isMostlyCyrillic(text: String): Boolean {
        var letters = 0
        var cyrillic = 0
        for (ch in text) {
            if (ch.isLetter()) {
                letters++
                if (ch in '\u0400'..'\u04FF') cyrillic++
            }
        }
        return letters > 0 && cyrillic * 100 / letters >= 40
    }

    // ------------------------------------------------------------------
    // Сеть
    // ------------------------------------------------------------------

    private suspend fun request(text: String, source: String, target: String): String? =
        requestRaw(text, source, target)?.let { parseMain(it) }

    private suspend fun requestRaw(text: String, source: String, target: String): String? =
        withContext(Dispatchers.IO) {
            repeat(2) { attempt ->
                try {
                    rateGate.withLock {
                        val wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastCallAt)
                        if (wait > 0) delay(wait)
                        lastCallAt = System.currentTimeMillis()
                    }
                    val url = ENDPOINT +
                        "?client=gtx&sl=" + URLEncoder.encode(source, "UTF-8") +
                        "&tl=" + URLEncoder.encode(target, "UTF-8") +
                        "&dt=t&dt=bd&q=" + URLEncoder.encode(text, "UTF-8")
                    val request = Request.Builder()
                        .url(url)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
                        )
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            return@withContext response.body?.string()
                        }
                    }
                } catch (_: Exception) {
                    if (attempt == 0) delay(700)
                }
            }
            null
        }

    private fun parseMain(raw: String): String? = try {
        val segments = JSONArray(raw).getJSONArray(0)
        buildString {
            for (i in 0 until segments.length()) {
                val segment = segments.optJSONArray(i) ?: continue
                append(segment.optString(0))
            }
        }.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun parseAlternatives(raw: String): List<String> = try {
        val dict = JSONArray(raw).optJSONArray(1) ?: return emptyList()
        val result = mutableListOf<String>()
        for (i in 0 until dict.length()) {
            val entry = dict.optJSONArray(i) ?: continue
            val words = entry.optJSONArray(1) ?: continue
            for (j in 0 until words.length()) {
                val word = words.optString(j)
                if (word.isNotBlank()) result += word
            }
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Двухуровневый кэш переводов: память + JSON-файл на диске.
 * Слова и тексты хранятся раздельно, запись на диск лимитирована по частоте.
 */
private class TranslationCache(context: Context) {

    private val file = File(context.filesDir, "translation_cache.json")
    private val texts = HashMap<String, String>()
    private val words = HashMap<String, String>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveMutex = Mutex()
    private var dirty = false
    private var lastSaveAt = 0L

    init {
        scope.launch { load() }
    }

    fun getText(text: String, target: String): String? = synchronized(this) {
        texts["t:$target:${md5(text)}"]
    }

    fun getWord(word: String, source: String, target: String): String? = synchronized(this) {
        words["$source>$target:$word"]
    }

    fun putText(text: String, target: String, value: String) {
        synchronized(this) {
            if (texts.size > MAX_TEXTS) texts.clear()
            texts["t:$target:${md5(text)}"] = value
            dirty = true
        }
        scheduleSave()
    }

    fun putWord(word: String, source: String, target: String, value: String) {
        synchronized(this) {
            if (words.size > MAX_WORDS) words.clear()
            words["$source>$target:$word"] = value
            dirty = true
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        scope.launch {
            val wait = SAVE_INTERVAL_MS - (System.currentTimeMillis() - lastSaveAt)
            if (wait > 0) delay(wait)
            persist()
        }
    }

    private suspend fun persist() {
        saveMutex.withLock {
            val now = System.currentTimeMillis()
            val snapshotTexts: HashMap<String, String>
            val snapshotWords: HashMap<String, String>
            synchronized(this) {
                if (!dirty || now - lastSaveAt < SAVE_INTERVAL_MS) return
                snapshotTexts = HashMap(texts)
                snapshotWords = HashMap(words)
                dirty = false
                lastSaveAt = now
            }
            try {
                val root = JSONObject()
                val t = JSONObject()
                snapshotTexts.forEach { (k, v) -> t.put(k, v) }
                val w = JSONObject()
                snapshotWords.forEach { (k, v) -> w.put(k, v) }
                root.put("texts", t)
                root.put("words", w)
                file.writeText(root.toString())
            } catch (_: Exception) {
                // Ошибка записи кэша не критична
            }
        }
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext
            val root = JSONObject(file.readText())
            val loadedTexts = HashMap<String, String>()
            root.optJSONObject("texts")?.let { obj ->
                val it = obj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    loadedTexts[k] = obj.optString(k)
                }
            }
            val loadedWords = HashMap<String, String>()
            root.optJSONObject("words")?.let { obj ->
                val it = obj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    loadedWords[k] = obj.optString(k)
                }
            }
            synchronized(this) {
                texts.putAll(loadedTexts)
                words.putAll(loadedWords)
            }
        } catch (_: Exception) {
            // Повреждённый кэш — игнорируем
        }
    }

    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) sb.append("%02x".format(b))
        return sb.toString()
    }

    companion object {
        private const val MAX_TEXTS = 2500
        private const val MAX_WORDS = 15000
        private const val SAVE_INTERVAL_MS = 2500L
    }
}
