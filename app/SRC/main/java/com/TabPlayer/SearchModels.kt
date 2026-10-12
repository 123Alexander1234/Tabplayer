package com.tabplayer

data class SearchResult(
    val video_id: String,
    val title: String,
    val uploader: String?,
    val duration: Int?,
    val thumbnail: String?
)

data class SearchResponse(
    val results: List<SearchResult>
)
