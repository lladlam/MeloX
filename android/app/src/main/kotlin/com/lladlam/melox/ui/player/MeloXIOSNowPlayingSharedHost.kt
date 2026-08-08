package com.lladlam.melox.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage

/**
 * Apple-Music-style container transform driven by one reversible progress.
 *
 * 0f = mini player
 * 1f = full player
 *
 * Opening and closing use the same AnimatedVisibility transition. If the
 * target changes while the spring is still moving, Compose retargets from the
 * current value and velocity instead of restarting a fixed-duration script.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MeloXIOSNowPlayingSharedHost(
    state: MeloXPlaybackUiState,
    onDismiss: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val expansionProgress by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = 0.90f,
                stiffness = 320f,
                visibilityThreshold = 0.001f,
            )
        },
        label = "full-player-expansion-progress",
    ) { visibility ->
        if (visibility == EnterExitState.Visible) 1f else 0f
    }

    val transitionActive = with(sharedTransitionScope) { isTransitionActive }
    val backdropAlpha = smoothStep(expansionProgress, 0.06f, 0.74f)
    val fullPlayerAlpha = smoothStep(expansionProgress, 0.66f, 0.98f)
    val cornerRadius = (22f * (1f - smoothStep(expansionProgress, 0f, 0.82f))).dp
    val baseSurfaceAlpha = 0.82f + (0.18f * smoothStep(expansionProgress, 0.08f, 0.72f))
    val baseSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = baseSurfaceAlpha)

    val sharedContainerModifier = with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(
                key = sharedPlayerContainerKey(state.mediaId),
            ),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
        )
    }

    Box(
        modifier = sharedContainerModifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            // IMPORTANT: do not use Color.Black here. sharedBounds keeps both
            // source and target content visible during the morph; a black target
            // therefore becomes the giant black rectangle seen in light mode.
            // Start from the same theme surface as the mini player and let the
            // artwork field gradually cover it.
            .background(baseSurfaceColor),
    ) {
        MeloXExpansionBackdrop(
            artworkUrl = state.artworkUrl,
            alpha = backdropAlpha,
        )

        // Full player chrome starts appearing only after the container/artwork
        // have already travelled most of the way. On close the same progress is
        // traversed backwards, so controls disappear before the card collapses.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = fullPlayerAlpha
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        ) {
            MeloXIOSNowPlayingV2(
                state = state,
                onDismiss = onDismiss,
            )

            if (transitionActive) {
                SharedArtworkCutout(state)
            }
        }

        // Exactly one moving cover is visible during the root transition.
        SharedArtworkDestination(
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@Composable
private fun MeloXExpansionBackdrop(
    artworkUrl: String?,
    alpha: Float,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = 1.32f
                        scaleY = 1.32f
                    }
                    .blur(
                        radius = 48.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }

        // These are readability veils only. They fade in with the artwork and
        // are never an opaque black transform layer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.07f * alpha)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.00f),
                            Color.Black.copy(alpha = 0.05f * alpha),
                            Color.Black.copy(alpha = 0.40f * alpha),
                        ),
                    ),
                ),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedArtworkDestination(
    state: MeloXPlaybackUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val transitionActive = with(sharedTransitionScope) { isTransitionActive }

    ArtworkTargetLayout { artworkSize ->
        val sharedModifier = with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(
                    key = sharedArtworkKey(state.mediaId),
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 10f,
            )
        }

        Box(
            modifier = sharedModifier
                .graphicsLayer {
                    alpha = if (transitionActive) 1f else 0f
                }
                .size(artworkSize)
                .clip(RoundedCornerShape(12.dp))
                // Keep the fallback theme-neutral; never flash a white/black
                // placeholder while Coil resolves the already-cached cover.
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)),
        ) {
            if (!state.artworkUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = state.artworkUrl,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {},
                )
            }
        }
    }
}

/**
 * Clears the stationary artwork that MeloXIOSNowPlayingV2 would otherwise draw
 * underneath the shared moving artwork while its controls are fading in.
 */
@Composable
private fun SharedArtworkCutout(state: MeloXPlaybackUiState) {
    ArtworkTargetLayout { artworkSize ->
        val targetScale = if (state.isPlaying) 1f else 0.74f
        Canvas(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    scaleX = targetScale
                    scaleY = targetScale
                },
        ) {
            drawRoundRect(
                color = Color.Transparent,
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                blendMode = BlendMode.Clear,
            )
        }
    }
}

/**
 * Mirrors the V2 artwork page's final geometry so the shared destination and
 * temporary cutout stay pixel-aligned with the real player artwork.
 */
@Composable
private fun ArtworkTargetLayout(
    content: @Composable (artworkSize: androidx.compose.ui.unit.Dp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(30.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val artworkSize = maxOf(
                170.dp,
                minOf(maxWidth + 16.dp, maxHeight - 92.dp),
            )

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))

                content(artworkSize)

                Spacer(Modifier.height(22.dp))

                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = " ",
                        color = Color.Transparent,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = " ",
                        color = Color.Transparent,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(279.dp))
    }
}

private fun smoothStep(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    val t = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
