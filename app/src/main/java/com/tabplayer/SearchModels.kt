package com.tabplayer

data class SearchResult(
    val track_id: String,
    val title: String,
    val artists: String,
    val duration: Int?
)

data class SearchResponse(
    val results: List<SearchResult>
)
