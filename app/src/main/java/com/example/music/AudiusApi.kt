package com.example.music

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Data classes to represent the JSON structure from the Audius API
data class AudiusResponse(val data: List<Track>)

data class Track(
    val id: String,
    val title: String,
    val user: User
)

data class User(val name: String)

// Retrofit service interface to define the API endpoints
interface AudiusApiService {
    @GET("v1/tracks/trending")
    suspend fun getTrendingTracks(): AudiusResponse
}

// Singleton object to provide a single instance of the Retrofit service
object AudiusApi {
    private const val BASE_URL = "https://api.audius.co/"

    val retrofitService: AudiusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudiusApiService::class.java)
    }
}
