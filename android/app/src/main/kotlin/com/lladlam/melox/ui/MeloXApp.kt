package com.lladlam.melox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lladlam.melox.core.account.rememberNeteaseSessionStore
import com.lladlam.melox.ui.account.NeteaseLoginScreen
import com.lladlam.melox.ui.player.MeloXIOSMiniPlayer
import com.lladlam.melox.ui.player.MeloXIOSNowPlayingSharedHost
import com.lladlam.melox.ui.player.rememberMeloXPlaybackUiState
import com.lladlam.melox.ui.search.SearchScreen
import com.lladlam.melox.ui.settings.SettingsScreen

enum class AppTab(val title: String) {
    Home("首页"),
    Explore("发现"),
    Library("音乐库"),
    Settings("设置"),
    Search("搜索"),
}

private val MeloXAccent = Color(0xFFFF3147)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MeloXApp(
    openNowPlayingRequest: Int = 0,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showNeteaseLogin by remember { mutableStateOf(false) }
    val playbackState = rememberMeloXPlaybackUiState()
    val neteaseSession = rememberNeteaseSessionStore()

    LaunchedEffect(openNowPlayingRequest, playbackState.hasMedia) {
        if (openNowPlayingRequest > 0 && playbackState.hasMedia) {
            showNowPlaying = true
        }
    }

    LaunchedEffect(neteaseSession.cookie) {
        if (neteaseSession.isLoggedIn) {
            neteaseSession.refreshProfile()
        }
    }

    BackHandler(enabled = showNowPlaying && !showNeteaseLogin) {
        showNowPlaying = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            val sharedScope = this
            val fullPlayerVisible = showNowPlaying && playbackState.hasMedia

            // Keep the app page mounted while the player is open, but do not let
            // pointer input fall through the full-screen player into the page.
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    MeloXBottomChrome(
                        selectedTab = selectedTab,
                        onSelect = { selectedTab = it },
                        hasMedia = playbackState.hasMedia,
                        miniPlayer = {
                            AnimatedVisibility(
                                visible = !fullPlayerVisible,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                            ) {
                                MeloXIOSMiniPlayer(
                                    state = playbackState,
                                    onExpand = { showNowPlaying = true },
                                    sharedTransitionScope = sharedScope,
                                    animatedVisibilityScope = this,
                                )
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when (selectedTab) {
                        AppTab.Search -> SearchScreen()
                        AppTab.Home -> MeloXSectionShell(
                            "首页",
                            "每日推荐与个性化内容将按 iOS MeloX 结构接入。",
                        )
                        AppTab.Explore -> MeloXSectionShell(
                            "发现",
                            "推荐、排行榜、精品与分类内容正在迁移。",
                        )
                        AppTab.Library -> MeloXSectionShell(
                            "音乐库",
                            "歌曲、歌单与最近播放将在这里接入。",
                        )
                        AppTab.Settings -> SettingsScreen(
                            session = neteaseSession,
                            onLogin = { showNeteaseLogin = true },
                        )
                    }
                }
            }

            if (fullPlayerVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { change -> change.consume() }
                                }
                            }
                        },
                )
            }

            AnimatedVisibility(
                visible = fullPlayerVisible,
                enter = EnterTransition.None,
                exit = ExitTransition.None,
                modifier = Modifier.fillMaxSize(),
            ) {
                MeloXIOSNowPlayingSharedHost(
                    state = playbackState,
                    onDismiss = { showNowPlaying = false },
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = this,
                )
            }
        }

        if (showNeteaseLogin) {
            NeteaseLoginScreen(
                session = neteaseSession,
                onDismiss = { showNeteaseLogin = false },
                onLoggedIn = {
                    showNeteaseLogin = false
                    selectedTab = AppTab.Settings
                },
            )
        }
    }
}

@Composable
private fun MeloXSectionShell(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp),
    ) {
        Text(
            text = title,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
        )
    }
}

