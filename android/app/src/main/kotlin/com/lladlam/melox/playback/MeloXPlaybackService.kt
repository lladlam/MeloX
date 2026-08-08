package com.lladlam.melox.playback

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class MeloXPlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36",
                    "Referer" to "https://music.163.com/",
                ),
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(
                        TAG,
                        "Playback failed: code=${error.errorCodeName}, message=${error.message}",
                        error,
                    )
                }
            },
        )

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

        Log.d(TAG, "Preparing playback for song=$id url=$url")

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

    private companion object {
        const val TAG = "MeloXPlayback"
    }
}
