package com.vivacomigo.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.vivacomigo.app.data.model.Photo
import com.vivacomigo.app.data.model.User
import com.vivacomigo.app.data.repository.AuthRepository
import com.vivacomigo.app.data.repository.PhotoRepository
import com.vivacomigo.app.data.repository.UserRepository
import com.vivacomigo.app.widget.PhotoWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Estados de autenticação do usuário
 */
sealed class AuthState {
    object Loading : AuthState()
    data class Ready(val user: User) : AuthState() // Usuário sempre pronto, não precisa de autenticação
    data class Error(val message: String) : AuthState() // Erro de conexão
}

/**
 * Estado centralizado da UI
 *
 * @property authState Estado de autenticação
 * @property currentUser Usuário atual (criado localmente)
 * @property partner Parceiro pareado (null se não pareado)
 * @property latestPhoto Metadados da última foto recebida
 * @property latestPhotoUri URI da imagem em cache local
 * @property isLoading Indicador de carregamento
 * @property error Mensagem de erro (null se sem erro)
 * @property successMessage Mensagem de sucesso temporária
 */
data class MainUiState(
    val authState: AuthState = AuthState.Loading,
    val currentUser: User? = null,
    val partner: User? = null,
    val latestPhoto: Photo? = null,
    val latestPhotoUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel principal da aplicação - gerencia todo o estado e lógica de negócio.
 *
 * Responsabilidades:
 * - Gerenciar estado centralizado via StateFlow
 * - Coordenar operações entre repositories
 * - Polling de fotos a cada 30 segundos
 * - Atualização de widget
 *
 * Fluxos principais:
 * 1. Inicialização: loadUserData() → cria usuário local se necessário
 * 2. Pareamento: pairWithPartner(code) → transação MySQL para parear
 * 3. Envio: sendPhoto(uri) → upload BLOB para MySQL
 * 4. Sincronização: startPolling() → busca fotos a cada 30s
 *
 * 📖 Documentação completa: .claude/COMPONENTS.md → MainViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val authRepository = AuthRepository(context)
    private val userRepository = UserRepository(context)
    private val photoRepository = PhotoRepository(context)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private val POLLING_INTERVAL = 30_000L // 30 segundos

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            userRepository.getCurrentUser().fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Ready(user),
                        currentUser = user,
                        isLoading = false
                    )

                    // Load partner if exists
                    user.partnerId?.let { partnerId ->
                        loadPartner(partnerId)
                    }

                    // Load latest photo
                    loadLatestPhoto()

                    // Iniciar polling
                    startPolling()
                },
                onFailure = { error ->
                    android.util.Log.e("MainViewModel", "Erro ao carregar usuário: ${error.message}", error)
                    val errorMessage = when {
                        error.message?.contains("Communications link failure") == true -> 
                            "Não foi possível conectar ao servidor. Verifique sua conexão com a internet."
                        error.message?.contains("timeout") == true -> 
                            "Tempo de conexão esgotado. Verifique sua conexão com a internet."
                        else -> 
                            "Erro ao conectar: ${error.message ?: "Erro desconhecido"}"
                    }
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Error(errorMessage),
                        isLoading = false,
                        error = errorMessage
                    )
                }
            )
        }
    }
    
    fun retryConnection() {
        loadUserData()
    }

    private fun loadPartner(partnerId: String) {
        viewModelScope.launch {
            userRepository.getUser(partnerId).fold(
                onSuccess = { partner ->
                    _uiState.value = _uiState.value.copy(partner = partner)
                },
                onFailure = {
                    // Ignorar erro silenciosamente
                }
            )
        }
    }

    private fun loadLatestPhoto() {
        viewModelScope.launch {
            try {
                val currentUser = _uiState.value.currentUser ?: return@launch
                val photo = photoRepository.getLatestPhotoForUser(currentUser.id)
                
                // Buscar imagem BLOB e converter para URI
                val photoUri = photo?.let { p ->
                    try {
                        val imageBytes = photoRepository.getPhotoImage(p.id, currentUser.id)
                        imageBytes?.let { bytes ->
                            com.vivacomigo.app.data.repository.ImageHelper.saveImageToCache(
                                context,
                                bytes,
                                p.id
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "Erro ao carregar imagem: ${e.message}", e)
                        null
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    latestPhoto = photo,
                    latestPhotoUri = photoUri
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Erro ao carregar última foto: ${e.message}", e)
                // Não atualizar o estado em caso de erro para não quebrar a UI
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLLING_INTERVAL)
                if (_uiState.value.authState is AuthState.Ready) {
                    loadLatestPhoto()
                }
            }
        }
    }

    fun pairWithPartner(partnerCode: String) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "pairWithPartner chamado com código: $partnerCode")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = _uiState.value.currentUser ?: run {
                android.util.Log.e("MainViewModel", "currentUser é null!")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro: usuário não encontrado"
                )
                return@launch
            }

            android.util.Log.d("MainViewModel", "Usuário atual: ${currentUser.id}, código: ${currentUser.pairingCode}")

            // Limpar espaços e converter para maiúsculas
            val cleanCode = partnerCode.trim().uppercase()
            
            android.util.Log.d("MainViewModel", "Código limpo: $cleanCode")
            
            if (cleanCode.isEmpty()) {
                android.util.Log.w("MainViewModel", "Código vazio")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Código de pareamento não pode estar vazio"
                )
                return@launch
            }

            android.util.Log.d("MainViewModel", "Chamando userRepository.pairUsers...")
            userRepository.pairUsers(currentUser.id, cleanCode).fold(
                onSuccess = { updatedUser ->
                    android.util.Log.d("MainViewModel", "Pareamento bem-sucedido! Novo partner_id: ${updatedUser.partnerId}")
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Ready(updatedUser), // Atualizar authState também!
                        currentUser = updatedUser,
                        isLoading = false,
                        error = null
                    )
                    // Recarregar parceiro
                    updatedUser.partnerId?.let { 
                        android.util.Log.d("MainViewModel", "Carregando parceiro: $it")
                        loadPartner(it) 
                    }
                },
                onFailure = { error ->
                    android.util.Log.e("MainViewModel", "Erro ao parear: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro ao parear usuários"
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
                        error = null,
                        successMessage = "Foto enviada com sucesso! ❤️"
                    )
                    // Recarregar última foto após envio
                    loadLatestPhoto()
                    // Atualizar widget imediatamente
                    updateWidget()
                    // Limpar mensagem de sucesso após alguns segundos
                    viewModelScope.launch {
                        delay(3000)
                        _uiState.value = _uiState.value.copy(successMessage = null)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro ao enviar foto"
                    )
                }
            )
        }
    }

    fun unpairPartner() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentUser = _uiState.value.currentUser ?: return@launch

            userRepository.unpairUsers(currentUser.id).fold(
                onSuccess = { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        authState = AuthState.Ready(updatedUser), // Atualizar authState também!
                        currentUser = updatedUser,
                        partner = null,
                        isLoading = false,
                        error = null
                    )
                    // Parar polling
                    pollingJob?.cancel()
                    pollingJob = null
                    // Atualizar widget
                    updateWidget()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro ao desparar"
                    )
                }
            )
        }
    }
    
    private fun updateWidget() {
        viewModelScope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(context)
                    .getGlanceIds(PhotoWidget::class.java)
                    .firstOrNull()
                
                glanceId?.let {
                    PhotoWidget().update(context, it)
                    android.util.Log.d("MainViewModel", "Widget atualizado com sucesso")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Erro ao atualizar widget: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
