package com.example.hw2_gif_api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory


sealed class State {
    object Loading : State()
    data class Ok(val gifs: Gifs) : State()
    data class Error(val error: String) : State()
}

class RetrofitController {
    private val baseURL = "https://api.giphy.com/"
    private val apiKey = "6rwVLgcTqp2ehwbYcMUpsu4ffowoelDm"

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseURL)
        .addConverterFactory(
            Json { ignoreUnknownKeys = true }
                .asConverterFactory(
                    "application/json; charset=UTF8".toMediaType()
                )
        )
        .build()

    private val GIFsApi = retrofit.create(GiphyAPI::class.java)

    suspend fun requestGifs(offset: Int): State {
        val response = GIFsApi.getGIFs(apiKey, offset)
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                return State.Ok(body)
            }
        }
        return State.Error(response.code().toString())
    }

}

