package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BlackoutViewModel
import com.example.viewmodel.BlackoutViewModelFactory

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val viewModel: BlackoutViewModel by viewModels {
        val app = application as BlackoutApplication
        BlackoutViewModelFactory(app, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val customColorInt by viewModel.customThemeColor.collectAsState()

            MyApplicationTheme(
                themeMode = themeMode,
                customColor = Color(customColorInt)
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
