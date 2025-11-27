package com.example.mobile.network

import com.example.mobile.services.auth.AuthRepository
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val authRepository: AuthRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = authRepository.tokenState.value
        return if (token.isNullOrBlank()) {
            chain.proceed(original)
        } else {
            val newReq = original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newReq)
        }
    }
}

