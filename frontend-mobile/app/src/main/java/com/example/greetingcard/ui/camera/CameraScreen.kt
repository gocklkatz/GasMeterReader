package com.example.greetingcard.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                onPhotoCaptured = viewModel::onPhotoCaptured,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission required")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }

        // History button top-left
        IconButton(
            onClick = onNavigateToHistory,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.List, contentDescription = "History", tint = Color.White)
        }

        // Logout button top-right
        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Logout", color = Color.White)
        }

        // Upload status overlay
        when (val uploadState = state.uploadState) {
            is UploadState.Uploading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is UploadState.Success -> {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.resetUploadState()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Upload successful!",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            is UploadState.Error -> {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::resetUploadState) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uploadState.message)
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun CameraPreview(
    onPhotoCaptured: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    DisposableEffect(Unit) {
        onDispose {
            captureExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = { capturePhoto(context, imageCapture, captureExecutor, onPhotoCaptured) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {}
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onPhotoCaptured: (File) -> Unit
) {
    val filename = java.text.SimpleDateFormat("'IMG_'yyyyMMdd_HHmmssSSS", java.util.Locale.US).format(java.util.Date()) + ".jpg"
    val photoFile = File(context.cacheDir, filename)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onPhotoCaptured(photoFile)
            }
            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
