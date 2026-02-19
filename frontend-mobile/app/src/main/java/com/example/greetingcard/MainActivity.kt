package com.example.greetingcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.greetingcard.data.auth.AuthRepository
import com.example.greetingcard.ui.camera.CameraScreen
import com.example.greetingcard.ui.history.HistoryScreen
import com.example.greetingcard.ui.login.LoginScreen
import com.example.greetingcard.ui.theme.GreetingCardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

const val ROUTE_LOGIN = "login"
const val ROUTE_CAMERA = "camera"
const val ROUTE_HISTORY = "history"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = runBlocking {
            if (authRepository.getToken() != null) ROUTE_CAMERA else ROUTE_LOGIN
        }

        setContent {
            GreetingCardTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(ROUTE_LOGIN) {
                        LoginScreen(onLoginSuccess = {
                            navController.navigate(ROUTE_CAMERA) {
                                popUpTo(ROUTE_LOGIN) { inclusive = true }
                            }
                        })
                    }
                    composable(ROUTE_CAMERA) {
                        CameraScreen(
                            onLogout = {
                                scope.launch {
                                    authRepository.clearToken()
                                    navController.navigate(ROUTE_LOGIN) {
                                        popUpTo(ROUTE_CAMERA) { inclusive = true }
                                    }
                                }
                            },
                            onNavigateToHistory = {
                                navController.navigate(ROUTE_HISTORY)
                            }
                        )
                    }
                    composable(ROUTE_HISTORY) {
                        HistoryScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
