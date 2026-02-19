package com.example.greetingcard.ui.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.greetingcard.data.queue.PendingUpload
import com.example.greetingcard.data.queue.PendingUploadDao
import com.example.greetingcard.data.queue.UploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

data class CameraUiState(
    val uploadState: UploadState = UploadState.Idle
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val pendingUploadDao: PendingUploadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(uploadState = UploadState.Uploading)

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val timestamp = sdf.format(Date())

            // Copy to permanent storage so the file survives until the worker runs
            val permanentDir = File(context.filesDir, "pending_uploads").also { it.mkdirs() }
            val permanentFile = File(permanentDir, file.name)
            file.copyTo(permanentFile, overwrite = true)

            val pendingId = pendingUploadDao.insert(
                PendingUpload(filePath = permanentFile.absolutePath, timestamp = timestamp)
            ).toInt()

            val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(workDataOf(UploadWorker.KEY_UPLOAD_ID to pendingId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workRequest.id)
                .collect { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.SUCCEEDED ->
                            _uiState.value = _uiState.value.copy(uploadState = UploadState.Success)
                        WorkInfo.State.FAILED ->
                            _uiState.value = _uiState.value.copy(
                                uploadState = UploadState.Error("Upload failed — will retry when online")
                            )
                        WorkInfo.State.RUNNING ->
                            _uiState.value = _uiState.value.copy(uploadState = UploadState.Uploading)
                        else -> Unit
                    }
                }
        }
    }

    fun resetUploadState() {
        _uiState.value = _uiState.value.copy(uploadState = UploadState.Idle)
    }
}
