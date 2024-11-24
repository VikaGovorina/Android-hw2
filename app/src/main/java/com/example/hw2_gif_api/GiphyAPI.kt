package com.example.hw2_gif_api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.serialization.Serializable


interface GiphyAPI {
    @GET("v1/gifs/trending")
    suspend fun getGIFs(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<Gifs>
}

@Serializable
data class Gifs(
    val data: List<GifData>
)

@Serializable
data class GifData(
    val id: String,
    val images: Images
)

@Serializable
data class Images(
    val original: Original
)

@Serializable
data class Original(
    val url: String,
    val width: String,
    val height: String
)
