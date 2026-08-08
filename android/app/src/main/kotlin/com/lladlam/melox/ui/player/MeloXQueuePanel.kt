package com.lladlam.melox.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage

@Composable
fun MeloXQueuePanel(
    state: MeloXPlaybackUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QueueModeButton(
                label = "随机",
                symbol = "⇄",
                selected = state.shuffleEnabled,
                onClick = state::toggleShuffle,
                modifier = Modifier.weight(1f),
            )
            QueueModeButton(
                label = repeatModeLabel(state.repeatMode),
                symbol = if (state.repeatMode == Player.REPEAT_MODE_ONE) "↻1" else "↻",
                selected = state.repeatMode != Player.REPEAT_MODE_OFF,
                onClick = state::cycleRepeatMode,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "继续播放",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
        )

        if (state.upcomingQueue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "没有待播放歌曲",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                items(
                    items = state.upcomingQueue,
                    key = { item -> "${item.mediaId}-${item.queueIndex}" },
                ) { item ->
                    QueueRow(
                        item = item,
                        onClick = { state.playQueueItem(item.queueIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueModeButton(
    label: String,
    symbol: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (selected) 0.16f else 0.07f,
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = symbol,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(7.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun QueueRow(
    item: PlaybackQueueItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "♪",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { "未知歌曲" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

private fun repeatModeLabel(mode: Int): String = when (mode) {
    Player.REPEAT_MODE_ALL -> "列表循环"
    Player.REPEAT_MODE_ONE -> "单曲循环"
    else -> "循环关闭"
}
