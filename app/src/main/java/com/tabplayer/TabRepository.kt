package com.tabplayer

import android.content.Context
import com.google.gson.Gson

object TabRepository {
    fun load(context: Context, name: String): TabTrack {
        val json = context.assets.open(name).bufferedReader().use { it.readText() }
        return Gson().fromJson(json, TabTrack::class.java)
    }
}
