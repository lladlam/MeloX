package com.lladlam.melox.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lladlam.melox.core.account.NeteaseSessionStore
import com.lladlam.melox.core.library.NeteaseLibraryClient
import com.lladlam.melox.core.library.NeteaseLibrarySnapshot
import com.lladlam.melox.core.library.NeteasePlaylistSummary
import com.lladlam.melox.core.model.SearchSong
import com.lladlam.melox.playback.PlaybackCommands
import kotlinx.coroutines.launch

private sealed interface LibraryDestination {
    data class Songs(val title: String, val songs: List<SearchSong>) : LibraryDestination
    data class Playlist(val playlist: NeteasePlaylistSummary) : LibraryDestination
}

@Composable
fun LibraryScreen(
    session: NeteaseSessionStore,
    onLogin: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val client = remember(appContext) {
        NeteaseLibraryClient(
            cookieProvider = { NeteaseSessionStore.readCookie(appContext) },
        )
    }

    var snapshot by remember(session.cookie) { mutableStateOf<NeteaseLibrarySnapshot?>(null) }
    var destination by remember(session.cookie) { mutableStateOf<LibraryDestination?>(null) }
    var destinationSongs by remember { mutableStateOf<List<SearchSong>>(emptyList()) }
    var loading by remember(session.cookie) { mutableStateOf(false) }
    var errorMessage by remember(session.cookie) { mutableStateOf<String?>(null) }

    suspend fun refreshLibrary() {
        if (!session.isLoggedIn) return
        if (session.profile == null) session.refreshProfile(force = true)
        val userId = session.profile?.userId ?: return
        loading = true
        errorMessage = null
        runCatching { client.snapshot(userId) }
            .onSuccess { snapshot = it }
            .onFailure { errorMessage = it.message ?: "音乐库加载失败" }
        loading = false
    }

    LaunchedEffect(session.cookie, session.profile?.userId) {
        if (session.isLoggedIn && snapshot == null && !loading) {
            refreshLibrary()
        }
    }

    LaunchedEffect(destination) {
        val target = destination
        when (target) {
            is LibraryDestination.Songs -> destinationSongs = target.songs
            is LibraryDestination.Playlist -> {
                loading = true
                errorMessage = null
                runCatching { client.playlistDetail(target.playlist.id) }
                    .onSuccess { destinationSongs = it.songs }
                    .onFailure { errorMessage = it.message ?: "歌单加载失败" }
                loading = false
            }
            null -> destinationSongs = emptyList()
        }
    }

    if (!session.isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 48.dp),
        ) {
            Text("音乐库", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogin),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("登录网易云音乐", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "登录后显示你的歌单、我喜欢的音乐和最近播放。",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
                    )
                }
            }
        }
        return
    }

    val currentDestination = destination
    if (currentDestination != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "‹",
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { destination = null }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 36.sp,
                    lineHeight = 38.sp,
                )
                Text(
                    text = when (currentDestination) {
                        is LibraryDestination.Songs -> currentDestination.title
                        is LibraryDestination.Playlist -> currentDestination.playlist.name
                    },
                    modifier = Modifier.weight(1f),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (loading && destinationSongs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SongList(
                    songs = destinationSongs,
                    onSongClick = { song ->
                        PlaybackCommands.playQueue(
                            context = context,
                            songs = destinationSongs,
                            selectedSongId = song.id,
                            onFailure = { errorMessage = it.message ?: "播放失败" },
                        )
                    },
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 42.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("音乐库", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
        }

        errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }

        if (loading && snapshot == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        snapshot?.let { data ->
            item {
                LibraryShortcut(
                    title = "我喜欢的音乐",
                    subtitle = "${data.likedSongs.size}${if (data.likedSongs.size >= 100) "+" else ""} 首已加载",
                    onClick = {
                        destination = LibraryDestination.Songs("我喜欢的音乐", data.likedSongs)
                    },
                )
            }
            item {
                LibraryShortcut(
                    title = "最近播放",
                    subtitle = "${data.recentSongs.size} 首",
                    onClick = {
                        destination = LibraryDestination.Songs("最近播放", data.recentSongs)
                    },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "歌单",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }

            items(data.playlists, key = { it.id }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    onClick = { destination = LibraryDestination.Playlist(playlist) },
                )
            }
        }
    }
}

@Composable
private fun LibraryShortcut(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text("›", fontSize = 27.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: NeteasePlaylistSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = playlist.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(9.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${playlist.trackCount} 首${playlist.creatorName.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SongList(
    songs: List<SearchSong>,
    onSongClick: (SearchSong) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
    ) {
        items(songs, key = { it.id }) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(7.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        song.name,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artists,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
