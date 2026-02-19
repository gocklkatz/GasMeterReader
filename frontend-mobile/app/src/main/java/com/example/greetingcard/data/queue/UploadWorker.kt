package com.example.greetingcard.data.queue

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.greetingcard.data.upload.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository,
    private val pendingUploadDao: PendingUploadDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uploadId = inputData.getInt(KEY_UPLOAD_ID, -1)
        if (uploadId == -1) return Result.failure()

        val pending = pendingUploadDao.getById(uploadId) ?: return Result.failure()
        pendingUploadDao.updateStatus(uploadId, UploadStatus.UPLOADING.name)

        val file = File(pending.filePath)
        if (!file.exists()) {
            pendingUploadDao.updateStatus(uploadId, UploadStatus.FAILED.name)
            return Result.failure()
        }

        val result = uploadRepository.upload(file, pending.timestamp)
        return if (result.isSuccess) {
            pendingUploadDao.updateStatus(uploadId, UploadStatus.DONE.name)
            Result.success()
        } else {
            pendingUploadDao.updateStatus(uploadId, UploadStatus.FAILED.name)
            Result.retry()
        }
    }

    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
    }
}
