package com.techpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techpulse.app.FeedViewModel
import com.techpulse.app.data.FeedItem
import com.techpulse.app.ui.components.EmptyState
import com.techpulse.app.ui.components.NewsList
import com.techpulse.app.ui.components.ScreenHeader

/** Избранное: статьи, сохранённые из ленты. Открываются во встроенном ридере. */
@Composable
fun BookmarksScreen(
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier,
    onOpenArticle: (FeedItem) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val sortedBookmarks = state.bookmarks.sortedByDescending { it.publishedAt }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            subtitle = if (sortedBookmarks.isEmpty()) "сохранённые статьи"
            else "сохранённые статьи · ${sortedBookmarks.size}",
            badgeText = "ЗАКЛАДКИ"
        )

        if (sortedBookmarks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.BookmarkBorder,
                title = "В избранном пусто",
                subtitle = "Нажмите на иконку закладки в карточке новости, чтобы сохранить её для чтения позже."
            )
        } else {
            NewsList(
                feedItems = sortedBookmarks,
                error = null,
                onOpen = onOpenArticle,
                onToggleBookmark = viewModel::toggleBookmark,
                isBookmarked = viewModel::isBookmarked
            )
        }
    }
}
