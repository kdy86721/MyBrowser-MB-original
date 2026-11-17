package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mybrowser.appui.theme.MyBrowserTheme
import com.mybrowser.appui.ui.MyBrowserRootScreen
import com.mybrowser.appui.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

  private val browserViewModel: BrowserViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIncomingIntent(intent)

    setContent {
      val uiState by browserViewModel.uiState.collectAsStateWithLifecycle()
      val view = LocalView.current

      LaunchedEffect(uiState.settings.hideStatusBar) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (uiState.settings.hideStatusBar) {
          controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
          controller.show(WindowInsetsCompat.Type.statusBars())
        }
      }

      MyBrowserTheme(
        themeMode = uiState.settings.themeMode,
        accentPreset = uiState.settings.accentPreset,
        dynamicColor = uiState.settings.dynamicColor,
      ) {
        MyBrowserRootScreen(viewModel = browserViewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    browserViewModel.onHostResume()
  }

  override fun onPause() {
    browserViewModel.onHostPause()
    super.onPause()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_VIEW) {
      val url = intent.dataString
      if (!url.isNullOrBlank()) {
        browserViewModel.createNewTab(initialUrl = url)
      }
    }
  }
}
