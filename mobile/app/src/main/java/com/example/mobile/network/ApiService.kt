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

data class PhotoDto(
    val id: Int,
    val url: String
)

data class EventDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val date: String? = null,
    val city: String,
    val photos: List<PhotoDto>? = null
)

data class SearchEventsRequest(
    val types: List<String>? = null,
    val cityName: String? = null
)

// New payload for creating an event via /events/cities/{city_name}
data class CreateEventRequest(
    val title: String,
    val description: String?,
    val location: String?,
    val date: String?,
    val types: List<String>?,
    val photoUrls: List<String>?
)

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

    @GET("events/cities/{cityName}")
    suspend fun getEventsByCity(@Path("cityName") cityName: String): List<EventDto>

    @POST("events/search")
    suspend fun searchEvents(@Body body: SearchEventsRequest): List<EventDto>

    // Create an event tied to a city (cityName in path)
    @POST("events/cities/{cityName}")
    suspend fun createEventForCity(@Path("cityName") cityName: String, @Body body: CreateEventRequest): EventDto

    // Fetch past events for the authenticated user
    @GET("events/me/past")
    suspend fun getMyPastEvents(): List<EventDto>
}
