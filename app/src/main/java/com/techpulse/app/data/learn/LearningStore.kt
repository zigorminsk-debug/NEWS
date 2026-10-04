package com.techpulse.app.data.learn

import android.content.Context
import kotlin.math.roundToInt

/**
 * Хранилище прогресса обучения чтению английских текстов:
 *
 *  - [known]   — слова, которые пользователь отметил «знаю» (подсказки не показываем);
 *  - [learned] — слова, которые встречались в подсказках много раз и считаются усвоенными;
 *  - [stats]   — сколько раз подсказка к слову была показана;
 *  - [articlesRead] — сколько англоязычных статей прочитано (открыт ридер со статьёй).
 *
 * Ключевая механика: чем больше статей прочитано и чем чаще слово видели,
 * тем МЕНЬШЕ подсказок-переводов показывается (постепенное убавление помощи).
 */
class LearningStore private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("techpulse_learning", Context.MODE_PRIVATE)

    private val known = HashSet<String>()
    private val learned = HashSet<String>()
    private val stats = HashMap<String, Int>()

    var articlesRead: Int = 0
        private set

    /** Авто-перевод полных абзацев (запоминается между запусками). */
    var autoTranslateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_TRANSLATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, value).apply()

    /** Режим обучения: точечные подсказки к словам (запоминается). */
    var learningEnabled: Boolean
        get() = prefs.getBoolean(KEY_LEARNING, false)
        set(value) = prefs.edit().putBoolean(KEY_LEARNING, value).apply()

    init {
        articlesRead = prefs.getInt(KEY_ARTICLES_READ, 0)
        known += prefs.getStringSet(KEY_KNOWN, emptySet()).orEmpty()
        learned += prefs.getStringSet(KEY_LEARNED, emptySet()).orEmpty()
        val rawStats = prefs.getString(KEY_STATS, "").orEmpty()
        if (rawStats.isNotEmpty()) {
            rawStats.split(';').forEach { entry ->
                val parts = entry.split('=')
                if (parts.size == 2) {
                    val count = parts[1].toIntOrNull()
                    if (parts[0].isNotBlank() && count != null) stats[parts[0]] = count
                }
            }
        }
    }

    val knownCount: Int get() = known.size
    val learnedCount: Int get() = learned.size

    fun isKnown(word: String): Boolean = word.lowercase() in known
    fun isLearned(word: String): Boolean = word.lowercase() in learned

    /** Пользователь нажал «Я знаю это слово». */
    fun markKnown(word: String) {
        val key = word.lowercase()
        known += key
        learned -= key
        persist()
    }

    /** Пользователь нажал «Больше не подсказывать» (слово засчитано как изучаемое/надоевшее). */
    fun stopHinting(word: String) {
        learned += word.lowercase()
        persist()
    }

    /** Фиксирует показ подсказок (батч на статью). */
    fun registerHints(words: Collection<String>) {
        var changed = false
        for (raw in words) {
            val key = raw.lowercase()
            if (key in known || key in learned) continue
            val shown = (stats[key] ?: 0) + 1
            stats[key] = shown
            changed = true
            if (shown >= LEARN_THRESHOLD) {
                learned += key
            }
        }
        if (changed) persist()
    }

    /** Статья прочитана до конца открытия (ридер закрыт с загруженным контентом). */
    fun onArticleRead() {
        articlesRead += 1
        persist()
    }

    /**
     * Доля «сложных» слов абзаца, к которым показываем перевод-подсказку.
     * Плавно убывает по мере чтения статей: сначала подсказки почти везде,
     * затем всё реже — пользователь начинает читать без перевода.
     */
    fun hintDensity(): Float = when {
        articlesRead < 5 -> 0.55f
        articlesRead < 15 -> 0.40f
        articlesRead < 30 -> 0.28f
        articlesRead < 60 -> 0.18f
        else -> 0.10f
    }

    private fun persist() {
        prefs.edit()
            .putInt(KEY_ARTICLES_READ, articlesRead)
            .putStringSet(KEY_KNOWN, HashSet(known))
            .putStringSet(KEY_LEARNED, HashSet(learned))
            .putString(
                KEY_STATS,
                stats.entries.joinToString(";") { (k, v) -> "$k=$v" }
            )
            .apply()
    }

    companion object {
        /** После стольких показов подсказки слово считается усвоенным. */
        private const val LEARN_THRESHOLD = 6

        private const val KEY_KNOWN = "known_words"
        private const val KEY_LEARNED = "learned_words"
        private const val KEY_STATS = "word_stats"
        private const val KEY_ARTICLES_READ = "articles_read"
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_LEARNING = "learning_mode"

        @Volatile
        private var instance: LearningStore? = null

        @Synchronized
        fun init(context: Context) {
            if (instance == null) instance = LearningStore(context.applicationContext)
        }

        fun get(context: Context): LearningStore =
            instance ?: synchronized(this) {
                instance ?: LearningStore(context.applicationContext).also { instance = it }
            }
    }
}

