package com.example.mobile.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.Response

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val displayName: String, val email: String, val password: String)

// New DTO for cities returned by the backend
data class CityDto(val id: Int, val name: String, val postalCode: String)

data class UserDto(val id: Int, val email: String, val displayName: String)

data class AuthResponse(val access_token: String, val user: UserDto)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    // Fetch all cities (cached client-side)
    @GET("cities/")
    suspend fun getCities(): List<CityDto>

    // Manage user-city associations
    @POST("cities/{cityId}/users/{userId}")
    suspend fun addUserCity(@Path("cityId") cityId: Int, @Path("userId") userId: Int): Response<Unit>

    @DELETE("cities/{cityId}/users/{userId}")
    suspend fun removeUserCity(@Path("cityId") cityId: Int, @Path("userId") userId: Int): Response<Unit>

    // Fetch user's cities with required route (trailing slash to avoid redirects)
    @GET("cities/users/{userId}/")
    suspend fun getUserCities(@Path("userId") userId: Int): List<CityDto>
}
