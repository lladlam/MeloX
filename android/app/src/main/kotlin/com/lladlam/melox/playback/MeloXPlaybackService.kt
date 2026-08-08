package com.lladlam.melox.playback

import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MeloXPlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == PlaybackCommands.ACTION_PLAY_SONG) {
            playSong(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun playSong(intent: Intent) {
        val url = intent.getStringExtra(PlaybackCommands.EXTRA_URL) ?: return
        val id = intent.getLongExtra(PlaybackCommands.EXTRA_ID, -1L)
        val title = intent.getStringExtra(PlaybackCommands.EXTRA_TITLE).orEmpty()
        val artists = intent.getStringExtra(PlaybackCommands.EXTRA_ARTISTS).orEmpty()
        val album = intent.getStringExtra(PlaybackCommands.EXTRA_ALBUM).orEmpty()
        val artwork = intent.getStringExtra(PlaybackCommands.EXTRA_ARTWORK)

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artists)
            .setAlbumTitle(album)
            .apply {
                artwork
                    ?.takeIf(String::isNotBlank)
                    ?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()

        val item = MediaItem.Builder()
            .setMediaId(if (id > 0L) id.toString() else url)
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()

        player?.apply {
            setMediaItem(item)
            prepare()
            play()
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
