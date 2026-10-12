package com.tabplayer

import android.content.Context
import com.google.gson.Gson

object TabRepository {
    private val gson = Gson()

    fun load(context: Context, name: String): TabTrack {
        val json = context.assets.open(name).bufferedReader().use { it.readText() }
        return gson.fromJson(json, TabTrack::class.java)
    }

    fun parse(json: String): TabTrack {
        return gson.fromJson(json, TabTrack::class.java)
    }
}
