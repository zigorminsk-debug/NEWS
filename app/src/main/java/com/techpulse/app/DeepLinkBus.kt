package com.techpulse.app

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Шина deep-link'ов: виджеты и уведомления кладут сюда ссылку на статью,
 * AppRoot подхватывает её и открывает встроенный ридер.
 */
object DeepLinkBus {

    val url = MutableStateFlow<String?>(null)

    fun push(link: String) {
        url.value = link
    }

    fun consume(): String? {
        val current = url.value
        url.value = null
        return current
    }
}
