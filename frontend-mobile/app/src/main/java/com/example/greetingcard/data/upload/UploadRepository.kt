package com.example.greetingcard.data.upload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.greetingcard.data.api.ApiService
import com.example.greetingcard.data.api.ReadingDto
import com.example.greetingcard.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    suspend fun upload(imageFile: File, timestamp: String): Result<ReadingDto> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getToken()
                    ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))

                val compressedBytes = compressImage(imageFile)
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    imageFile.name,
                    compressedBytes.toRequestBody("image/jpeg".toMediaType())
                )
                val timestampBody = timestamp.toRequestBody("text/plain".toMediaType())

                val response = apiService.uploadReading("Bearer $token", imagePart, timestampBody)
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Upload failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun compressImage(file: File): ByteArray {
        val original = BitmapFactory.decodeFile(file.absolutePath)
        val maxEdge = 1280
        val width = original.width
        val height = original.height
        val scale = if (width > height) maxEdge.toFloat() / width else maxEdge.toFloat() / height
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            original
        }
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return output.toByteArray()
    }
}
