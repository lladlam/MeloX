package com.lladlam.melox.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lladlam.melox.core.model.SearchSong
import java.util.concurrent.Executor

object PlaybackCommands {
    private const val TAG = "MeloXPlayback"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command -> mainHandler.post(command) }

    @Volatile
    private var activeController: MediaController? = null

    /**
     * Connects the app UI to [MeloXPlaybackService] through Media3's standard
     * controller/session path. This is important: MediaSessionService uses the
     * connected session to publish the system MediaStyle notification and to
     * promote itself to a mediaPlayback foreground service while playback is
     * ongoing.
     */
    fun playSong(
        context: Context,
        song: SearchSong,
        playbackUrl: String,
        onFailure: ((Throwable) -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        val token = SessionToken(
            appContext,
            ComponentName(appContext, MeloXPlaybackService::class.java),
        )
        val controllerFuture = MediaController.Builder(appContext, token).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    val metadata = MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtist(song.artists)
                        .setAlbumTitle(song.album)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .apply {
                            song.artworkUrl
                                ?.takeIf(String::isNotBlank)
                                ?.let { setArtworkUri(Uri.parse(it)) }
                        }
                        .build()

                    val item = MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(playbackUrl)
                        .setMediaMetadata(metadata)
                        .build()

                    activeController?.takeIf { it !== controller }?.release()
                    activeController = controller

                    controller.setMediaItem(item)
                    controller.prepare()
                    controller.play()

                    Log.d(
                        TAG,
                        "Playback dispatched through MediaController: song=${song.id}",
                    )
                } catch (error: Throwable) {
                    Log.e(TAG, "Unable to connect MediaController", error)
                    onFailure?.invoke(error)
                }
            },
            mainExecutor,
        )
    }
}
