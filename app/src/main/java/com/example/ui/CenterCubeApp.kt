package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.AlgorithmsScreen
import com.example.ui.screens.InputScreen
import com.example.ui.screens.SolveScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterCubeApp(
    viewModel: CenterCubeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(context) {
        viewModel.attachAndroidVoiceNarrator(context)
    }

    BackHandler(enabled = state.activeTab != AppTab.SOLVE) {
        viewModel.selectTab(AppTab.SOLVE)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isWide = maxWidth >= 600.dp

        val navItems = listOf(
            Triple(AppTab.SOLVE, stringResource(R.string.tab_solve), Pair(Icons.Rounded.ViewInAr, Icons.Outlined.ViewInAr)),
            Triple(AppTab.INPUT, stringResource(R.string.tab_input), Pair(Icons.Rounded.GridView, Icons.Outlined.GridView)),
            Triple(AppTab.ALGORITHMS, stringResource(R.string.tab_algorithms), Pair(Icons.Rounded.AutoStories, Icons.Outlined.AutoStories)),
            Triple(AppTab.STATS, stringResource(R.string.tab_stats), Pair(Icons.Rounded.BarChart, Icons.Outlined.BarChart))
        )

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianBg,
                        titleContentColor = TextPrimary
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanContainer)
                                    .border(1.dp, CyanReticle.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CenterFocusStrong,
                                    contentDescription = "CenterCube",
                                    tint = CyanReticle,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val subtitle = state.validProfile?.orientationLabel ?: "Check Center Boxes"
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanReticle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleVoiceGuide() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("voice_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (state.isVoiceEnabled) {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                },
                                contentDescription = if (state.isVoiceEnabled) {
                                    stringResource(R.string.voice_guide_on)
                                } else {
                                    stringResource(R.string.voice_guide_off)
                                },
                                tint = if (state.isVoiceEnabled) CyanReticle else TextSecondary
                            )
                        }
                        state.validProfile?.let { profile ->
                            Row(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .background(CarbonSurface, RoundedCornerShape(10.dp))
                                    .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(profile.topCenter.composeColor)
                                )
                                Text(
                                    text = "U:${profile.topCenter.shortCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(profile.frontCenter.composeColor)
                                )
                                Text(
                                    text = "F:${profile.frontCenter.shortCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWide) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = state.activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanReticle,
                                    selectedTextColor = CyanReticle,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    NavigationRail(
                        containerColor = CarbonSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = state.activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanReticle,
                                    selectedTextColor = CyanReticle,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedVisibility(visible = state.statusBanner != null) {
                        state.statusBanner?.let { banner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .background(CarbonElevated, RoundedCornerShape(10.dp))
                                    .border(1.dp, CyanReticle.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = banner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanReticle
                                )
                            }
                        }
                    }

                    when (state.activeTab) {
                        AppTab.SOLVE -> SolveScreen(
                            state = state,
                            isWideLayout = isWide,
                            onScramble = { viewModel.scramble() },
                            onReset = { viewModel.resetSolved() },
                            onRotateCube = { axis -> viewModel.rotateWholeCube(axis) },
                            onManualMove = { move -> viewModel.applyManualMove(move) },
                            onStepForward = { viewModel.stepForward() },
                            onStepBackward = { viewModel.stepBackward() },
                            onTogglePlay = { viewModel.togglePlay() },
                            onCycleSpeed = { viewModel.cycleSpeed() },
                            onJumpToStep = { idx -> viewModel.jumpToStep(idx) },
                            onOpenInput = { viewModel.selectTab(AppTab.INPUT) },
                            onToggleMoveArrows = { viewModel.toggleMoveArrows() },
                            onUndo = { viewModel.undoLastMove() },
                            onToggleVoice = { viewModel.toggleVoiceGuide() },
                            onSpeakCurrentStep = { viewModel.speakCurrentStep(force = true) }
                        )
                        AppTab.INPUT -> InputScreen(
                            state = state,
                            onSelectSubMode = { mode -> viewModel.selectInputSubMode(mode) },
                            onSelectColor = { color -> viewModel.selectPaintColor(color) },
                            onPaintSticker = { face, idx -> viewModel.paintSticker(face, idx) },
                            onCaptureFace = { face, stickers -> viewModel.applyScannedFace(face, stickers) },
                            onApplyPreset = { cube, msg -> viewModel.applyInputPreset(cube, msg) },
                            onSolveNow = { viewModel.solveAndJumpToSolveTab() }
                        )
                        AppTab.ALGORITHMS -> AlgorithmsScreen(
                            onTryAlgorithm = { item -> viewModel.loadAlgorithmDemo(item) }
                        )
                        AppTab.STATS -> StatsScreen(
                            stats = state.stats,
                            isWideLayout = isWide,
                            onResetStats = { viewModel.resetSessionStats() },
                            onGoToSolve = { viewModel.selectTab(AppTab.SOLVE) }
                        )
                    }
                }
            }
        }
    }
}
