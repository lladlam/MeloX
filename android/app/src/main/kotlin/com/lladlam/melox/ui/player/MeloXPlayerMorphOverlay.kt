package com.lladlam.melox.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Root player opening/closing overlay.
 *
 * Unlike the old SharedTransition/AnimatedContent implementation, this keeps the
 * app page mounted at all times and animates one overlay from the measured mini
 * player bounds to the full window. That means there is never a second page or
 * target Surface that can flash white/black while a transition is interrupted.
 */
@Composable
fun MeloXPlayerMorphOverlay(
    state: MeloXPlaybackUiState,
    progress: Float,
    sourceContainerBounds: Rect?,
    sourceArtworkBounds: Rect?,
    onDismiss: () -> Unit,
) {
    if (!state.hasMedia) return

    val p = progress.coerceIn(0f, 1f)
    if (p <= 0.0001f && sourceContainerBounds == null) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val screenRect = Rect(0f, 0f, screenWidthPx, screenHeightPx)

        val fallbackContainer = with(density) {
            Rect(
                14.dp.toPx(),
                screenHeightPx - 146.dp.toPx(),
                screenWidthPx - 80.dp.toPx(),
                screenHeightPx - 88.dp.toPx(),
            )
        }
        val sourceContainer = sourceContainerBounds ?: fallbackContainer

        val targetArtwork = calculateTargetArtworkRect(
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            state = state,
        )
        val sourceArtwork = sourceArtworkBounds ?: with(density) {
            Rect(
                sourceContainer.left + 9.dp.toPx(),
                sourceContainer.top + 6.dp.toPx(),
                sourceContainer.left + 49.dp.toPx(),
                sourceContainer.top + 46.dp.toPx(),
            )
        }

        val containerRect = lerpRect(sourceContainer, screenRect, p)
        val artworkRect = lerpRect(sourceArtwork, targetArtwork, p)
        val containerHandoff = smoothStep(p, 0f, 0.055f)
        val backdropAlpha = smoothStep(p, 0.06f, 0.74f)
        val fullPlayerAlpha = smoothStep(p, 0.955f, 1f)
        val morphChromeAlpha = smoothStep(p, 0.63f, 0.84f) * (1f - smoothStep(p, 0.94f, 1f))
        val movingArtworkAlpha = containerHandoff * (1f - fullPlayerAlpha)
        val corner = with(density) { (22.dp.toPx() * (1f - smoothStep(p, 0f, 0.82f))).toDp() }

        // Consume the screen while the morph exists so taps do not leak to the
        // page underneath. At p=0 this composable is removed by the root.
        Box(Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(containerRect.left.roundToInt(), containerRect.top.roundToInt())
                }
                .size(
                    width = with(density) { containerRect.width.toDp() },
                    height = with(density) { containerRect.height.toDp() },
                )
                .graphicsLayer { alpha = containerHandoff }
                .shadow(
                    elevation = (5f * (1f - p)).dp,
                    shape = RoundedCornerShape(corner),
                    clip = false,
                )
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * (1f - p)),
                    shape = RoundedCornerShape(corner),
                ),
        ) {
            MorphBackdrop(
                artworkUrl = state.artworkUrl,
                alpha = backdropAlpha,
            )
        }

        // One moving cover for the whole morph. It is handed off only during the
        // last few percent, when it is already at the exact final geometry.
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(artworkRect.left.roundToInt(), artworkRect.top.roundToInt())
                }
                .size(
                    width = with(density) { artworkRect.width.toDp() },
                    height = with(density) { artworkRect.height.toDp() },
                )
                .graphicsLayer { alpha = movingArtworkAlpha }
                .clip(
                    RoundedCornerShape(
                        with(density) {
                            (lerp(10.dp.toPx(), 12.dp.toPx(), p)).toDp()
                        },
                    ),
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!state.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = "专辑封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // During the middle phase only lightweight fixed-position chrome is
        // drawn. The real player is not mounted until the container is already
        // effectively full screen, which removes the old page/surface flash.
        if (morphChromeAlpha > 0.001f) {
            MorphChrome(
                state = state,
                alpha = morphChromeAlpha,
                targetArtwork = targetArtwork,
            )
        }

        // Final handoff to the real player happens at ~96-100%. Because its
        // backdrop now fades in only when the overlay is already full-screen,
        // there is no giant target card during open/close or interruption.
        if (fullPlayerAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = fullPlayerAlpha },
            ) {
                MeloXIOSNowPlayingV2(
                    state = state,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun MorphBackdrop(
    artworkUrl: String?,
    alpha: Float,
) {
    Box(Modifier.fillMaxSize()) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = 1.30f
                        scaleY = 1.30f
                    }
                    .blur(
                        radius = 46.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.10f * alpha)),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f * alpha),
                            Color.Black.copy(alpha = 0.46f * alpha),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun MorphChrome(
    state: MeloXPlaybackUiState,
    alpha: Float,
    targetArtwork: Rect,
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha },
    ) {
        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        with(density) { 32.dp.toPx().roundToInt() },
                        (targetArtwork.bottom + with(density) { 22.dp.toPx() }).roundToInt(),
                    )
                }
                .fillMaxWidth()
                .padding(end = 64.dp),
        ) {
            Text(
                text = state.title.ifBlank { "正在播放" },
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist,
                color = Color.White.copy(alpha = 0.64f),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(279.dp)
                .padding(horizontal = 32.dp),
        ) {
            val fraction = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.96f)),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatDurationMorph(state.positionMs),
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "标准",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "−${formatDurationMorph((state.durationMs - state.positionMs).coerceAtLeast(0L))}",
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(19.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MorphTransportGlyph(MorphGlyph.Backward, 34.dp)
                MorphTransportGlyph(if (state.isPlaying) MorphGlyph.Pause else MorphGlyph.Play, 48.dp)
                MorphTransportGlyph(MorphGlyph.Forward, 34.dp)
            }

            Spacer(Modifier.height(31.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("◀", color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.82f)),
                )
                Text("◖", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            }
        }
    }
}

