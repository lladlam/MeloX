package com.lladlam.melox.playback

import android.content.Context
import android.content.Intent
import com.lladlam.melox.core.model.SearchSong

object PlaybackCommands {
    const val ACTION_PLAY_SONG = "com.lladlam.melox.action.PLAY_SONG"
    const val EXTRA_ID = "song_id"
    const val EXTRA_TITLE = "song_title"
    const val EXTRA_ARTISTS = "song_artists"
    const val EXTRA_ALBUM = "song_album"
    const val EXTRA_ARTWORK = "song_artwork"
    const val EXTRA_URL = "song_url"

    fun playSong(
        context: Context,
        song: SearchSong,
    ) {
        val intent = Intent(context, MeloXPlaybackService::class.java)
            .setAction(ACTION_PLAY_SONG)
            .putExtra(EXTRA_ID, song.id)
            .putExtra(EXTRA_TITLE, song.name)
            .putExtra(EXTRA_ARTISTS, song.artists)
            .putExtra(EXTRA_ALBUM, song.album)
            .putExtra(EXTRA_ARTWORK, song.artworkUrl)
            .putExtra(EXTRA_URL, song.playbackUrl)

        context.startForegroundService(intent)
    }
}
