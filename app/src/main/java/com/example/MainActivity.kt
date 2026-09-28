package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.example.videobrowser.detection.WebViewCacheManager
import com.example.videobrowser.ui.BrowserScreen
import com.example.videobrowser.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WebViewCacheManager.configureGraphicsEnvironment()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by browserViewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(
                themeMode = uiState.themeMode,
                colorTheme = uiState.colorTheme,
                fontFamilyChoice = uiState.appFontFamily
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(viewModel = browserViewModel)
                }
            }
        }
    }
}
