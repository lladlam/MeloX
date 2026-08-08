package com.lladlam.melox.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lladlam.melox.core.lyrics.LyricsDocument
import com.lladlam.melox.core.network.NeteaseSearchClient

@Composable
fun MeloXLyricsPanel(
    state: MeloXPlaybackUiState,
    modifier: Modifier = Modifier,
) {
    val client = remember { NeteaseSearchClient() }
    val listState = rememberLazyListState()
    val mediaId = state.mediaId
    var lyrics by remember(mediaId) { mutableStateOf<LyricsDocument?>(null) }
    var isLoading by remember(mediaId) { mutableStateOf(false) }
    var errorMessage by remember(mediaId) { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaId) {
        val songId = mediaId?.toLongOrNull() ?: return@LaunchedEffect
        isLoading = true
        errorMessage = null
        runCatching { client.lyrics(songId) }
            .onSuccess { lyrics = it }
            .onFailure { errorMessage = it.message ?: "歌词加载失败" }
        isLoading = false
    }

    val document = lyrics
    val highlightedIndex = document?.highlightedIndex(state.positionMs)

    LaunchedEffect(highlightedIndex, mediaId) {
        val index = highlightedIndex ?: return@LaunchedEffect
        val target = (index - 2).coerceAtLeast(0)
        runCatching {
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)),
    ) {
        when {
            isLoading && document == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            errorMessage != null && document == null -> {
                Text(
                    text = errorMessage.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            document == null || document.lines.isEmpty() -> {
                Text(
                    text = "暂无歌词",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    itemsIndexed(
                        items = document.lines,
                        key = { index, line -> "${line.timeMs}-$index" },
                    ) { index, line ->
                        val active = index == highlightedIndex
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { state.seekTo(line.timeMs) }
                                .padding(horizontal = 22.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = line.text,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = if (active) 25.sp else 21.sp,
                                lineHeight = if (active) 32.sp else 28.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (active) 1f else 0.36f,
                                ),
                            )

                            line.translation
                                ?.takeIf(String::isNotBlank)
                                ?.let { translation ->
                                    Text(
                                        text = translation,
                                        modifier = Modifier.padding(top = 5.dp),
                                        fontSize = 14.sp,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = if (active) 0.68f else 0.26f,
                                        ),
                                    )
                                }

                            line.romanization
                                ?.takeIf(String::isNotBlank)
                                ?.let { romanization ->
                                    Text(
                                        text = romanization,
                                        modifier = Modifier.padding(top = 3.dp),
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = if (active) 0.48f else 0.20f,
                                        ),
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}
