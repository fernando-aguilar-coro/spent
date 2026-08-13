package com.example.spent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.spent.ui.navigation.SpentAppNavHost
import com.example.spent.ui.theme.SpentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SpentApplication
        val repository = app.repository

        setContent {
            val isDarkTheme by repository.isDarkThemeFlow.collectAsState(initial = false)

            SpentTheme(darkTheme = isDarkTheme) {
                SpentAppNavHost(repository = repository)
            }
        }
    }
}