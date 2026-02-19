package com.example.greetingcard.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String)
data class ReadingDto(val id: Int, val timestamp: String, val imagePath: String)

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("readings")
    suspend fun getReadings(@Header("Authorization") token: String): Response<List<ReadingDto>>

    @Multipart
    @POST("readings")
    suspend fun uploadReading(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("timestamp") timestamp: RequestBody
    ): Response<ReadingDto>
}
