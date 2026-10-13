package com.tabplayer

data class SearchResult(
    val track_id: String,
    val title: String,
    val artist: String?,
    val duration: Int?,
    val thumbnail: String?
)

data class SearchResponse(
    val results: List<SearchResult>
)