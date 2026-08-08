package com.lladlam.melox.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import kotlin.math.roundToLong

@Composable
fun MeloXIOSNowPlayingV2(
    state: MeloXPlaybackUiState,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(MeloXNowPlayingPage.Artwork) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        MeloXIOSBackdrop(state.artworkUrl)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(width = 60.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.52f)),
                )
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (
                        initialState != MeloXNowPlayingPage.Artwork &&
                        targetState != MeloXNowPlayingPage.Artwork
                    ) {
                        fadeIn(tween(440)) togetherWith fadeOut(tween(300))
                    } else {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(240))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "melox-ios-now-playing-page-v2",
            ) { selectedPage ->
                when (selectedPage) {
                    MeloXNowPlayingPage.Artwork -> MeloXIOSArtworkPageV2(state)
                    MeloXNowPlayingPage.Lyrics -> MeloXIOSLyricsPanel(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                    MeloXNowPlayingPage.Queue -> MeloXQueuePanel(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            MeloXIOSBottomControlsV2(
                state = state,
                page = page,
                onPageSelected = { destination ->
                    page = if (page == destination) {
                        MeloXNowPlayingPage.Artwork
                    } else {
                        destination
                    }
                },
            )
        }
    }
}

@Composable
private fun MeloXIOSBackdrop(artworkUrl: String?) {
    Box(Modifier.fillMaxSize()) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.34f)
                    .blur(
                        radius = 46.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.03f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.50f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun MeloXIOSArtworkPageV2(state: MeloXPlaybackUiState) {
    val artworkScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.74f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 210f),
        label = "melox-ios-artwork-scale-v2",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.34f))

        Artwork(
            url = state.artworkUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(artworkScale)
                .shadow(
                    elevation = if (state.isPlaying) 26.dp else 14.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.34f),
                    spotColor = Color.Black.copy(alpha = 0.34f),
                )
                .clip(RoundedCornerShape(12.dp)),
        )

        Spacer(Modifier.weight(0.18f))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = state.title.ifBlank { "正在播放" },
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.artist,
                color = Color.White.copy(alpha = 0.64f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 19.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun MeloXIOSBottomControlsV2(
    state: MeloXPlaybackUiState,
    page: MeloXNowPlayingPage,
    onPageSelected: (MeloXNowPlayingPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(279.dp),
    ) {
        MeloXIOSProgressControlV2(state)
        Spacer(Modifier.height(19.dp))
        MeloXIOSTransportControlsV2(state)
        Spacer(Modifier.height(31.dp))
        MeloXIOSVolumeControlV2(state)
        Spacer(Modifier.height(3.dp))
        MeloXIOSPageSelectorV2(
            state = state,
            page = page,
            onPageSelected = onPageSelected,
        )
    }
}

@Composable
private fun MeloXIOSProgressControlV2(state: MeloXPlaybackUiState) {
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Slider(
            value = progress,
            onValueChange = { value ->
                if (state.durationMs > 0L) {
                    state.seekTo((state.durationMs * value).roundToLong())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White),
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
        ) {
            Text(
                text = formatDurationV2(state.positionMs),
                modifier = Modifier.align(Alignment.CenterStart),
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.sp,
            )

            MeloXQualityChipV2(
                modifier = Modifier.align(Alignment.Center),
            )

            Text(
                text = "−${formatDurationV2((state.durationMs - state.positionMs).coerceAtLeast(0L))}",
                modifier = Modifier.align(Alignment.CenterEnd),
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun MeloXQualityChipV2(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { expanded = true }
                .padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MeloXGlyph(
                glyph = MeloXGlyphKind.Waveform,
                modifier = Modifier.size(11.dp),
                color = Color.White.copy(alpha = 0.86f),
            )
            Text(
                text = "标准",
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("✓ 标准") },
                onClick = { expanded = false },
            )
        }
    }
}

@Composable
private fun MeloXIOSTransportControlsV2(state: MeloXPlaybackUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        MeloXTransportIconButtonV2(
            glyph = MeloXGlyphKind.Backward,
            visualSize = 34.dp,
            enabled = state.hasPrevious || state.repeatMode == Player.REPEAT_MODE_ALL,
            onClick = state::previous,
        )
        Spacer(Modifier.weight(1f))
        MeloXTransportIconButtonV2(
            glyph = if (state.isPlaying) MeloXGlyphKind.Pause else MeloXGlyphKind.Play,
            visualSize = 48.dp,
            enabled = true,
            onClick = state::togglePlayPause,
        )
        Spacer(Modifier.weight(1f))
        MeloXTransportIconButtonV2(
            glyph = MeloXGlyphKind.Forward,
            visualSize = 34.dp,
            enabled = state.hasNext || state.repeatMode == Player.REPEAT_MODE_ALL,
            onClick = state::next,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun MeloXTransportIconButtonV2(
    glyph: MeloXGlyphKind,
    visualSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MeloXGlyph(
            glyph = glyph,
            modifier = Modifier.size(visualSize),
            color = Color.White.copy(alpha = if (enabled) 1f else 0.28f),
        )
    }
}

@Composable
private fun MeloXIOSVolumeControlV2(state: MeloXPlaybackUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeloXGlyph(
            glyph = MeloXGlyphKind.SpeakerLow,
            modifier = Modifier.size(13.dp),
            color = Color.White.copy(alpha = 0.62f),
        )

        Slider(
            value = state.volume,
            onValueChange = state::changeVolume,
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.volume.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.78f)),
                    )
                }
            },
        )

        MeloXGlyph(
            glyph = MeloXGlyphKind.SpeakerHigh,
            modifier = Modifier.size(15.dp),
            color = Color.White.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun MeloXIOSPageSelectorV2(
    state: MeloXPlaybackUiState,
    page: MeloXNowPlayingPage,
    onPageSelected: (MeloXNowPlayingPage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeloXPageIconButtonV2(
            glyph = MeloXGlyphKind.Lyrics,
            selected = page == MeloXNowPlayingPage.Lyrics,
            enabled = true,
            onClick = { onPageSelected(MeloXNowPlayingPage.Lyrics) },
        )

        MeloXPageIconButtonV2(
            glyph = MeloXGlyphKind.Floating,
            selected = false,
            enabled = false,
            onClick = {},
        )

        Box {
            MeloXPageIconButtonV2(
                glyph = MeloXGlyphKind.Queue,
                selected = page == MeloXNowPlayingPage.Queue,
                enabled = true,
                onClick = { onPageSelected(MeloXNowPlayingPage.Queue) },
            )

            if (
                page != MeloXNowPlayingPage.Queue &&
                (state.shuffleEnabled || state.repeatMode != Player.REPEAT_MODE_OFF)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            state.shuffleEnabled -> "↝"
                            state.repeatMode == Player.REPEAT_MODE_ONE -> "1"
                            else -> "↻"
                        },
                        color = Color.Black.copy(alpha = 0.74f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MeloXPageIconButtonV2(
    glyph: MeloXGlyphKind,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.68f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MeloXGlyph(
            glyph = glyph,
            modifier = Modifier.size(22.dp),
            color = when {
                !enabled -> Color.White.copy(alpha = 0.26f)
                selected -> Color.Black.copy(alpha = 0.68f)
                else -> Color.White.copy(alpha = 0.72f)
            },
        )
    }
}

private enum class MeloXGlyphKind {
    Backward,
    Forward,
    Play,
    Pause,
    SpeakerLow,
    SpeakerHigh,
    Lyrics,
    Floating,
    Queue,
    Waveform,
}

@Composable
private fun MeloXGlyph(
    glyph: MeloXGlyphKind,
    modifier: Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = (w.coerceAtMost(h) * 0.085f).coerceAtLeast(1.5f)

        when (glyph) {
            MeloXGlyphKind.Backward,
            MeloXGlyphKind.Forward -> {
                val forward = glyph == MeloXGlyphKind.Forward
                fun triangle(left: Float, right: Float) {
                    val p = Path()
                    if (forward) {
                        p.moveTo(left, h * 0.16f)
                        p.lineTo(right, h * 0.50f)
                        p.lineTo(left, h * 0.84f)
                    } else {
                        p.moveTo(right, h * 0.16f)
                        p.lineTo(left, h * 0.50f)
                        p.lineTo(right, h * 0.84f)
                    }
                    p.close()
                    drawPath(p, color)
                }
                triangle(w * 0.08f, w * 0.50f)
                triangle(w * 0.44f, w * 0.92f)
            }

            MeloXGlyphKind.Play -> {
                val p = Path().apply {
                    moveTo(w * 0.26f, h * 0.12f)
                    lineTo(w * 0.82f, h * 0.50f)
                    lineTo(w * 0.26f, h * 0.88f)
                    close()
                }
                drawPath(p, color)
            }

            MeloXGlyphKind.Pause -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.10f),
                    size = Size(w * 0.20f, h * 0.80f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.58f, h * 0.10f),
                    size = Size(w * 0.20f, h * 0.80f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                )
            }

            MeloXGlyphKind.SpeakerLow,
            MeloXGlyphKind.SpeakerHigh -> {
                val speaker = Path().apply {
                    moveTo(w * 0.08f, h * 0.40f)
                    lineTo(w * 0.30f, h * 0.40f)
                    lineTo(w * 0.54f, h * 0.20f)
                    lineTo(w * 0.54f, h * 0.80f)
                    lineTo(w * 0.30f, h * 0.60f)
                    lineTo(w * 0.08f, h * 0.60f)
                    close()
                }
                drawPath(speaker, color)

                if (glyph == MeloXGlyphKind.SpeakerHigh) {
                    drawArc(
                        color = color,
                        startAngle = -52f,
                        sweepAngle = 104f,
                        useCenter = false,
                        topLeft = Offset(w * 0.40f, h * 0.29f),
                        size = Size(w * 0.34f, h * 0.42f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = color,
                        startAngle = -52f,
                        sweepAngle = 104f,
                        useCenter = false,
                        topLeft = Offset(w * 0.39f, h * 0.14f),
                        size = Size(w * 0.56f, h * 0.72f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }

            MeloXGlyphKind.Lyrics -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.08f, h * 0.10f),
                    size = Size(w * 0.84f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val tail = Path().apply {
                    moveTo(w * 0.62f, h * 0.76f)
                    lineTo(w * 0.52f, h * 0.92f)
                    lineTo(w * 0.72f, h * 0.78f)
                }
                drawPath(tail, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawLine(color, Offset(w * 0.31f, h * 0.35f), Offset(w * 0.31f, h * 0.49f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w * 0.58f, h * 0.35f), Offset(w * 0.58f, h * 0.49f), stroke, StrokeCap.Round)
            }

            MeloXGlyphKind.Floating -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.18f),
                    size = Size(w * 0.72f, h * 0.58f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawLine(color, Offset(w * 0.31f, h * 0.40f), Offset(w * 0.69f, h * 0.40f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w * 0.31f, h * 0.55f), Offset(w * 0.58f, h * 0.55f), stroke, StrokeCap.Round)
            }

            MeloXGlyphKind.Queue -> {
                val ys = listOf(0.28f, 0.50f, 0.72f)
                ys.forEach { y ->
                    drawCircle(color, radius = stroke * 0.72f, center = Offset(w * 0.18f, h * y))
                    drawLine(color, Offset(w * 0.34f, h * y), Offset(w * 0.86f, h * y), stroke, StrokeCap.Round)
                }
            }

            MeloXGlyphKind.Waveform -> {
                val xs = listOf(0.18f, 0.38f, 0.60f, 0.82f)
                val heights = listOf(0.40f, 0.72f, 0.58f, 0.34f)
                xs.zip(heights).forEach { (x, barHeight) ->
                    val half = h * barHeight * 0.5f
                    drawLine(
                        color = color,
                        start = Offset(w * x, h * 0.5f - half),
                        end = Offset(w * x, h * 0.5f + half),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

private fun formatDurationV2(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val minutes = seconds / 60L
    val remainder = seconds % 60L
    return "%d:%02d".format(minutes, remainder)
}
