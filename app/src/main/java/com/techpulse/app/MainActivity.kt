package com.techpulse.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.techpulse.app.ui.AppRoot
import com.techpulse.app.ui.theme.TechPulseTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Extra со ссылкой на статью (deep link из виджетов рабочего стола). */
        const val EXTRA_OPEN_URL = "com.techpulse.app.extra.OPEN_URL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Иммерсивный тёмный режим: прозрачные системные панели, светлые иконки
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        handleDeepLink(intent)

        setContent {
            TechPulseTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val link = intent?.getStringExtra(EXTRA_OPEN_URL)
        if (!link.isNullOrBlank()) {
            DeepLinkBus.push(link)
        }
    }
}
