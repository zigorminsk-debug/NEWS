package com.techpulse.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techpulse.app.data.ItResource
import com.techpulse.app.data.Sources
import com.techpulse.app.ui.components.EmptyState
import com.techpulse.app.ui.components.GradientDivider
import com.techpulse.app.ui.components.ScreenHeader
import com.techpulse.app.ui.components.SearchField
import com.techpulse.app.ui.formatHost
import com.techpulse.app.ui.theme.AccentCyan
import com.techpulse.app.ui.theme.SurfaceElevated
import com.techpulse.app.ui.theme.TextPrimary
import com.techpulse.app.ui.theme.TextSecondary

/**
 * Каталог полезных IT-ресурсов: сгруппирован по категориям,
 * с поиском и открытием сайтов во встроенном браузере с авто-переводом на русский.
 */
@Composable
fun ResourcesScreen(
    modifier: Modifier = Modifier,
    onOpenSite: (ItResource) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var activeCategory by rememberSaveable { mutableStateOf("") }

    val filtered = remember(query, activeCategory) {
        Sources.RESOURCES.filter { resource ->
            val matchesCategory = activeCategory.isEmpty() || resource.category == activeCategory
            val matchesQuery = query.isBlank() ||
                resource.name.contains(query, ignoreCase = true) ||
                resource.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            subtitle = "полезные сайты и сервисы для IT · ${filtered.size}",
            badgeText = "IT-РЕСУРСЫ"
        )

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Поиск по ресурсам…"
        )

        // Чипы категорий
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "all_categories") {
                FilterChip(
                    selected = activeCategory.isEmpty(),
                    onClick = { activeCategory = "" },
                    label = { Text("Все") }
                )
            }
            items(Sources.RESOURCE_CATEGORIES, key = { it }) { category ->
                FilterChip(
                    selected = activeCategory == category,
                    onClick = {
                        activeCategory = if (activeCategory == category) "" else category
                    },
                    label = { Text(category) }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = "Ничего не найдено",
                subtitle = "Измените запрос или выберите другую категорию."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Sources.RESOURCE_CATEGORIES.forEach { category ->
                    val categoryItems = filtered.filter { it.category == category }
                    if (categoryItems.isNotEmpty()) {
                        item(key = "category_header_$category") {
                            CategoryHeader(category = category, count = categoryItems.size)
                        }
                        items(categoryItems, key = { it.id }) { resource ->
                            ResourceCard(resource = resource) {
                                onOpenSite(resource)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Заголовок категории с иконкой, названием и количеством. */
@Composable
private fun CategoryHeader(category: String, count: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = category.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "· $count",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        GradientDivider()
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Новости и медиа" -> Icons.Filled.Language
    "Обучение" -> Icons.Filled.School
    "Документация" -> Icons.Filled.MenuBook
    "Инструменты" -> Icons.Filled.Build
    "Искусственный интеллект" -> Icons.Filled.Memory
    "Сообщества" -> Icons.Filled.People
    "Карьера" -> Icons.Filled.Work
    "Практика" -> Icons.Filled.Code
    else -> Icons.Filled.Language
}

/** Карточка ресурса: имя, описание, домен — клик открывает сайт. */
@Composable
private fun ResourceCard(resource: ItResource, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    imageVector = categoryIcon(resource.category),
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (resource.lang == "ru") "RU" else "EN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Link,
                        contentDescription = null,
                        tint = AccentCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = formatHost(resource.url),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF63B3FF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
