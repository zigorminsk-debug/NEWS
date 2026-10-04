package com.techpulse.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.techpulse.app.data.BookmarksStore
import com.techpulse.app.data.FeedItem
import com.techpulse.app.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Общая ViewModel приложения: состояние ленты, фильтров, поиска и избранного.
 */
class FeedViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val initialLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val items: List<FeedItem> = emptyList(),
        val bookmarks: List<FeedItem> = emptyList(),
        val query: String = "",
        val activeSources: Set<String> = emptySet(),
        val error: String? = null,
        val lastUpdated: Long = 0L
    )

    private val repository = NewsRepository(application)
    private val bookmarkStore = BookmarksStore(application)

    private val _uiState = MutableStateFlow(UiState(bookmarks = bookmarkStore.current))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Сначала показываем кэш (если есть), затем обновляем по сети
            val cached = repository.loadCache()
            _uiState.update { it.copy(items = cached, initialLoading = cached.isEmpty()) }
            runRefresh(initial = cached.isEmpty())
        }
    }

    fun refresh() {
        if (_uiState.value.initialLoading || _uiState.value.isRefreshing) return
        viewModelScope.launch { runRefresh(initial = false) }
    }

    private suspend fun runRefresh(initial: Boolean) {
        _uiState.update { it.copy(initialLoading = initial, isRefreshing = !initial, error = null) }
        val fresh = repository.fetchAll()
        _uiState.update { state ->
            if (fresh.isEmpty()) {
                state.copy(
                    initialLoading = false,
                    isRefreshing = false,
                    error = if (state.items.isEmpty()) {
                        "Не удалось загрузить новости. Проверьте подключение к интернету и повторите попытку."
                    } else {
                        "Не удалось обновить ленту — показаны ранее сохранённые новости."
                    }
                )
            } else {
                state.copy(
                    initialLoading = false,
                    isRefreshing = false,
                    items = fresh,
                    error = null,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun toggleSourceFilter(sourceId: String) {
        _uiState.update { state ->
            val active = if (sourceId in state.activeSources) {
                state.activeSources - sourceId
            } else {
                state.activeSources + sourceId
            }
            state.copy(activeSources = active)
        }
    }

    fun clearSourceFilter() {
        _uiState.update { it.copy(activeSources = emptySet()) }
    }

    fun toggleBookmark(item: FeedItem) {
        bookmarkStore.toggle(item)
        _uiState.update { it.copy(bookmarks = bookmarkStore.current) }
    }

    fun isBookmarked(item: FeedItem): Boolean = bookmarkStore.isBookmarked(item)
}
