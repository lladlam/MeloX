package com.lladlam.melox.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lladlam.melox.core.account.rememberNeteaseSessionStore
import com.lladlam.melox.ui.account.NeteaseLoginScreen
import com.lladlam.melox.ui.library.LibraryScreen
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
    var loginReturnTab by remember { mutableStateOf(AppTab.Settings) }
    var tabBarMinimized by remember { mutableStateOf(false) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    val playbackState = rememberMeloXPlaybackUiState()
    val neteaseSession = rememberNeteaseSessionStore()

    val tabBarMinimizeConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                if (available.y < 0f) {
                    if (scrollAccumulator > 0f) scrollAccumulator = 0f
                    scrollAccumulator += available.y
                    if (scrollAccumulator <= -18f) {
                        tabBarMinimized = true
                        scrollAccumulator = 0f
                    }
                } else if (available.y > 0f) {
                    if (scrollAccumulator < 0f) scrollAccumulator = 0f
                    scrollAccumulator += available.y
                    if (scrollAccumulator >= 18f) {
                        tabBarMinimized = false
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

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

    LaunchedEffect(selectedTab) {
        // MeloX / iOS 26 expands the floating tab chrome when switching roots.
        // The same progress then runs backwards if the user taps a tab while the
        // bar is midway through its compact transition.
        tabBarMinimized = false
        scrollAccumulator = 0f
    }

    BackHandler(enabled = showNowPlaying && !showNeteaseLogin) {
        showNowPlaying = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            val sharedScope = this
            val fullPlayerVisible = showNowPlaying && playbackState.hasMedia

            // The app content intentionally occupies the full screen. The bottom
            // chrome is no longer Scaffold.bottomBar: it floats above the content,
            // so rows remain visible through the translucent glass surfaces.
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(tabBarMinimizeConnection),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background,
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
                        AppTab.Library -> LibraryScreen(
                            session = neteaseSession,
                            onLogin = {
                                loginReturnTab = AppTab.Library
                                showNeteaseLogin = true
                            },
                        )
                        AppTab.Settings -> SettingsScreen(
                            session = neteaseSession,
                            onLogin = {
                                loginReturnTab = AppTab.Settings
                                showNeteaseLogin = true
                            },
                        )
                    }
                }
            }

            MeloXBottomChrome(
                selectedTab = selectedTab,
                onSelect = { tab ->
                    tabBarMinimized = false
                    selectedTab = tab
                },
                hasMedia = playbackState.hasMedia,
                minimized = tabBarMinimized,
                modifier = Modifier.align(Alignment.BottomCenter),
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

            // Transparent hit-test barrier: pages stay mounted for the morph but
            // never receive pointer input while the full player is on top.
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
                    selectedTab = loginReturnTab
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
    minimized: Boolean,
    modifier: Modifier = Modifier,
    miniPlayer: @Composable () -> Unit,
) {
    val rawProgress by animateFloatAsState(
        targetValue = if (minimized) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.90f,
            stiffness = 330f,
            visibilityThreshold = 0.001f,
        ),
        label = "melox-tab-minimize-progress",
    )
    val progress = rawProgress.coerceIn(0f, 1f)

    // Stage A: labels disappear and the two chrome heights tighten first.
    val labelStage = smoothStep(progress, 0.00f, 0.32f)
    val sizeStage = smoothStep(progress, 0.00f, 0.36f)
    // Stage B: left tab capsule contracts toward its anchored left edge while
    // the mini player moves into the center slot and narrows at the same time.
    val shrinkStage = smoothStep(progress, 0.25f, 0.82f)
    // Stage C: only after the horizontal morph has almost finished does the
    // entire cluster descend toward the navigation bar.
    val dropStage = smoothStep(progress, 0.78f, 1.00f)

    val navHeight = lerpDp(66.dp, 58.dp, sizeStage)
    val searchSize = lerpDp(66.dp, 58.dp, sizeStage)
    val expandedChromeHeight = if (hasMedia) 137.dp else 72.dp
    val chromeHeight = lerpDp(expandedChromeHeight, 64.dp, dropStage)
    val labelAlpha = 1f - labelStage
    val expandedLayerAlpha = 1f - smoothStep(progress, 0.43f, 0.72f)
    val compactLayerAlpha = smoothStep(progress, 0.52f, 0.82f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 5.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(chromeHeight),
        ) {
            val horizontalMargin = 14.dp
            val compactSize = 58.dp
            val expandedGap = 9.dp
            val compactGap = 4.dp
            val expandedNavWidth = maxWidth - horizontalMargin * 2 - expandedGap - 66.dp
            val navWidth = lerpDp(expandedNavWidth, compactSize, shrinkStage)
            val navRadius = lerpDp(34.dp, 29.dp, shrinkStage)

            val desiredCompactMiniVisibleWidth =
                (maxWidth - horizontalMargin * 2 - compactSize * 2 - compactGap * 2)
                    .coerceAtLeast(80.dp)
            // MeloXIOSMiniPlayer owns 14 dp of horizontal padding on each side.
            // Expand the wrapper by 28 dp so the visible glass surface lands
            // exactly between the two 58 dp compact circles.
            val compactMiniWrapperWidth =
                (desiredCompactMiniVisibleWidth + 28.dp).coerceAtMost(maxWidth)
            val compactMiniWrapperX = horizontalMargin + compactSize + compactGap - 14.dp
            val miniWrapperWidth = lerpDp(maxWidth, compactMiniWrapperWidth, shrinkStage)
            val miniWrapperX = lerpDp(0.dp, compactMiniWrapperX, shrinkStage)
            val miniLift = lerpDp(72.dp, 0.dp, shrinkStage)

            if (hasMedia) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = miniWrapperX,
                            y = -3.dp - miniLift,
                        )
                        .width(miniWrapperWidth),
                ) {
                    miniPlayer()
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = horizontalMargin, y = -3.dp)
                    .width(navWidth)
                    .height(navHeight),
                shape = RoundedCornerShape(navRadius),
                color = bottomGlassColor(),
                border = BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                ),
                shadowElevation = lerpDp(7.dp, 10.dp, progress),
                tonalElevation = 0.dp,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = expandedLayerAlpha }
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
                                labelAlpha = labelAlpha,
                                onClick = { onSelect(tab) },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = compactLayerAlpha }
                            .clickable { onSelect(selectedTab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        RootGlyphIcon(
                            glyph = selectedTab.rootGlyph(),
                            modifier = Modifier.size(27.dp),
                            color = MeloXAccent,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -horizontalMargin, y = -3.dp)
                    .size(searchSize)
                    .clickable { onSelect(AppTab.Search) },
                shape = CircleShape,
                color = bottomGlassColor(),
                border = BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                ),
                shadowElevation = lerpDp(7.dp, 10.dp, progress),
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RootGlyphIcon(
                        glyph = RootGlyph.Search,
                        modifier = Modifier.size(lerpDp(31.dp, 29.dp, sizeStage)),
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
private fun bottomGlassColor(): Color {
    // This is deliberately much more translucent than the old Scaffold bottomBar.
    // Because the chrome is now an overlay, list artwork/text remains visible
    // through this layer and reads like floating glass instead of an opaque footer.
    return MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
}

@Composable
private fun RootTabButton(
    tab: AppTab,
    glyph: RootGlyph,
    selected: Boolean,
    labelAlpha: Float,
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
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RootGlyphIcon(glyph = glyph, modifier = Modifier.size(26.dp), color = foreground)
        Text(
            text = tab.title,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha },
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = foreground,
        )
    }
}

private enum class RootGlyph { Home, Explore, Library, Settings, Search }

private fun AppTab.rootGlyph(): RootGlyph = when (this) {
    AppTab.Home -> RootGlyph.Home
    AppTab.Explore -> RootGlyph.Explore
    AppTab.Library -> RootGlyph.Library
    AppTab.Settings -> RootGlyph.Settings
    AppTab.Search -> RootGlyph.Search
}

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

private fun smoothStep(value: Float, start: Float, end: Float): Float {
    if (end <= start) return if (value >= end) 1f else 0f
    val t = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerpDp(start: Dp, end: Dp, progress: Float): Dp =
    (start.value + (end.value - start.value) * progress.coerceIn(0f, 1f)).dp
