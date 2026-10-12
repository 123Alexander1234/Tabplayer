package com.tabplayer

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val SERVER_URL = "https://tabplayer-e29cf.containers.snapdeploy.app"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("$SERVER_URL/search?q=$encoded")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val text = response.body?.string() ?: return@withContext emptyList()
            gson.fromJson(text, SearchResponse::class.java).results
        }
    }

    suspend fun transcribeByVideoId(videoId: String): TabTrack = withContext(Dispatchers.IO) {
        val body = "{\"video_id\":\"$videoId\"}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SERVER_URL/transcribe_url")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Ошибка сервера: ${response.code}")
            val text = response.body?.string() ?: error("Пустой ответ")
            gson.fromJson(text, TabTrack::class.java)
        }
    }
}
