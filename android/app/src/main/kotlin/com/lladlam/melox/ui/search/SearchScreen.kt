package com.lladlam.melox.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lladlam.melox.core.model.SearchSong
import com.lladlam.melox.core.network.NeteaseSearchClient
import com.lladlam.melox.playback.PlaybackCommands
import kotlinx.coroutines.launch

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { NeteaseSearchClient() }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchSong>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var resolvingSongId by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submitSearch() {
        val keywords = query.trim()
        if (keywords.isEmpty() || isLoading) return

        scope.launch {
            isLoading = true
            errorMessage = null
            runCatching { client.searchSongs(keywords) }
                .onSuccess { results = it }
                .onFailure { errorMessage = it.message ?: "搜索失败" }
            isLoading = false
        }
    }

    fun playSong(song: SearchSong) {
        if (resolvingSongId != null) return

        scope.launch {
            resolvingSongId = song.id
            errorMessage = null
            runCatching { client.playbackUrl(song.id) }
                .onSuccess { playbackUrl ->
                    PlaybackCommands.playSong(
                        context = context,
                        song = song,
                        playbackUrl = playbackUrl,
                    )
                }
                .onFailure {
                    errorMessage = it.message ?: "无法获取歌曲音源"
                }
            resolvingSongId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = "搜索",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("歌曲") },
                placeholder = { Text("输入歌曲或歌手") },
            )
            Button(
                onClick = ::submitSearch,
                enabled = query.isNotBlank() && !isLoading,
            ) {
                Text("搜索")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            isLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            results.isEmpty() -> {
                Text(
                    text = "先搜索一首歌。当前 Android 版会使用 MeloX 同源的网易云 EAPI 获取结果。",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                ) {
                    items(
                        items = results,
                        key = { it.id },
                    ) { song ->
                        SongResultRow(
                            song = song,
                            isResolving = resolvingSongId == song.id,
                            onClick = { playSong(song) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongResultRow(
    song: SearchSong,
    isResolving: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isResolving, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = buildString {
                    append(song.artists)
                    if (song.album.isNotBlank()) {
                        append(" · ")
                        append(song.album)
                    }
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (isResolving) {
            CircularProgressIndicator()
        }
    }
}
