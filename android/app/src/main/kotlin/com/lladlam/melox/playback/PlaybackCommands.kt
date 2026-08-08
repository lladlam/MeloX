package com.lladlam.melox.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
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
     * Installs the supplied songs as Media3 playlist items using stable MeloX
     * song URIs. MeloXPlaybackService resolves each song ID to a temporary
     * NetEase CDN URL just-in-time when ExoPlayer actually opens the item.
     */
    fun playQueue(
        context: Context,
        songs: List<SearchSong>,
        selectedSongId: Long,
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
                    val queue = songs
                        .ifEmpty { return@addListener }
                        .map { song -> song.toMediaItem() }
                    val startIndex = songs.indexOfFirst { it.id == selectedSongId }
                        .takeIf { it >= 0 }
                        ?: 0

                    activeController?.takeIf { it !== controller }?.release()
                    activeController = controller

                    controller.setMediaItems(queue, startIndex, C.TIME_UNSET)
                    controller.prepare()
                    controller.play()

                    Log.d(
                        TAG,
                        "Playback queue dispatched: size=${queue.size}, start=$startIndex, song=$selectedSongId",
                    )
                } catch (error: Throwable) {
                    Log.e(TAG, "Unable to connect MediaController", error)
                    onFailure?.invoke(error)
                }
            },
            mainExecutor,
        )
    }

    private fun SearchSong.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(artists)
            .setAlbumTitle(album)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .apply {
                artworkUrl
                    ?.takeIf(String::isNotBlank)
                    ?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(NeteasePlaybackResolver.uriForSong(id))
            .setMediaMetadata(metadata)
            .build()
    }
}
