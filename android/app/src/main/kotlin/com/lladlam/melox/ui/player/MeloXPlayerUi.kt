package com.lladlam.melox.ui.player

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.compose.AsyncImage
import com.lladlam.melox.playback.MeloXPlaybackService
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Stable
class MeloXPlaybackUiState internal constructor() {
    private var controller: MediaController? = null

    var mediaId by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var artist by mutableStateOf("")
        private set
    var album by mutableStateOf("")
        private set
    var artworkUrl by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    val hasMedia: Boolean
        get() = mediaId != null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
        }
    }

    internal fun bind(newController: MediaController) {
        controller?.removeListener(listener)
        controller = newController
        newController.addListener(listener)
        refresh()
    }

    internal fun unbind() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    internal fun refresh() {
        val player = controller ?: return
        val item = player.currentMediaItem
        val metadata = player.mediaMetadata.takeUnless { it == MediaMetadata.EMPTY }
            ?: item?.mediaMetadata
            ?: MediaMetadata.EMPTY

        mediaId = item?.mediaId
        title = metadata.title?.toString().orEmpty()
        artist = metadata.artist?.toString().orEmpty()
        album = metadata.albumTitle?.toString().orEmpty()
        artworkUrl = metadata.artworkUri?.toString()
        isPlaying = player.isPlaying
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration
            .takeUnless { it == C.TIME_UNSET || it < 0L }
            ?: 0L
    }

    fun togglePlayPause() {
        controller?.let { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)))
    }

    fun seekBack() {
        controller?.seekBack()
    }

    fun seekForward() {
        controller?.seekForward()
    }
}

@Composable
fun rememberMeloXPlaybackUiState(): MeloXPlaybackUiState {
    val context = LocalContext.current.applicationContext
    val state = remember { MeloXPlaybackUiState() }

    DisposableEffect(context) {
        val token = SessionToken(
            context,
            ComponentName(context, MeloXPlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        val handler = Handler(Looper.getMainLooper())
        var disposed = false

        future.addListener(
            {
                if (!disposed) {
                    runCatching { future.get() }
                        .onSuccess(state::bind)
                }
            },
            { command -> handler.post(command) },
        )

        onDispose {
            disposed = true
            if (!future.isDone) future.cancel(true)
            state.unbind()
        }
    }

    LaunchedEffect(state.isPlaying, state.mediaId) {
        while (true) {
            state.refresh()
            delay(if (state.isPlaying) 500L else 1_000L)
        }
    }

    return state
}

@Composable
fun MeloXMiniPlayer(
    state: MeloXPlaybackUiState,
    onExpand: () -> Unit,
) {
    if (!state.hasMedia) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp,
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Artwork(
                url = state.artworkUrl,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp)),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "正在播放" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        state.togglePlayPause()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.isPlaying) "Ⅱ" else "▶",
                    fontSize = if (state.isPlaying) 22.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun MeloXNowPlaying(
    state: MeloXPlaybackUiState,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⌄", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(44.dp))
            }

            Spacer(Modifier.height(34.dp))

            Artwork(
                url = state.artworkUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp)),
            )

            Spacer(Modifier.height(30.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.title.ifBlank { "正在播放" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = state.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }

            Spacer(Modifier.height(24.dp))

            val progress = if (state.durationMs > 0L) {
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Slider(
                value = progress,
                onValueChange = { value ->
                    if (state.durationMs > 0L) {
                        state.seekTo((state.durationMs * value).roundToLong())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(state.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerControlButton("−10", 20.sp) { state.seekBack() }
                PlayerControlButton(
                    if (state.isPlaying) "Ⅱ" else "▶",
                    if (state.isPlaying) 34.sp else 30.sp,
                    emphasized = true,
                ) {
                    state.togglePlayPause()
                }
                PlayerControlButton("+10", 20.sp) { state.seekForward() }
            }

            if (state.album.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = state.album,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                )
            }
        }
    }
}

@Composable
private fun Artwork(
    url: String?,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "专辑封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = "♪",
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (emphasized) 76.dp else 58.dp)
            .clip(CircleShape)
            .background(
                if (emphasized) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    val minutes = seconds / 60L
    val remainder = seconds % 60L
    return "%d:%02d".format(minutes, remainder)
}