/**
 * Разметка текста на слова и выбор слов-кандидатов для подсказок-переводов.
 */
object WordEngine {

    private val WORD_REGEX = Regex("[A-Za-z]+(?:['’\\-][A-Za-z]+)*")
    private const val MAX_HINTS_PER_PARAGRAPH = 5

    data class Token(val text: String, val isWord: Boolean)

    /** Разбивает текст на чередующиеся «слово/не-слово» токены, сохраняя исходную строку. */
    fun tokenize(text: String): List<Token> {
        val tokens = ArrayList<Token>()
        var last = 0
        for (match in WORD_REGEX.findAll(text)) {
            if (match.range.first > last) {
                tokens += Token(text.substring(last, match.range.first), isWord = false)
            }
            tokens += Token(match.value, isWord = true)
            last = match.range.last + 1
        }
        if (last < text.length) tokens += Token(text.substring(last), isWord = false)
        return tokens
    }

    /** Нормализация для словаря/кэша: нижний регистр, без висячих апострофов. */
    fun normalize(word: String): String =
        word.lowercase().trim('\'', '’', '-')

    /** Может ли слово быть кандидатом на подсказку (исключаем общеизвестные и мусор). */
    fun isEligibleWord(word: String): Boolean {
        if (word.length < 4) return false
        if (!word.any { it.isLetter() }) return false
        // Аббревиатуры (API, NASA, SDK) не переводим
        if (word.length <= 6 && word.count { it.isUpperCase() } >= word.count { it.isLetter() } - 1 &&
            word.any { it.isUpperCase() }
        ) return false
        // Любая из словоформ в списке общеизвестных — подсказка не нужна
        return formsOf(word).none { it in CommonWords.ENGLISH }
    }

    /** Грубые словоформы для сверки со списком частотных слов. */
    private fun formsOf(word: String): List<String> {
        val base = normalize(word)
        if (base.length < 3) return listOf(base)
        return buildList {
            add(base)
            if (base.endsWith("'s") || base.endsWith("’s")) add(base.dropLast(2))
            if (base.length > 4 && base.endsWith("s") && !base.endsWith("ss")) add(base.dropLast(1))
            if (base.length > 5 && base.endsWith("es")) add(base.dropLast(2))
            if (base.length > 5 && base.endsWith("ies")) add(base.dropLast(3) + "y")
            if (base.length > 5 && base.endsWith("ing")) add(base.dropLast(3))
            if (base.length > 4 && base.endsWith("ed") && !base.endsWith("eed")) add(base.dropLast(2))
        }
    }

    /**
     * Выбирает слова абзаца, к которым будут показаны подсказки-переводы:
     *  - слово не общеизвестное, не «известное» и не «усвоенное»;
     *  - количество = доля [density] от числа кандидатов (не больше пяти на абзац);
     *  - выбор детерминирован парой (слово, seed статьи), чтобы подсказки
     *    не «прыгали» при перерисовке экрана.
     *
     * @return множество ключей (normalize-форма) выбранных слов.
     */
    fun selectHints(
        text: String,
        articleSeed: Int,
        density: Float,
        store: LearningStore
    ): Set<String> {
        val candidates = tokenize(text)
            .asSequence()
            .filter { it.isWord }
            .map { it.text }
            .filter { isEligibleWord(it) }
            .map(::normalize)
            .filter { it.length >= 3 && !store.isKnown(it) && !store.isLearned(it) }
            .distinct()
            .toList()

        if (candidates.isEmpty()) return emptySet()

        val target = (candidates.size * density)
            .roundToInt()
            .coerceIn(1, MAX_HINTS_PER_PARAGRAPH)

        return candidates
            .sortedBy { stableScore(it, articleSeed) }
            .take(target)
            .toSet()
    }

    private fun stableScore(word: String, seed: Int): Int {
        var hash = seed xor 0x5bd1e995
        for (ch in word) hash = hash * 31 + ch.code
        return hash
    }
}
