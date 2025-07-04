package com.mssdepas.meteoesp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
    import com.mssdepas.meteoesp.ui.AuthState
import com.mssdepas.meteoesp.ui.AuthViewModel
import com.mssdepas.meteoesp.ui.MainViewModel
import com.mssdepas.meteoesp.ui.main.MainScreen
import com.mssdepas.meteoesp.ui.theme.MeteoEspTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeteoEspTheme {
                MeteoEspApp(
                    authViewModel = authViewModel,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}

@Composable
fun MeteoEspApp(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    var showSignUp by remember { mutableStateOf(false) }

    when (authState) {
        is AuthState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is AuthState.Unauthenticated -> {
            if (showSignUp) {
                SignUpScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { showSignUp = false }
                )
            } else {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignUp = { showSignUp = true }
                )
            }
        }

        is AuthState.Authenticated -> {
            MainScreen(vm = mainViewModel, authViewModel = authViewModel)
        }
    }
}