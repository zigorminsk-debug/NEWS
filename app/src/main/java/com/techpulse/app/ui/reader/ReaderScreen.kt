package com.techpulse.app.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.techpulse.app.FeedViewModel
import com.techpulse.app.data.FeedItem
import com.techpulse.app.data.article.ArticleExtractor
import com.techpulse.app.data.learn.LearningStore
import com.techpulse.app.data.learn.WordEngine
import com.techpulse.app.data.translate.Translator
import com.techpulse.app.ui.components.EmptyState
import com.techpulse.app.ui.components.GradientDivider
import com.techpulse.app.ui.components.LoadingIndicator
import com.techpulse.app.ui.formatHost
import com.techpulse.app.ui.openUrl
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.AccentGreen
import com.techpulse.app.ui.theme.Background
import com.techpulse.app.ui.theme.SurfaceDark
import com.techpulse.app.ui.theme.SurfaceElevated
import com.techpulse.app.ui.theme.TextPrimary
import com.techpulse.app.ui.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private sealed interface ArticleUi {
    data object Loading : ArticleUi
    data class Success(val article: ArticleExtractor.Article) : ArticleUi
    data class Error(val message: String) : ArticleUi
}

/**
 * Встроенный ридер статьи.
 *
 *  - Англоязычные статьи по умолчанию автоматически переводятся на русский
 *    (переключатель «Авто-перевод»). Тап по переведённому абзацу показывает оригинал.
 *  - Переключатель «Обучение» включает режим с подсказками: общеизвестные слова
 *    не переводятся, к «сложным» словам показываются мини-переводы, а по мере
 *    прочтения статей доля подсказок постепенно снижается.
 *  - Тап по любому английскому слову открывает карточку с переводом.
 */
