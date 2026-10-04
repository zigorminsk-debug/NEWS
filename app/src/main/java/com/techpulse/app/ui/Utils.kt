package com.techpulse.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Открывает ссылку в Chrome Custom Tabs (встроенный браузер), с фолбэком на системный. */
fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .build()
            .launchUrl(context, uri)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(context, "Не найдено приложение для открытия ссылки", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Человекочитаемый домен ссылки: "https://habr.com/ru/post/1" -> "habr.com". */
fun formatHost(url: String): String = try {
    Uri.parse(url).host ?: url
} catch (_: Exception) {
    url
}

/** Сколько времени прошло с момента публикации. */
object TimeAgo {
    fun format(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            minutes < 24 * 60 -> "${minutes / 60} ч назад"
            minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} дн назад"
            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
