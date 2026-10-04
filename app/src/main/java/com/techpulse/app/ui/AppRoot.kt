package com.techpulse.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techpulse.app.FeedViewModel
import com.techpulse.app.data.FeedItem
import com.techpulse.app.data.ItResource
import com.techpulse.app.ui.reader.ReaderScreen
import com.techpulse.app.ui.screens.BookmarksScreen
import com.techpulse.app.ui.screens.FeedScreen
import com.techpulse.app.ui.screens.ResourcesScreen
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.Background
import com.techpulse.app.ui.theme.TextSecondary
import com.techpulse.app.ui.update.UpdateHost
import com.techpulse.app.ui.web.WebViewScreen
import com.techpulse.app.update.AppUpdateManager
import com.techpulse.app.update.UpdateState

private data class TabItem(
    val title: String,
    val icon: ImageVector
)

private data class WebTarget(
    val url: String,
    val title: String,
    val offerTranslation: Boolean
)

/**
 * Корень приложения: нижняя навигация из трёх разделов.
 *
 * Поверх основного контента открываются полноэкранные окна:
 *  - [ReaderScreen] — чтение статьи внутри приложения (авто-перевод / обучение);
 *  - [WebViewScreen] — просмотр сайтов из каталога (с авто-переводом на русский);
 *  - [UpdateHost] — диалоги автообновления приложения.
 */
@Composable
fun AppRoot() {
    val viewModel: FeedViewModel = viewModel()
    val context = LocalContext.current

    val updateManager = remember { AppUpdateManager(context.applicationContext) }
    val updateState by updateManager.state.collectAsStateWithLifecycle()

    // Автопроверка обновлений при запуске приложения
    LaunchedEffect(Unit) {
        updateManager.checkIfNeeded(force = false)
    }

    val tabs = listOf(
        TabItem("Лента", Icons.Filled.Article),
        TabItem("Ресурсы", Icons.Filled.Explore),
        TabItem("Избранное", Icons.Filled.Bookmarks)
    )

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var readerItem by remember { mutableStateOf<FeedItem?>(null) }
    var webTarget by remember { mutableStateOf<WebTarget?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index && readerItem == null && webTarget == null,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentCyan,
                                selectedTextColor = AccentCyan,
                                indicatorColor = AccentCyan.copy(alpha = 0.18f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                0 -> FeedScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding),
                    onOpenArticle = { readerItem = it },
                    updateAvailable = updateState is UpdateState.Available ||
                        updateState is UpdateState.Downloading ||
                        updateState is UpdateState.Ready,
                    onCheckUpdates = { updateManager.checkIfNeeded(force = true) }
                )

                1 -> ResourcesScreen(
                    modifier = Modifier.padding(innerPadding),
                    onOpenSite = { resource: ItResource ->
                        webTarget = WebTarget(
                            url = resource.url,
                            title = resource.name,
                            offerTranslation = resource.lang != "ru"
                        )
                    }
                )

                else -> BookmarksScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding),
                    onOpenArticle = { readerItem = it }
                )
            }
        }

        // Полноэкранный ридер статьи — поверх всех экранов
        readerItem?.let { item ->
            ReaderScreen(
                item = item,
                viewModel = viewModel,
                onBack = { readerItem = null }
            )
        }

        // Полноэкранный просмотр сайта — поверх всех экранов
        webTarget?.let { target ->
            WebViewScreen(
                title = target.title,
                url = target.url,
                offerTranslation = target.offerTranslation,
                onBack = { webTarget = null }
            )
        }

        // Диалоги автообновления — самый верхний слой
        UpdateHost(manager = updateManager, state = updateState)
    }
}
