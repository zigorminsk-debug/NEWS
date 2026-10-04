package com.techpulse.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techpulse.app.FeedViewModel
import com.techpulse.app.ui.screens.BookmarksScreen
import com.techpulse.app.ui.screens.FeedScreen
import com.techpulse.app.ui.screens.ResourcesScreen
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.TextSecondary

private data class TabItem(
    val title: String,
    val icon: ImageVector
)

/** Корень приложения: нижняя навигация из трёх разделов. */
@Composable
fun AppRoot() {
    val viewModel: FeedViewModel = viewModel()

    val tabs = listOf(
        TabItem("Лента", Icons.Filled.Article),
        TabItem("Ресурсы", Icons.Filled.Explore),
        TabItem("Избранное", Icons.Filled.Bookmarks)
    )

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
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
                modifier = Modifier.padding(innerPadding)
            )
            1 -> ResourcesScreen(
                modifier = Modifier.padding(innerPadding)
            )
            else -> BookmarksScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
