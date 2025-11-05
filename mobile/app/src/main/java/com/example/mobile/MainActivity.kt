package com.example.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.ui.theme.MobileTheme
import com.example.mobile.ui.login.LoginScreen
import com.example.mobile.ui.navigation.AppTabs
import com.example.mobile.ui.auth.RegisterScreen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobile.auth.AuthRepository
import com.example.mobile.network.NetworkModule
import com.example.mobile.cities.CitiesRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileTheme {
                val context = LocalContext.current
                val authRepo = remember { AuthRepository(context) }
                val api = remember { NetworkModule.createApi(authRepo) }
                val citiesRepo = remember { CitiesRepository(context, api) }
                LaunchedEffect(Unit) { citiesRepo.ensureLoaded() }

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                // observe token changes to navigate and show snackbars
                val token by authRepo.tokenState.collectAsState()
                var prevToken by remember { mutableStateOf<String?>(null) }
                var firstRun by remember { mutableStateOf(true) }

                LaunchedEffect(token) {
                    if (firstRun) {
                        // don't react to the initial emission from DataStore
                        firstRun = false
                        prevToken = token
                        return@LaunchedEffect
                    }

                    if (token != null && prevToken.isNullOrBlank()) {
                        // logged in
                        snackbarHostState.showSnackbar("Connecté")
                        navController.navigate("tabs") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else if (token.isNullOrBlank() && !prevToken.isNullOrBlank()) {
                        // logged out
                        snackbarHostState.showSnackbar("Déconnecté")
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                    prevToken = token
                }

                // Use the collected `token` state instead of calling StateFlow.value directly
                val startDestination = if (token.isNullOrBlank()) "login" else "tabs"

                Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                api = api,
                                authRepo = authRepo,
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                api = api,
                                authRepo = authRepo,
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("tabs") {
                            AppTabs(authRepo = authRepo, snackbarHostState = snackbarHostState, citiesRepo = citiesRepo)
                        }
                    }
                }
            }
        }
    }
}