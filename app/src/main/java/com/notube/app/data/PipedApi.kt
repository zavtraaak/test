package com.notube.app.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface PipedApi {
    @GET("trending")
    suspend fun trending(@Query("region") region: String = "US"): List<PipedFeedItem>

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "videos",
    ): PipedSearchResponse

    @GET("streams/{id}")
    suspend fun streams(@Path("id") videoId: String): PipedStreamResponse
}

object PipedClient {
    // Pool of public Piped API instances. We try them in order until one responds.
    // Source: https://github.com/TeamPiped/Piped/wiki/Instances
    private val INSTANCES = listOf(
        "https://pipedapi.kavin.rocks/",
        "https://api.piped.privacydev.net/",
        "https://pipedapi.r4fo.com/",
        "https://pipedapi.adminforge.de/",
        "https://piapi.ggtyler.dev/",
    )

    @Volatile var currentInstance: String = INSTANCES.first()
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun build(baseUrl: String): PipedApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(JsonConverterFactory(json))
        .build()
        .create(PipedApi::class.java)

    /** Run [block] against each instance in turn until one succeeds. */
    suspend fun <T> withFallback(block: suspend (PipedApi) -> T): T {
        var last: Throwable? = null
        for (instance in INSTANCES) {
            try {
                val result = block(build(instance))
                currentInstance = instance
                return result
            } catch (t: Throwable) {
                last = t
            }
        }
        throw last ?: IllegalStateException("All Piped instances failed")
    }
}
