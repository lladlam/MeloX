package com.lladlam.melox.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.lladlam.melox.core.network.NeteaseSearchClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class NeteasePlaybackResolver(
    private val client: NeteaseSearchClient = NeteaseSearchClient(),
) : ResolvingDataSource.Resolver {
    private val resolvedUris = ConcurrentHashMap<Long, Uri>()

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != MELOX_SCHEME || uri.host != SONG_HOST) {
            return dataSpec
        }

        val songId = uri.lastPathSegment?.toLongOrNull()
            ?: throw IOException("Invalid MeloX song URI: $uri")

        val resolved = resolvedUris[songId] ?: run {
            val resolvedUrl = client.playbackUrlBlocking(songId)
            Uri.parse(resolvedUrl).also { resolvedUri ->
                resolvedUris[songId] = resolvedUri
            }
        }

        return dataSpec.withUri(resolved)
    }

    override fun resolveReportedUri(uri: Uri): Uri {
        if (uri.scheme != MELOX_SCHEME || uri.host != SONG_HOST) return uri
        val songId = uri.lastPathSegment?.toLongOrNull() ?: return uri
        return resolvedUris[songId] ?: uri
    }

    companion object {
        private const val MELOX_SCHEME = "melox"
        private const val SONG_HOST = "song"

        fun uriForSong(songId: Long): Uri =
            Uri.Builder()
                .scheme(MELOX_SCHEME)
                .authority(SONG_HOST)
                .appendPath(songId.toString())
                .build()
    }
}
