package com.example.spent

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.spent.ui.navigation.SpentAppNavHost
import com.example.spent.ui.theme.SpentTheme
import com.example.spent.util.LocaleHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()

        val app = application as SpentApplication
        val repository = app.repository

        setContent {
            val isDarkThemeOverride by repository.isDarkThemeFlow.collectAsState(initial = null)
            val appLanguage by repository.appLanguageFlow.collectAsState(initial = null)
            val useDarkTheme = isDarkThemeOverride ?: isSystemInDarkTheme()

            val baseContext = LocalContext.current
            val localizedContext = remember(appLanguage, baseContext) {
                LocaleHelper.createLocalizedContext(baseContext, appLanguage)
            }
            val localizedConfiguration = remember(appLanguage, baseContext) {
                LocaleHelper.createLocalizedConfiguration(baseContext, appLanguage)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration
            ) {
                SpentTheme(darkTheme = useDarkTheme) {
                    SpentAppNavHost(repository = repository)
                }
            }
        }
    }
}