@Composable
fun ReaderScreen(
    item: FeedItem,
    viewModel: FeedViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val learningStore = remember { LearningStore.get(context) }

    val articleUi by produceState<ArticleUi>(ArticleUi.Loading, item.link) {
        value = try {
            ArticleUi.Success(ArticleExtractor.load(item.link))
        } catch (e: Exception) {
            ArticleUi.Error(e.message ?: "Не удалось открыть статью")
        }
    }

    val isRussianArticle = remember(item.link) {
        Translator.isMostlyCyrillic(item.title + " " + item.summary)
    }

    var autoTranslate by remember(item.link) { mutableStateOf(!isRussianArticle && learningStore.autoTranslateEnabled) }
    var learningMode by remember(item.link) { mutableStateOf(!isRussianArticle && learningStore.learningEnabled) }
    var bookmarked by remember(item.link) { mutableStateOf(viewModel.isBookmarked(item)) }

    // Переводы абзацев/заголовка (авто-перевод)
    var translations by remember(item.link) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var translatedTitle by remember(item.link) { mutableStateOf<String?>(null) }
    var totalToTranslate by remember(item.link) { mutableStateOf(1) }
    var translatedCount by remember(item.link) { mutableStateOf(0) }
    // Абзацы, для которых пользователь попросил показать оригинал
    var originalShown by remember(item.link) { mutableStateOf<Set<Int>>(emptySet()) }

    // Подсказки режима обучения
    var hintTranslations by remember(item.link) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var tappedWord by remember { mutableStateOf<String?>(null) }

    val article = (articleUi as? ArticleUi.Success)?.article
    val articleSeed = remember(item.link) { item.link.hashCode() }
    val density = remember(item.link) { learningStore.hintDensity() }

    // Детерминированный выбор слов-подсказок для статьи
    val hintSelection: Map<Int, Set<String>> = remember(article, learningMode) {
        val a = article
        if (a == null || !learningMode) {
            emptyMap()
        } else {
            a.blocks.mapIndexedNotNull { index, block ->
                val text = ArticleExtractor.blockText(block) ?: return@mapIndexedNotNull null
                val selection = WordEngine.selectHints(text, articleSeed, density, learningStore)
                if (selection.isEmpty()) null else index to selection
            }.toMap()
        }
    }

    BackHandler(onBack = onBack)

    // Прогресс чтения: статья засчитывается при закрытии успешно загруженного ридера
    DisposableEffect(item.link, article != null) {
        onDispose {
            if (article != null && !isRussianArticle) {
                learningStore.onArticleRead()
            }
        }
    }

    // Авто-перевод: заголовок, затем абзацы по очереди (с кэшем)
    LaunchedEffect(item.link, autoTranslate, article) {
        val a = article ?: return@LaunchedEffect
        if (!autoTranslate || isRussianArticle) return@LaunchedEffect

        if (translatedTitle == null && !Translator.isMostlyCyrillic(a.title)) {
            Translator.translateText(a.title, "ru")?.let { translatedTitle = it }
        }

        val targets = a.blocks.mapIndexedNotNull { index, block ->
            val text = ArticleExtractor.blockText(block)
                ?.takeIf { it.length >= 24 && !Translator.isMostlyCyrillic(it) }
            if (text != null) index to text else null
        }
        totalToTranslate = targets.size
        translatedCount = targets.count { translations.containsKey(it.first) }

        for ((index, text) in targets) {
            if (!isActive) return@LaunchedEffect
            if (translations.containsKey(index)) continue
            val translated = Translator.translateText(text, "ru") ?: continue
            translations = translations + (index to translated)
            translatedCount += 1
            delay(60)
        }
    }

    // Предзагрузка переводов подсказок режима обучения
    LaunchedEffect(hintSelection) {
        val words = hintSelection.values.flatten().distinct()
        if (words.isEmpty()) return@LaunchedEffect
        learningStore.registerHints(words)

        val fromCache = words.mapNotNull { w -> Translator.cachedWord(w, "en", "ru")?.let { w to it } }
        if (fromCache.isNotEmpty()) hintTranslations = hintTranslations + fromCache.toMap()

        val missing = words.filterNot { w -> fromCache.any { it.first == w } }
        coroutineScope {
            for (chunk in missing.chunked(4)) {
                val results = chunk
                    .map { word -> async { Translator.translateWord(word, "en", "ru")?.main?.let { word to it } } }
                    .awaitAll()
                    .filterNotNull()
                if (results.isNotEmpty()) hintTranslations = hintTranslations + results.toMap()
            }
        }
    }

    val translatingNow = autoTranslate && !isRussianArticle && article != null &&
        translatedCount < totalToTranslate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ReaderTopBar(
            item = item,
            bookmarked = bookmarked,
            showLanguageTools = !isRussianArticle,
            autoTranslate = autoTranslate,
            learningMode = learningMode,
            onToggleTranslate = {
                autoTranslate = !autoTranslate
                if (autoTranslate) learningMode = false
                learningStore.autoTranslateEnabled = autoTranslate
                learningStore.learningEnabled = learningMode
            },
            onToggleLearning = {
                learningMode = !learningMode
                if (learningMode) autoTranslate = false
                learningStore.autoTranslateEnabled = autoTranslate
                learningStore.learningEnabled = learningMode
            },
            onOpenInBrowser = { openUrl(context, item.link) },
            onToggleBookmark = {
                viewModel.toggleBookmark(item)
                bookmarked = viewModel.isBookmarked(item)
            },
            onBack = onBack
        )

        if (translatingNow) {
            LinearProgressIndicator(
                progress = {
                    if (totalToTranslate > 0) translatedCount.toFloat() / totalToTranslate else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = AccentCyan,
                trackColor = Color.Transparent
            )
        }

        if (learningMode && !isRussianArticle) {
            LearningBanner(
                density = density,
                articlesRead = learningStore.articlesRead,
                learnedWords = learningStore.knownCount + learningStore.learnedCount
            )
        }

        when (val state = articleUi) {
            ArticleUi.Loading -> LoadingIndicator("Открываем статью…")

            is ArticleUi.Error -> EmptyState(
                icon = Icons.Filled.CloudOff,
                title = "Не удалось открыть статью",
                subtitle = state.message + "\nМожно открыть оригинал в браузере.",
                actionText = "Открыть в браузере",
                onAction = { openUrl(context, item.link) }
            )

            is ArticleUi.Success -> ArticleBody(
                article = state.article,
                item = item,
                isRussianArticle = isRussianArticle,
                autoTranslate = autoTranslate,
                learningMode = learningMode,
                translations = translations,
                translatedTitle = translatedTitle,
                originalShown = originalShown,
                onToggleOriginal = { index ->
                    originalShown = if (index in originalShown) {
                        originalShown - index
                    } else {
                        originalShown + index
                    }
                },
                hintSelection = hintSelection,
                hintTranslations = hintTranslations,
                onWordClick = { tappedWord = it }
            )
        }
    }

    tappedWord?.let { word ->
        WordTranslationSheet(
            word = word,
            learningStore = learningStore,
            onClose = { tappedWord = null }
        )
    }
}

