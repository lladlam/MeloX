package com.lladlam.melox.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SHARED_ARTWORK_DURATION_MS = 430

/**
 * Hosts the full player and provides the destination geometry for the artwork
 * shared with the mini player. The real player remains responsible for all
 * playback state and internal page transitions; this overlay is only visible
 * while the root shared transition is active.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MeloXIOSNowPlayingSharedHost(
    state: MeloXPlaybackUiState,
    onDismiss: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Box(Modifier.fillMaxSize()) {
        MeloXIOSNowPlayingV2(
            state = state,
            onDismiss = onDismiss,
        )

        // Keep the destination mounted at all times so SharedTransitionScope
        // can match it on the very first frame. It becomes transparent once
        // the transition finishes and hands rendering back to the real player.
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
        // Same grabber slot as MeloXIOSNowPlayingV2.
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
                        // Clip AFTER sharedElement so the image is promoted to
                        // the transition overlay before local clipping occurs.
                        .clip(RoundedCornerShape(12.dp)),
                )

                Spacer(Modifier.height(22.dp))

                // Invisible geometry twins of the title/artist block. Their
                // measured height keeps this target aligned with the artwork
                // page underneath without drawing duplicate text.
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

        // Same fixed control layer height as the iOS-derived player.
        Spacer(Modifier.height(279.dp))
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
