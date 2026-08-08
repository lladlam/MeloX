package com.lladlam.melox.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lladlam.melox.MainActivity

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

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            httpDataSourceFactory,
            NeteasePlaybackResolver(),
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(resolvingDataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
            }

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(
                        TAG,
                        "Playback failed: code=${error.errorCodeName}, message=${error.message}",
                        error,
                    )
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d(
                        TAG,
                        "isPlaying=$isPlaying, ongoing=${isPlaybackOngoing()}",
                    )
                }
            },
        )

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_NOW_PLAYING
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? {
        Log.d(TAG, "Controller connected: ${controllerInfo.packageName}")
        return mediaSession
    }

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