// ----------------------------------------------------------------------
// Верхняя панель
// ----------------------------------------------------------------------

@Composable
private fun ReaderTopBar(
    item: FeedItem,
    bookmarked: Boolean,
    showLanguageTools: Boolean,
    autoTranslate: Boolean,
    learningMode: Boolean,
    onToggleTranslate: () -> Unit,
    onToggleLearning: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.sourceName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(item.sourceColorHex),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (showLanguageTools) "EN → RU · ридер" else "ридер · " + formatHost(item.link),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showLanguageTools) {
                IconButton(onClick = onToggleTranslate) {
                    Icon(
                        Icons.Filled.Translate,
                        contentDescription = "Авто-перевод статьи",
                        tint = if (autoTranslate) AccentCyan else TextSecondary
                    )
                }
                IconButton(onClick = onToggleLearning) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = "Режим обучения",
                        tint = if (learningMode) AccentGreen else TextSecondary
                    )
                }
            }
            IconButton(onClick = onOpenInBrowser) {
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = "Открыть в браузере",
                    tint = TextSecondary
                )
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Закладка",
                    tint = if (bookmarked) AccentCyan else TextSecondary
                )
            }
        }
        GradientDivider()
    }
}

/** Информационная плашка режима обучения: текущая интенсивность подсказок и прогресс. */
@Composable
private fun LearningBanner(density: Float, articlesRead: Int, learnedWords: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.School,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ОБУЧЕНИЕ · подсказки ${(density * 100).toInt()}% · " +
                    "статей: $articlesRead · слов изучено: $learnedWords",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

// ----------------------------------------------------------------------
// Тело статьи
// ----------------------------------------------------------------------

@Composable
private fun ArticleBody(
    article: ArticleExtractor.Article,
    item: FeedItem,
    isRussianArticle: Boolean,
    autoTranslate: Boolean,
    learningMode: Boolean,
    translations: Map<Int, String>,
    translatedTitle: String?,
    originalShown: Set<Int>,
    onToggleOriginal: (Int) -> Unit,
    hintSelection: Map<Int, Set<String>>,
    hintTranslations: Map<String, String>,
    onWordClick: (String) -> Unit
) {
    val showTranslatedTitle = autoTranslate && translatedTitle != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "meta") {
            Text(
                text = buildString {
                    append(item.sourceName)
                    article.siteName?.takeIf { it.isNotBlank() && !it.equals(item.sourceName, true) }
                        ?.let { append(" · ").append(it) }
                },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }

        item(key = "title") {
            Column {
                Text(
                    text = if (showTranslatedTitle) translatedTitle!! else article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 28.sp
                )
                if (showTranslatedTitle) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        article.heroImageUrl?.let { hero ->
            item(key = "hero") {
                AsyncImage(
                    model = hero,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(SurfaceElevated),
                    error = ColorPainter(SurfaceElevated)
                )
            }
        }

        itemsIndexed(article.blocks, key = { index, _ -> "block_$index" }) { index, block ->
            when (block) {
                is ArticleExtractor.Block.Image -> AsyncImage(
                    model = block.url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(SurfaceElevated),
                    error = ColorPainter(SurfaceElevated)
                )

                is ArticleExtractor.Block.Heading -> Text(
                    text = translations[index] ?: block.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                is ArticleExtractor.Block.Quote -> Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = translations[index] ?: block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                is ArticleExtractor.Block.Paragraph -> ParagraphBlock(
                    text = block.text,
                    isRussianArticle = isRussianArticle,
                    autoTranslate = autoTranslate,
                    learningMode = learningMode,
                    translation = translations[index],
                    showOriginal = index in originalShown,
                    onToggleOriginal = { onToggleOriginal(index) },
                    hintWords = hintSelection[index].orEmpty(),
                    hintTranslations = hintTranslations,
                    onWordClick = onWordClick
                )
            }
        }

        item(key = "reader_footer") {
            Text(
                text = "— конец статьи · ${formatHost(article.url)} —",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ParagraphBlock(
    text: String,
    isRussianArticle: Boolean,
    autoTranslate: Boolean,
    learningMode: Boolean,
    translation: String?,
    showOriginal: Boolean,
    onToggleOriginal: () -> Unit,
    hintWords: Set<String>,
    hintTranslations: Map<String, String>,
    onWordClick: (String) -> Unit
) {
    val style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp)

    Column {
        when {
            // Русская статья — оригинал как есть
            isRussianArticle -> Text(text = text, style = style, color = TextPrimary)

            // Переведённый абзац (тап — показать оригинал)
            autoTranslate && translation != null && !showOriginal -> {
                Column(modifier = Modifier.clickable(onClick = onToggleOriginal)) {
                    Text(text = translation, style = style, color = TextPrimary)
                    Text(
                        text = "переведено · тап — оригинал",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = AccentCyan.copy(alpha = 0.6f)
                    )
                }
            }

            // Оригинал с кликабельными словами (+ подсказки в режиме обучения)
            else -> {
                val visibleHints = if (learningMode) {
                    hintWords.mapNotNull { w -> hintTranslations[w]?.let { w to it } }.toMap()
                } else {
                    emptyMap()
                }
                val pendingHints = if (learningMode) hintWords - visibleHints.keys else emptySet()

                AnnotatedParagraph(
                    text = text,
                    hints = visibleHints,
                    pendingHints = pendingHints,
                    onWordClick = onWordClick,
                    style = style,
                    baseColor = TextPrimary
                )
                if (autoTranslate && translation != null && showOriginal) {
                    TextButton(onClick = onToggleOriginal, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            text = "показать перевод",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AccentCyan
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// Абзац с кликабельными словами и подсказками
// ----------------------------------------------------------------------

private const val TAG_WORD = "word"

/**
 * Абзац, в котором каждое английское слово кликабельно (показывает карточку
 * перевода), а к выбранным «сложным» словам рядом выводится мини-перевод.
 */
@Composable
private fun AnnotatedParagraph(
    text: String,
    hints: Map<String, String>,
    pendingHints: Set<String>,
    onWordClick: (String) -> Unit,
    style: TextStyle,
    baseColor: Color
) {
    val tokens = remember(text) { WordEngine.tokenize(text) }

    val annotated = remember(tokens, hints, pendingHints, baseColor) {
        buildAnnotatedString {
            var pos = 0
            for (token in tokens) {
                if (!token.isWord) {
                    append(token.text)
                    pos += token.text.length
                    continue
                }
                val key = WordEngine.normalize(token.text)
                val hint = hints[key]

                val wordStart = pos
                if (hint != null) {
                    withStyle(SpanStyle(color = AccentGreen.copy(alpha = 0.95f))) {
                        append(token.text)
                    }
                } else if (key in pendingHints) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(token.text)
                    }
                } else {
                    append(token.text)
                }
                pos += token.text.length
                addStringAnnotation(TAG_WORD, token.text, wordStart, pos)

                if (hint != null) {
                    val hintText = "·$hint"
                    val hintStart = pos
                    withStyle(
                        SpanStyle(
                            color = AccentGreen.copy(alpha = 0.75f),
                            fontSize = style.fontSize * 0.72f,
                            fontFamily = FontFamily.Monospace
                        )
                    ) {
                        append(hintText)
                    }
                    pos += hintText.length
                    // Тап по самой подсказке тоже открывает карточку слова
                    addStringAnnotation(TAG_WORD, token.text, hintStart, pos)
                }
            }
        }
    }

    ClickableText(
        text = annotated,
        style = style.copy(color = baseColor),
        onClick = { offset ->
            annotated.getStringAnnotations(TAG_WORD, offset, offset)
                .firstOrNull()
                ?.let { onWordClick(it.item) }
        }
    )
}

// ----------------------------------------------------------------------
// Карточка перевода слова
// ----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordTranslationSheet(
    word: String,
    learningStore: LearningStore,
    onClose: () -> Unit
) {
    val cleanWord = remember(word) { WordEngine.normalize(word) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val translation by produceState<Translator.WordTranslation?>(null, cleanWord) {
        value = Translator.translateWord(cleanWord, "en", "ru")
            ?: Translator.WordTranslation("", emptyList())
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "EN → RU",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(14.dp))

            val result = translation
            if (result == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Переводим…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else if (result.main.isBlank()) {
                Text(
                    text = "Не удалось получить перевод. Проверьте интернет и попробуйте ещё раз.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = result.main,
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentGreen
                )
                if (result.alternatives.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "также: " + result.alternatives.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    learningStore.markKnown(word)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color(0xFF04290F)
                )
            ) {
                Text("Я знаю это слово", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    learningStore.stopHinting(word)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Больше не показывать к нему подсказки", color = TextSecondary)
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Закрыть", color = TextSecondary)
            }
        }
    }
}
