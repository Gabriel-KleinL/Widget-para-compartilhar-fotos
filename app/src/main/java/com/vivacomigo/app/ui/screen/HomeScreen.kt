package com.vivacomigo.app.ui.screen

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.vivacomigo.app.data.model.Photo
import com.vivacomigo.app.data.model.User
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    currentUser: User,
    partner: User?,
    latestPhoto: Photo?,
    latestPhotoUri: Uri?,
    onSendPhoto: (Uri) -> Unit,
    onUnpair: () -> Unit,
    isLoading: Boolean,
    successMessage: String? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mostrar mensagem de sucesso
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    // Permissões para câmera e galeria
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    }
    val permissionsState = rememberMultiplePermissionsState(permissions)
    
    // Launcher para galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onSendPhoto(it) }
    }
    
    // Launcher para câmera
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { onSendPhoto(it) }
        }
    }
    
    // Função para criar URI temporária para foto da câmera
    fun createCameraImageUri(): Uri? {
        return try {
            val imageFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Dialog para escolher entre galeria e câmera
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    fun launchImagePicker() {
        if (permissionsState.allPermissionsGranted) {
            showImageSourceDialog = true
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
    
    fun launchCamera() {
        val uri = createCameraImageUri()
        if (uri != null) {
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }
    
    fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Viva Comigo") },
                actions = {
                    if (partner != null) {
                        IconButton(
                            onClick = onUnpair,
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "🔌",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (partner != null && currentUser.partnerId != null) {
                FloatingActionButton(
                    onClick = { launchImagePicker() },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Enviar foto",
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Partner info ou aguardando parceiro
                    if (currentUser.partnerId != null) {
                        if (partner != null) {
                            // Parceiro carregado
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Conectado com",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = partner.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        } else {
                            // Aguardando parceiro
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aguardando o parceiro...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Latest photo
                        Text(
                            text = "Última foto recebida",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (latestPhotoUri != null) {
                                    AsyncImage(
                                        model = latestPhotoUri,
                                        contentDescription = "Foto compartilhada",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "❤️",
                                            style = MaterialTheme.typography.displayLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Nenhuma foto ainda",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (partner != null) {
                            Text(
                                text = "Toque no + para enviar uma foto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Dialog para escolher fonte da imagem
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text("Escolher foto") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                launchGallery()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 Galeria")
                        }
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                launchCamera()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📸 Câmera")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showImageSourceDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
