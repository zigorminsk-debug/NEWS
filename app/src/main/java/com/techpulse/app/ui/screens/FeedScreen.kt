package com.techpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techpulse.app.BuildConfig
import com.techpulse.app.FeedViewModel
import com.techpulse.app.data.Sources
import com.techpulse.app.ui.TimeAgo
import com.techpulse.app.ui.components.EmptyState
import com.techpulse.app.ui.components.LoadingIndicator
import com.techpulse.app.ui.components.NewsList
import com.techpulse.app.ui.components.ScreenHeader
import com.techpulse.app.ui.components.SearchField
import com.techpulse.app.ui.openUrl
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.TextSecondary

/**
 * Главный экран — лента IT-новостей:
 * поиск, фильтр по источникам, карточки с аннотациями и переходом к источнику.
 */
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filteredItems = remember(state.items, state.query, state.activeSources) {
        state.items.filter { item ->
            val matchesSource = state.activeSources.isEmpty() || item.sourceId in state.activeSources
            val matchesQuery = state.query.isBlank() ||
                item.title.contains(state.query, ignoreCase = true) ||
                item.summary.contains(state.query, ignoreCase = true)
            matchesSource && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            subtitle = when {
                state.lastUpdated > 0 -> "обновлено: ${TimeAgo.format(state.lastUpdated)} · ${filteredItems.size} новостей"
                else -> "IT · хай-тек · новости и ресурсы"
            },
            badgeText = "BUILD ${BuildConfig.BUILD_NUMBER}",
            trailing = {
                IconButton(
                    onClick = viewModel::refresh,
                    enabled = !state.isRefreshing && !state.initialLoading
                ) {
                    if (state.isRefreshing || state.initialLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Обновить ленту",
                            tint = TextSecondary
                        )
                    }
                }
            }
        )

        SearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "Поиск по новостям…"
        )

        SourceFilterRow(
            active = state.activeSources,
            onToggle = viewModel::toggleSourceFilter,
            onClear = viewModel::clearSourceFilter
        )

        Spacer(Modifier.height(4.dp))

        when {
            state.initialLoading -> LoadingIndicator("Загрузка ленты новостей…")

            filteredItems.isEmpty() && state.items.isEmpty() -> EmptyState(
                icon = Icons.Filled.WifiOff,
                title = "Лента пуста",
                subtitle = state.error
                    ?: "Нажмите на иконку обновления, чтобы загрузить IT-новости.",
                actionText = "Обновить",
                onAction = viewModel::refresh
            )

            filteredItems.isEmpty() -> EmptyState(
                icon = Icons.Filled.Search,
                title = "Ничего не найдено",
                subtitle = "Попробуйте изменить поисковый запрос или фильтр источников."
            )

            else -> NewsList(
                feedItems = filteredItems,
                error = state.error,
                onOpen = { openUrl(context, it.link) },
                onToggleBookmark = viewModel::toggleBookmark,
                isBookmarked = viewModel::isBookmarked
            )
        }
    }
}

/** Горизонтальная лента чипов фильтрации по источникам. */
@Composable
private fun SourceFilterRow(
    active: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all_sources") {
            FilterChip(
                selected = active.isEmpty(),
                onClick = onClear,
                label = { Text("Все источники") }
            )
        }
        items(Sources.NEWS, key = { it.id }) { source ->
            FilterChip(
                selected = source.id in active,
                onClick = { onToggle(source.id) },
                label = { Text(source.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(source.colorHex))
                    )
                }
            )
        }
    }
}
