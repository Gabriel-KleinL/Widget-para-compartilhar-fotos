package com.vivacomigo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.vivacomigo.app.ui.screen.HomeScreen
import com.vivacomigo.app.ui.screen.PairingScreen
import com.vivacomigo.app.ui.theme.VivaTheme
import com.vivacomigo.app.ui.viewmodel.AuthState
import com.vivacomigo.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(application) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VivaTheme {
                val uiState by viewModel.uiState.collectAsState()

                when (uiState.authState) {
                    is AuthState.Loading -> {
                        // Show loading screen
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is AuthState.Error -> {
                        // Show error screen with retry button
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Erro de Conexão",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (uiState.authState as AuthState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.retryConnection() },
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Tentar Novamente")
                                }
                            }
                        }
                    }

                    is AuthState.Ready -> {
                        val user = (uiState.authState as AuthState.Ready).user

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
                                latestPhotoUri = uiState.latestPhotoUri,
                                onSendPhoto = { uri ->
                                    viewModel.sendPhoto(uri)
                                },
                                onUnpair = {
                                    viewModel.unpairPartner()
                                },
                                isLoading = uiState.isLoading,
                                successMessage = uiState.successMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
