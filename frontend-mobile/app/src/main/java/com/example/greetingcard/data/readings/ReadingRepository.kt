package com.example.greetingcard.data.readings

import com.example.greetingcard.data.api.ApiService
import com.example.greetingcard.data.api.ReadingDto
import com.example.greetingcard.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getReadings(): Result<List<ReadingDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getToken()
                    ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
                val response = apiService.getReadings("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(Exception("Failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
