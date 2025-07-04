package com.mssdepas.meteoesp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mssdepas.meteoesp.ui.AuthState
import com.mssdepas.meteoesp.ui.AuthViewModel
import com.mssdepas.meteoesp.ui.MainViewModel
import com.mssdepas.meteoesp.ui.main.AccountScreen
import com.mssdepas.meteoesp.ui.main.MainScreen
import com.mssdepas.meteoesp.ui.main.MapScreen
import com.mssdepas.meteoesp.ui.theme.MeteoEspTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Map : Screen("map", "Mapa", Icons.Default.Map)
    object Account : Screen("account", "Cuenta", Icons.Default.Person)
}

val items = listOf(
    Screen.Home,
    Screen.Map,
    Screen.Account
)

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
            val navController = rememberNavController()
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        MainScreen(vm = mainViewModel, authViewModel = authViewModel)
                    }
                    composable(Screen.Map.route) {
                        MapScreen(vm = mainViewModel)
                    }
                    composable(Screen.Account.route) {
                        AccountScreen(authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}