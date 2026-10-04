package com.techpulse.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.techpulse.app.BuildConfig
import com.techpulse.app.data.FeedItem
import com.techpulse.app.ui.TimeAgo
import com.techpulse.app.ui.formatHost
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.SurfaceElevated
import com.techpulse.app.ui.theme.TextPrimary
import com.techpulse.app.ui.theme.TextSecondary

/**
 * Лента новостей: карточки с краткой аннотацией, переходом к источнику
 * и кнопкой добавления в избранное.
 */
@Composable
fun NewsList(
    feedItems: List<FeedItem>,
    error: String? = null,
    onOpen: (FeedItem) -> Unit,
    onToggleBookmark: (FeedItem) -> Unit,
    isBookmarked: (FeedItem) -> Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (error != null) {
            item(key = "error_banner") { ErrorBanner(error) }
        }

        items(feedItems, key = { it.id }) { item ->
            NewsCard(
                item = item,
                isBookmarked = isBookmarked(item),
                onOpen = { onOpen(item) },
                onToggleBookmark = { onToggleBookmark(item) }
            )
        }

        item(key = "footer") { ListFooter() }
    }
}

@Composable
private fun ListFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "— конец ленты —",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "TechPulse v${BuildConfig.VERSION_NAME} · сборка №${BuildConfig.BUILD_NUMBER}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TextSecondary.copy(alpha = 0.7f)
        )
    }
}

/** Карточка новости: источник, время, заголовок, аннотация, обложка, действия. */
@Composable
fun NewsCard(
    item: FeedItem,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Источник + время + закладка
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(item.sourceColorHex))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.sourceName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(item.sourceColorHex),
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = TimeAgo.format(item.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (isBookmarked) AccentCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Заголовок (перевод на русский, если он уже готов; под ним — оригинал)
            val translatedTitle = item.translatedTitle
            Text(
                text = translatedTitle ?: item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (translatedTitle != null && !translatedTitle.equals(item.title, ignoreCase = true)) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Обложка, если есть в фиде
            val imageUrl = item.imageUrl
            if (imageUrl != null) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(SurfaceElevated),
                    error = ColorPainter(SurfaceElevated)
                )
            }

            // Краткая аннотация (переведённая, если готова)
            val displayedSummary = item.translatedSummary ?: item.summary
            if (displayedSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = displayedSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            // Призыв к переходу в источник
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ЧИТАТЬ В ПРИЛОЖЕНИИ",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = AccentCyan
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatHost(item.link),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
