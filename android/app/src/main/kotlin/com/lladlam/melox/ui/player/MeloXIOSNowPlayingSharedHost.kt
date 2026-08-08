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
 * Both opening and closing use the same AnimatedVisibility transition. If the
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
    val backdropAlpha = smoothStep(expansionProgress, 0.04f, 0.72f)
    val fullPlayerAlpha = smoothStep(expansionProgress, 0.64f, 0.98f)
    val cornerRadius = (22f * (1f - smoothStep(expansionProgress, 0f, 0.82f))).dp

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
            // Never allow the transforming container to expose a light/default
            // Surface while the artwork-derived backdrop is still fading in.
            .background(Color.Black),
    ) {
        // The coloured/blurred artwork field fades over the stable black base.
        // On close this mapping is traversed in the exact opposite direction.
        MeloXExpansionBackdrop(
            artworkUrl = state.artworkUrl,
            alpha = backdropAlpha,
        )

        // The V2 player can reveal its chrome from ~2/3 progress, but while the
        // root shared transition is active we punch out its own stationary
        // artwork. That leaves exactly one visible cover: the moving shared one.
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

        // This is the only artwork drawn while the root transition is active.
        // Use SubcomposeAsyncImage instead of the generic Artwork() wrapper so
        // a one-frame target-side load cannot expose Artwork's light placeholder.
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.09f * alpha)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.01f * alpha),
                            Color.Black.copy(alpha = 0.08f * alpha),
                            Color.Black.copy(alpha = 0.44f * alpha),
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
                .background(Color.Black.copy(alpha = 0.10f)),
        ) {
            if (!state.artworkUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = state.artworkUrl,
                    contentDescription = "专辑封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        // Source cover is already in Coil's memory cache in the
                        // normal path. If it is not, keep this target dark rather
                        // than briefly exposing the old white Artwork placeholder.
                    },
                    error = {},
                )
            }
        }
    }
}

/**
 * Clears the stationary artwork that MeloXIOSNowPlayingV2 would otherwise draw
 * underneath the shared moving artwork while its controls are fading in.
 * Because the whole V2 layer is rendered offscreen, BlendMode.Clear exposes the
 * host backdrop below without hiding any of the surrounding controls/text.
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
 * the temporary cutout stay pixel-aligned with the real player artwork.
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

                // Invisible geometry twins keep the artwork destination aligned
                // with the real V2 artwork/title block.
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
