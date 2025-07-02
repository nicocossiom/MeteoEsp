package com.mssdepas.meteoesp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mssdepas.meteoesp.ui.MainViewModel
import com.mssdepas.meteoesp.ui.main.MainScreen
import com.mssdepas.meteoesp.util.GlobalExceptionHandler

class MainActivity : ComponentActivity() {
    // Use the by viewModels() delegate
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Pass the viewModel instance to your Composable
            MainScreen(viewModel)
        }
    }
}