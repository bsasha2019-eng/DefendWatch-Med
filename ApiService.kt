package com.example.myapplication

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Моделі даних
data class MedicalRequest(val description: String)
data class MedicalResponse(val category: String, val advice: String)

// Інтерфейс API
interface ApiService {
    @POST("api/ai-guide")
    suspend fun getAiAdvice(@Body request: MedicalRequest): MedicalResponse
}

// Налаштування Retrofit
object RetrofitClient {
    private const val BASE_URL = "http://127.0.0.1:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
