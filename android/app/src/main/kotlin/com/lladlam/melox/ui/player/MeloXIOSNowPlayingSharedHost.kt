package com.lladlam.melox.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

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
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        // The artwork-derived background starts appearing while the capsule is
        // still expanding. The same alpha is traversed backwards when closing.
        MeloXExpansionBackdrop(
            artworkUrl = state.artworkUrl,
            alpha = backdropAlpha,
        )

        // Full controls stay at their final positions. They only become visible
        // after the container/artwork has covered roughly two thirds of the
        // journey. On close this exact mapping runs in reverse.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = fullPlayerAlpha
                },
        ) {
            MeloXIOSNowPlayingV2(
                state = state,
                onDismiss = onDismiss,
            )
        }

        // Artwork remains a true shared element inside the transforming
        // container. It is not faded together with mini-player chrome.
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black),
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
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
                .background(Color.Black.copy(alpha = 0.09f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.01f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.44f),
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

                val sharedModifier = with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = sharedArtworkKey(state.mediaId),
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }

                Artwork(
                    url = state.artworkUrl,
                    modifier = sharedModifier
                        .graphicsLayer {
                            alpha = if (transitionActive) 1f else 0f
                        }
                        .size(artworkSize)
                        .clip(RoundedCornerShape(12.dp)),
                )

                Spacer(Modifier.height(22.dp))

                // Geometry twins only; they keep the shared artwork's final
                // destination identical to the real artwork page below it.
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = state.title.ifBlank { "正在播放" },
                        color = Color.Transparent,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = state.artist.ifBlank { " " },
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
