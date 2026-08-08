package com.lladlam.melox.core.model

data class SearchSong(
    val id: Long,
    val name: String,
    val artists: String,
    val album: String,
    val artworkUrl: String?,
) {
    val playbackUrl: String
        get() = "https://music.163.com/song/media/outer/url?id=$id"
}
