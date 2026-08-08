package com.lladlam.melox.ui.player

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lladlam.melox.core.lyrics.LyricLine
import com.lladlam.melox.core.lyrics.LyricsDocument
import com.lladlam.melox.core.network.NeteaseSearchClient
import kotlinx.coroutines.delay

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

    var anchorPositionMs by remember(mediaId) { mutableLongStateOf(state.positionMs) }
    var anchorRealtimeMs by remember(mediaId) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var renderedPositionMs by remember(mediaId) { mutableLongStateOf(state.positionMs) }

    LaunchedEffect(state.positionMs, state.isPlaying, mediaId) {
        anchorPositionMs = state.positionMs
        anchorRealtimeMs = SystemClock.elapsedRealtime()
        renderedPositionMs = state.positionMs
    }

    LaunchedEffect(state.isPlaying, mediaId) {
        while (true) {
            renderedPositionMs = if (state.isPlaying) {
                anchorPositionMs + (SystemClock.elapsedRealtime() - anchorRealtimeMs)
            } else {
                anchorPositionMs
            }
            delay(if (state.isPlaying) 50L else 250L)
        }
    }

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
    val highlightedIndex = document?.highlightedIndex(renderedPositionMs)

    LaunchedEffect(highlightedIndex, mediaId) {
        val index = highlightedIndex ?: return@LaunchedEffect
        val target = (index - 2).coerceAtLeast(0)
        runCatching { listState.animateScrollToItem(target) }
    }

    Box(modifier = modifier) {
        when {
            isLoading && document == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            errorMessage != null && document == null -> {
                Text(
                    text = errorMessage.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp,
                )
            }

            document == null || document.lines.isEmpty() -> {
                Text(
                    text = "暂无歌词",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(top = 62.dp, bottom = 86.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
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
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                        ) {
                            SynchronizedLyricText(
                                line = line,
                                positionMs = renderedPositionMs,
                                active = active,
                            )

                            line.translation
                                ?.takeIf(String::isNotBlank)
                                ?.let { translation ->
                                    Text(
                                        text = translation,
                                        modifier = Modifier.padding(top = 6.dp),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = Color.White.copy(
                                            alpha = if (active) 0.72f else 0.28f,
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
                                        color = Color.White.copy(
                                            alpha = if (active) 0.52f else 0.22f,
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

@Composable
private fun SynchronizedLyricText(
    line: LyricLine,
    positionMs: Long,
    active: Boolean,
) {
    val emphasisScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.96f,
        animationSpec = tween(durationMillis = 180),
        label = "lyric-line-scale",
    )

    val annotated = if (active && line.syllables.isNotEmpty()) {
        buildAnnotatedString {
            for (syllable in line.syllables) {
                val progress = when {
                    positionMs < syllable.startTimeMs -> 0f
                    positionMs >= syllable.endTimeMs -> 1f
                    else -> {
                        val duration = (syllable.endTimeMs - syllable.startTimeMs)
                            .coerceAtLeast(1L)
                        ((positionMs - syllable.startTimeMs).toFloat() / duration.toFloat())
                            .coerceIn(0f, 1f)
                    }
                }
                val alpha = 0.30f + (0.70f * progress)
                withStyle(
                    SpanStyle(
                        color = Color.White.copy(alpha = alpha),
                        fontWeight = if (progress > 0f) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                ) {
                    append(syllable.text)
                }
            }
        }
    } else {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = Color.White.copy(alpha = if (active) 1f else 0.36f),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                ),
            ) {
                append(line.text)
            }
        }
    }

    Text(
        text = annotated,
        modifier = Modifier.graphicsLayer {
            scaleX = emphasisScale
            scaleY = emphasisScale
        },
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        fontSize = if (active) 27.sp else 22.sp,
        lineHeight = if (active) 35.sp else 30.sp,
    )
}