@Composable
private fun MeloXBottomChrome(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
    hasMedia: Boolean,
    miniPlayer: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 5.dp),
    ) {
        if (hasMedia) miniPlayer()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(66.dp),
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                border = BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                ),
                shadowElevation = 7.dp,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val primaryTabs = listOf(
                        AppTab.Home to RootGlyph.Home,
                        AppTab.Explore to RootGlyph.Explore,
                        AppTab.Library to RootGlyph.Library,
                        AppTab.Settings to RootGlyph.Settings,
                    )
                    primaryTabs.forEach { (tab, glyph) ->
                        RootTabButton(
                            tab = tab,
                            glyph = glyph,
                            selected = selectedTab == tab,
                            onClick = { onSelect(tab) },
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .size(66.dp)
                    .clickable { onSelect(AppTab.Search) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                border = BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                ),
                shadowElevation = 7.dp,
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RootGlyphIcon(
                        glyph = RootGlyph.Search,
                        modifier = Modifier.size(31.dp),
                        color = if (selectedTab == AppTab.Search) {
                            MeloXAccent
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RootTabButton(
    tab: AppTab,
    glyph: RootGlyph,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val foreground = if (selected) MeloXAccent else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RootGlyphIcon(glyph = glyph, modifier = Modifier.size(26.dp), color = foreground)
        Text(
            text = tab.title,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = foreground,
        )
    }
}

private enum class RootGlyph { Home, Explore, Library, Settings, Search }

@Composable
private fun RootGlyphIcon(
    glyph: RootGlyph,
    modifier: Modifier,
    color: Color,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.115f

        when (glyph) {
            RootGlyph.Home -> {
                val roof = Path().apply {
                    moveTo(w * 0.10f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.90f, h * 0.48f)
                }
                drawPath(roof, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.24f, h * 0.43f),
                    size = Size(w * 0.52f, h * 0.43f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                    style = Stroke(width = stroke),
                )
            }
            RootGlyph.Explore -> {
                drawCircle(color, radius = w * 0.35f, style = Stroke(width = stroke))
                val needle = Path().apply {
                    moveTo(w * 0.38f, h * 0.62f)
                    lineTo(w * 0.57f, h * 0.36f)
                    lineTo(w * 0.63f, h * 0.55f)
                    close()
                }
                drawPath(needle, color)
            }
            RootGlyph.Library -> {
                val p = Path().apply {
                    moveTo(w * 0.26f, h * 0.22f)
                    lineTo(w * 0.26f, h * 0.72f)
                    cubicTo(w * 0.26f, h * 0.83f, w * 0.10f, h * 0.84f, w * 0.10f, h * 0.70f)
                    cubicTo(w * 0.10f, h * 0.57f, w * 0.29f, h * 0.55f, w * 0.37f, h * 0.62f)
                    lineTo(w * 0.37f, h * 0.28f)
                    lineTo(w * 0.83f, h * 0.18f)
                    lineTo(w * 0.83f, h * 0.61f)
                    cubicTo(w * 0.83f, h * 0.74f, w * 0.66f, h * 0.77f, w * 0.61f, h * 0.66f)
                }
                drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            RootGlyph.Settings -> {
                drawCircle(color, radius = w * 0.33f, style = Stroke(width = stroke))
                drawCircle(color, radius = w * 0.095f)
                repeat(8) { index ->
                    val angle = Math.toRadians((index * 45.0) - 90.0)
                    val cx = w / 2f
                    val cy = h / 2f
                    val r1 = w * 0.37f
                    val r2 = w * 0.47f
                    drawLine(
                        color = color,
                        start = Offset(
                            cx + kotlin.math.cos(angle).toFloat() * r1,
                            cy + kotlin.math.sin(angle).toFloat() * r1,
                        ),
                        end = Offset(
                            cx + kotlin.math.cos(angle).toFloat() * r2,
                            cy + kotlin.math.sin(angle).toFloat() * r2,
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
            RootGlyph.Search -> {
                drawCircle(
                    color = color,
                    radius = w * 0.29f,
                    center = Offset(w * 0.43f, h * 0.40f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.64f, h * 0.62f),
                    end = Offset(w * 0.86f, h * 0.84f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
