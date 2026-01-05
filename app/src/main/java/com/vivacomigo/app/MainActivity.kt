package com.vivacomigo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vivacomigo.app.ui.screen.HomeScreen
import com.vivacomigo.app.ui.screen.LoginScreen
import com.vivacomigo.app.ui.screen.PairingScreen
import com.vivacomigo.app.ui.theme.VivaTheme
import com.vivacomigo.app.ui.viewmodel.AuthState
import com.vivacomigo.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VivaTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (uiState.authState) {
                    is AuthState.Loading -> {
                        // Show loading screen
                    }

                    is AuthState.NotAuthenticated -> {
                        LoginScreen(
                            onLogin = { email, password ->
                                viewModel.login(email, password)
                            },
                            onRegister = { email, password ->
                                viewModel.register(email, password)
                            },
                            isLoading = uiState.isLoading,
                            error = uiState.error
                        )
                    }

                    is AuthState.Authenticated -> {
                        val user = (uiState.authState as AuthState.Authenticated).user

                        if (user.partnerId == null) {
                            // Show pairing screen
                            PairingScreen(
                                user = user,
                                onPair = { code ->
                                    viewModel.pairWithPartner(code)
                                },
                                isLoading = uiState.isLoading,
                                error = uiState.error
                            )
                        } else {
                            // Show home screen
                            HomeScreen(
                                currentUser = user,
                                partner = uiState.partner,
                                latestPhoto = uiState.latestPhoto,
                                onSendPhoto = { uri ->
                                    viewModel.sendPhoto(uri)
                                },
                                onLogout = {
                                    viewModel.logout()
                                },
                                isLoading = uiState.isLoading
                            )
                        }
                    }
                }
            }
        }
    }
}
