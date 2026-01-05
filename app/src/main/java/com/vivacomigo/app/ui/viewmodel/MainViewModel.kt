package com.vivacomigo.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vivacomigo.app.data.model.Photo
import com.vivacomigo.app.data.model.User
import com.vivacomigo.app.data.repository.AuthRepository
import com.vivacomigo.app.data.repository.PhotoRepository
import com.vivacomigo.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
}

data class MainUiState(
    val authState: AuthState = AuthState.Loading,
    val currentUser: User? = null,
    val partner: User? = null,
    val latestPhoto: Photo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val photoRepository = PhotoRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val firebaseUser = authRepository.currentUser
            if (firebaseUser != null) {
                loadUserData(firebaseUser.uid)
            } else {
                _uiState.value = _uiState.value.copy(
                    authState = AuthState.NotAuthenticated,
                    isLoading = false
                )
            }
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            // Load user
            userRepository.getUserFlow(userId).collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Authenticated(user),
                        currentUser = user
                    )

                    // Load partner if exists
                    user.partnerId?.let { partnerId ->
                        loadPartner(partnerId)
                    }

                    // Load latest photo
                    loadLatestPhoto(userId)
                }
            }
        }
    }

    private fun loadPartner(partnerId: String) {
        viewModelScope.launch {
            userRepository.getUserFlow(partnerId).collect { partner ->
                _uiState.value = _uiState.value.copy(partner = partner)
            }
        }
    }

    private fun loadLatestPhoto(userId: String) {
        viewModelScope.launch {
            photoRepository.getLatestPhotoForUser(userId).collect { photo ->
                _uiState.value = _uiState.value.copy(latestPhoto = photo)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.login(email, password).fold(
                onSuccess = { user ->
                    loadUserData(user.uid)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.register(email, password).fold(
                onSuccess = { firebaseUser ->
                    // Create user document
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        pairingCode = authRepository.generatePairingCode(),
                        displayName = firebaseUser.email?.substringBefore('@') ?: ""
                    )
                    userRepository.createUser(user)
                    loadUserData(firebaseUser.uid)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = MainUiState(authState = AuthState.NotAuthenticated)
    }

    fun pairWithPartner(partnerCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = _uiState.value.currentUser ?: return@launch

            userRepository.findUserByPairingCode(partnerCode).fold(
                onSuccess = { partner ->
                    if (partner.id == currentUser.id) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Você não pode parear consigo mesmo!"
                        )
                        return@fold
                    }

                    userRepository.pairUsers(currentUser.id, partner.id).fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = null
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Código inválido ou usuário não encontrado"
                    )
                }
            )
        }
    }

    fun sendPhoto(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = _uiState.value.currentUser ?: return@launch
            val partnerId = currentUser.partnerId ?: return@launch

            photoRepository.uploadPhoto(imageUri, currentUser.id, partnerId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}
