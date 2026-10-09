package com.tabplayer

data class TabNote(
    val timeMs: Long,
    val string: Int,     // 1=G, 2=D, 3=A, 4=E
    val fret: Int,
    val durationMs: Long
)

data class TabTrack(
    val title: String,
    val tuning: List<String> = listOf("E", "A", "D", "G"),
    val bpm: Int = 120,
    val notes: List<TabNote>
)
