package com.techpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.techpulse.app.ui.AppRoot
import com.techpulse.app.ui.theme.TechPulseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var updateManager: UpdateManager
    private var availableRelease by mutableStateOf<UpdateManager.Release?>(null)
    private var pendingRelease: UpdateManager.Release? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateManager = UpdateManager(applicationContext)
        updateManager.register()

        // Иммерсивный тёмный режим: прозрачные системные панели, светлые иконки
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            TechPulseTheme {
                AppRoot()
                availableRelease?.let { release ->
                    AlertDialog(
                        onDismissRequest = { availableRelease = null },
                        title = { Text("Доступно обновление") },
                        text = {
                            Text(
                                "TechPulse ${release.versionName} готов к установке. " +
                                    "APK будет загружен, после чего Android попросит подтвердить установку."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                availableRelease = null
                                startUpdate(release)
                            }) { Text("Обновить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { availableRelease = null }) { Text("Позже") }
                        }
                    )
                }
            }
        }

        lifecycleScope.launch {
            availableRelease = runCatching { updateManager.check() }.getOrNull()
        }
    }

    private fun startUpdate(release: UpdateManager.Release) {
        if (updateManager.canInstallPackages()) {
            pendingRelease = null
            updateManager.download(release)
        } else {
            pendingRelease = release
            updateManager.openInstallPermissionSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        val release = pendingRelease
        if (release != null && ::updateManager.isInitialized && updateManager.canInstallPackages()) {
            pendingRelease = null
            updateManager.download(release)
        }
    }

    override fun onDestroy() {
        if (::updateManager.isInitialized) updateManager.unregister()
        super.onDestroy()
    }
}
