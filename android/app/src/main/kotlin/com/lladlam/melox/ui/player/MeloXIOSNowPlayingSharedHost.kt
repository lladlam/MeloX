package com.lladlam.melox.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private const val PLAYER_CONTAINER_DURATION_MS = 470
private const val SHARED_ARTWORK_DURATION_MS = 470
private const val FULL_CONTROLS_REVEAL_DELAY_MS = 300
private const val FULL_CONTROLS_REVEAL_DURATION_MS = 170

/**
 * Root Apple-Music-style container transform.
 *
 * The mini-player capsule and this full-screen container share bounds, while
 * the artwork inside them is a real shared element. The player chrome is
 * intentionally delayed until the artwork is roughly two thirds of the way
 * to its destination.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MeloXIOSNowPlayingSharedHost(
    state: MeloXPlaybackUiState,
    onDismiss: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    var revealFullPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        revealFullPlayer = true
    }

    val fullPlayerAlpha by animateFloatAsState(
        targetValue = if (revealFullPlayer) 1f else 0f,
        animationSpec = tween(
            durationMillis = FULL_CONTROLS_REVEAL_DURATION_MS,
            delayMillis = FULL_CONTROLS_REVEAL_DELAY_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "full-player-delayed-reveal",
    )

    val sharedContainerModifier = with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(
                key = sharedPlayerContainerKey(state.mediaId),
            ),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = playerContainerBoundsTransform,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = PLAYER_CONTAINER_DURATION_MS,
                    easing = FastOutSlowInEasing,
                ),
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 180),
            ),
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
        )
    }

    Box(
        modifier = sharedContainerModifier.fillMaxSize(),
    ) {
        // This backdrop participates from the beginning of the container
        // transform, so the mini-player surface gradually becomes artwork
        // colour instead of cutting to a full-screen background at the end.
        MeloXExpansionBackdrop(state.artworkUrl)

        // The full player itself stays invisible during the first ~2/3 of the
        // morph. Its controls then fade into their final fixed positions.
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

        // The artwork is independently shared so it travels from the mini
        // player's lower-left corner to the full-player artwork position while
        // the container itself expands behind it.
        SharedArtworkDestination(
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@Composable
private fun MeloXExpansionBackdrop(artworkUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                        boundsTransform = artworkBoundsTransform,
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
                // position aligned with the real artwork page underneath.
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

@OptIn(ExperimentalSharedTransitionApi::class)
private val playerContainerBoundsTransform = BoundsTransform { initialBounds: Rect, targetBounds: Rect ->
    keyframes {
        durationMillis = PLAYER_CONTAINER_DURATION_MS
        initialBounds at 0 using FastOutSlowInEasing
        targetBounds at PLAYER_CONTAINER_DURATION_MS using FastOutSlowInEasing
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val artworkBoundsTransform = BoundsTransform { initialBounds: Rect, targetBounds: Rect ->
    keyframes {
        durationMillis = SHARED_ARTWORK_DURATION_MS
        initialBounds at 0 using FastOutSlowInEasing
        targetBounds at SHARED_ARTWORK_DURATION_MS using FastOutSlowInEasing
    }
}
