package com.lladlam.melox.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    // The expansion surface and the final player use the same MeloX palette.
    // Cross-fade the two layers rather than stacking them at full opacity, or
    // the palette becomes noticeably darker in the last third of the morph.
    val expansionBackdropAlpha = backdropAlpha * (1f - fullPlayerAlpha)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = expansionBackdropAlpha },
        ) {
            MeloXFlowingLightBackdrop(
                artworkUrl = state.artworkUrl,
                isPlaying = state.isPlaying,
            )
        }

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

        SharedArtworkDestination(
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
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