private enum class MorphGlyph { Backward, Play, Pause, Forward }

@Composable
private fun MorphTransportGlyph(kind: MorphGlyph, sizeDp: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(sizeDp)) {
        val c = Color.White.copy(alpha = 0.95f)
        when (kind) {
            MorphGlyph.Play -> {
                val p = Path().apply {
                    moveTo(size.width * 0.23f, size.height * 0.12f)
                    lineTo(size.width * 0.84f, size.height * 0.50f)
                    lineTo(size.width * 0.23f, size.height * 0.88f)
                    close()
                }
                drawPath(p, c)
            }
            MorphGlyph.Pause -> {
                drawRoundRect(
                    color = c,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.10f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.19f, size.height * 0.80f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.04f),
                )
                drawRoundRect(
                    color = c,
                    topLeft = Offset(size.width * 0.59f, size.height * 0.10f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.19f, size.height * 0.80f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.04f),
                )
            }
            MorphGlyph.Backward, MorphGlyph.Forward -> {
                val mirror = if (kind == MorphGlyph.Backward) -1f else 1f
                val center = size.width / 2f
                fun x(raw: Float): Float = center + (raw - center) * mirror
                val a = Path().apply {
                    moveTo(x(size.width * 0.10f), size.height * 0.50f)
                    lineTo(x(size.width * 0.48f), size.height * 0.16f)
                    lineTo(x(size.width * 0.48f), size.height * 0.84f)
                    close()
                }
                val b = Path().apply {
                    moveTo(x(size.width * 0.44f), size.height * 0.50f)
                    lineTo(x(size.width * 0.82f), size.height * 0.16f)
                    lineTo(x(size.width * 0.82f), size.height * 0.84f)
                    close()
                }
                drawPath(a, c)
                drawPath(b, c)
            }
        }
    }
}

@Composable
private fun calculateTargetArtworkRect(
    screenWidthPx: Float,
    screenHeightPx: Float,
    state: MeloXPlaybackUiState,
): Rect {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density).toFloat()
    val horizontalPadding = with(density) { 32.dp.toPx() }
    val grabber = with(density) { 30.dp.toPx() }
    val controls = with(density) { 279.dp.toPx() }
    val titleBlock = with(density) { 50.dp.toPx() }
    val artworkGap = with(density) { 22.dp.toPx() }
    val minimum = with(density) { 170.dp.toPx() }
    val extraWidth = with(density) { 16.dp.toPx() }
    val heightReserve = with(density) { 92.dp.toPx() }

    val pageWidth = screenWidthPx - horizontalPadding * 2f
    val pageHeight = (screenHeightPx - statusTop - grabber - controls).coerceAtLeast(minimum)
    val baseSize = max(minimum, min(pageWidth + extraWidth, pageHeight - heightReserve))
    val scale = if (state.isPlaying) 1f else 0.74f
    val size = baseSize * scale

    val contentBlock = baseSize + artworkGap + titleBlock
    val baseTop = statusTop + grabber + ((pageHeight - contentBlock) / 2f).coerceAtLeast(0f)
    val baseLeft = (screenWidthPx - baseSize) / 2f
    val centerX = baseLeft + baseSize / 2f
    val centerY = baseTop + baseSize / 2f

    return Rect(
        centerX - size / 2f,
        centerY - size / 2f,
        centerX + size / 2f,
        centerY + size / 2f,
    )
}

private fun lerpRect(a: Rect, b: Rect, t: Float): Rect = Rect(
    lerp(a.left, b.left, t),
    lerp(a.top, b.top, t),
    lerp(a.right, b.right, t),
    lerp(a.bottom, b.bottom, t),
)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun smoothStep(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    val x = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun formatDurationMorph(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
