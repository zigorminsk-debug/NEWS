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

    /**
     * Начальный уровень обучения, выбранный пользователем: 1..10.
     * 1 — «почти всё переведено», 10 — подсказок почти нет.
     */
    var learningLevel: Int
        get() = prefs.getInt(KEY_LEVEL, DEFAULT_LEVEL)
        set(value) = prefs.edit().putInt(KEY_LEVEL, value.coerceIn(1, MAX_LEVEL)).apply()

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
     * Текущий уровень: к начальному уровню добавляется прогресс —
     * каждые [ARTICLES_PER_LEVEL] прочитанных статьи повышают уровень на 1,
     * поэтому количество подсказок со временем уменьшается само.
     */
    fun currentLevel(): Int =
        (learningLevel + articlesRead / ARTICLES_PER_LEVEL).coerceIn(1, MAX_LEVEL)

    /** Доля «сложных» слов с подсказкой для заданного уровня: 1 → 95%, 10 → 5%. */
    fun densityForLevel(level: Int): Float {
        val t = (level.coerceIn(1, MAX_LEVEL) - 1) / (MAX_LEVEL - 1).toFloat()
        return START_DENSITY + (END_DENSITY - START_DENSITY) * t
    }

    /** Доля «сложных» слов с подсказкой на текущем уровне. */
    fun hintDensity(): Float = densityForLevel(currentLevel())

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

        /** Уровни обучения: 1 — переведено почти всё, 10 — почти без перевода. */
        const val MAX_LEVEL = 10
        const val DEFAULT_LEVEL = 1

        /** Каждые столько прочитанных статей уровень повышается на 1 автоматически. */
        const val ARTICLES_PER_LEVEL = 3

        /** Плотность подсказок на уровне 1 / уровне 10. */
        private const val START_DENSITY = 0.95f
        private const val END_DENSITY = 0.05f

        private const val KEY_KNOWN = "known_words"
        private const val KEY_LEARNED = "learned_words"
        private const val KEY_STATS = "word_stats"
        private const val KEY_ARTICLES_READ = "articles_read"
        private const val KEY_AUTO_TRANSLATE = "auto_translate"
        private const val KEY_LEARNING = "learning_mode"
        private const val KEY_LEVEL = "learning_level"

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
    private const val MAX_HINTS_PER_PARAGRAPH_DENSE = 8
    private const val MAX_ARTICLE_HINT_WORDS = 80

    /**
     * План подсказок для всей статьи.
     *
     * Правило повторений: первое вхождение слова-кандидата идёт с переводом,
     * второе — только с маркером-подсказкой (подчёркивание), третье и далее —
     * без перевода и маркера.
     *
     * @param wordsInBlock блок -> слова-кандидаты, встречающиеся в этом блоке;
     * @param preCount     блок -> (слово -> сколько раз оно встретилось в предыдущих блоках);
     * @param selected     все слова-кандидаты статьи (для предзагрузки переводов).
     */
    class ArticleHints(
        val wordsInBlock: Map<Int, Set<String>>,
        val preCount: Map<Int, Map<String, Int>>,
        val selected: Set<String>
    ) {
        companion object {
            val EMPTY = ArticleHints(emptyMap(), emptyMap(), emptySet())
        }
    }

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

        val cap = if (density >= 0.7f) MAX_HINTS_PER_PARAGRAPH_DENSE else MAX_HINTS_PER_PARAGRAPH
        val target = (candidates.size * density)
            .roundToInt()
            .coerceIn(1, cap)

        return candidates
            .sortedBy { stableScore(it, articleSeed) }
            .take(target)
            .toSet()
    }

    /**
     * Строит общий план подсказок для статьи: объединяет выбор по абзацам,
     * ограничивает общее количество слов и подсчитывает вхождения каждого
     * слова по всей статье (для правила «1-я встреча — перевод, 2-я — маркер,
     * 3-я — без перевода»).
     *
     * @param texts тексты блоков статьи по порядку; null — блок без текста.
     */
    fun buildArticleHints(
        texts: List<String?>,
        articleSeed: Int,
        density: Float,
        store: LearningStore
    ): ArticleHints {
        // Кандидаты по каждому блоку (учитывают известные/усвоенные слова)
        val perBlock: List<Set<String>> = texts.map { text ->
            if (text.isNullOrBlank()) emptySet() else selectHints(text, articleSeed, density, store)
        }
        val union = LinkedHashSet<String>()
        perBlock.forEach { union.addAll(it) }
        if (union.isEmpty()) return ArticleHints.EMPTY

        // Нормализованные токены-слова каждого блока в порядке следования
        val blockTokens: List<List<String>> = texts.map { text ->
            if (text.isNullOrBlank()) {
                emptyList()
            } else {
                tokenize(text).asSequence()
                    .filter { it.isWord }
                    .map { normalize(it.text) }
                    .toList()
            }
        }

        // Ограничение на общее число слов: приоритет — по первому появлению в тексте
        val limited: Set<String> = if (union.size <= MAX_ARTICLE_HINT_WORDS) {
            union
        } else {
            val firstIndex = HashMap<String, Int>()
            var cursor = 0
            blockTokens.forEach { tokens ->
                for (token in tokens) {
                    if (token in union && !firstIndex.containsKey(token)) {
                        firstIndex[token] = cursor
                    }
                    cursor++
                }
            }
            union.sortedBy { firstIndex[it] ?: Int.MAX_VALUE }
                .take(MAX_ARTICLE_HINT_WORDS)
                .toSet()
        }

        // Обходим статью с начала: для каждого блока фиксируем, сколько раз
        // каждое слово-кандидат уже встречалось раньше
        val counts = HashMap<String, Int>()
        val preCount = HashMap<Int, Map<String, Int>>()
        val wordsInBlock = HashMap<Int, Set<String>>()
        blockTokens.forEachIndexed { index, tokens ->
            if (!texts[index].isNullOrBlank()) {
                val inBlock = tokens
                    .asSequence()
                    .filter { it in limited }
                    .toCollection(LinkedHashSet())
                if (inBlock.isNotEmpty()) {
                    preCount[index] = inBlock.associateWith { counts[it] ?: 0 }
                    wordsInBlock[index] = inBlock
                }
            }
            for (token in tokens) {
                if (token in limited) counts[token] = (counts[token] ?: 0) + 1
            }
        }
        return ArticleHints(wordsInBlock, preCount, limited)
    }

    private fun stableScore(word: String, seed: Int): Int {
        var hash = seed xor 0x5bd1e995
        for (ch in word) hash = hash * 31 + ch.code
        return hash
    }
}
