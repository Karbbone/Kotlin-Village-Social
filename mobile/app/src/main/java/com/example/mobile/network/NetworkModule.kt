package com.example.mobile.network

import android.content.Context
import com.example.mobile.auth.AuthRepository
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://mobile.maillet.bzh/"

    fun createApi(context: Context, authRepository: AuthRepository): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 20MB HTTP cache stored in app cache dir
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 20L * 1024L * 1024L)

        // Add default JSON headers
        val defaultHeaders = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(req)
        }

        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .cache(cache)
            .addInterceptor(AuthInterceptor(authRepository)) // add auth first so it's logged
            .addInterceptor(defaultHeaders)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
