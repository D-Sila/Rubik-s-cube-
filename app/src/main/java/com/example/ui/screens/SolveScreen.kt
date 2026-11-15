package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.cube.CenterProfile
import com.example.cube.CubeValidationResult
import com.example.cube.Face
import com.example.cube.Move
import com.example.stats.SessionStatsFormatter
import com.example.stats.SessionStatsState
import com.example.stats.StopwatchState
import com.example.ui.CenterCubeUiState
import com.example.ui.CubeAxis
import com.example.ui.components.InteractiveCube3D
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SolveScreen(
    state: CenterCubeUiState,
    isWideLayout: Boolean,
    onScramble: () -> Unit,
    onReset: () -> Unit,
    onRotateCube: (CubeAxis) -> Unit,
    onManualMove: (Move) -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onTogglePlay: () -> Unit,
    onCycleSpeed: () -> Unit,
    onJumpToStep: (Int) -> Unit,
    onOpenInput: () -> Unit,
    onToggleMoveArrows: () -> Unit,
    onUndo: () -> Unit,
    onToggleVoice: () -> Unit,
    onSpeakCurrentStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isWideLayout) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InteractiveCube3D(
                    cube = state.cube,
                    activeMove = state.activeAnimationMove,
                    moveProgress = state.moveProgress,
                    guideMove = state.nextStep?.move,
                    showMoveArrows = state.showMoveArrows,
                    onToggleMoveArrows = onToggleMoveArrows,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
                SolveStopwatchBar(stats = state.stats)
                SolvePercentageBar(state = state)
                QuickCubeActionsRow(
                    canUndo = state.canUndo,
                    lastRecordedMove = state.lastRecordedMove,
                    isVoiceEnabled = state.isVoiceEnabled,
                    onUndo = onUndo,
                    onToggleVoice = onToggleVoice,
                    onScramble = onScramble,
                    onReset = onReset,
                    onRotateCube = onRotateCube
                )
                ManualTurnBar(
                    canUndo = state.canUndo,
                    lastRecordedMove = state.lastRecordedMove,
                    onUndo = onUndo,
                    onManualMove = onManualMove
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CenterAnalysisCard(
                    validation = state.validation,
                    onOpenInput = onOpenInput
                )
                SolutionPlayerSection(
                    state = state,
                    onStepForward = onStepForward,
                    onStepBackward = onStepBackward,
                    onTogglePlay = onTogglePlay,
                    onCycleSpeed = onCycleSpeed,
                    onJumpToStep = onJumpToStep,
                    onSpeakCurrentStep = onSpeakCurrentStep
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InteractiveCube3D(
                cube = state.cube,
                activeMove = state.activeAnimationMove,
                moveProgress = state.moveProgress,
                guideMove = state.nextStep?.move,
                showMoveArrows = state.showMoveArrows,
                onToggleMoveArrows = onToggleMoveArrows,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(276.dp)
            )

            SolveStopwatchBar(stats = state.stats)

            SolvePercentageBar(state = state)

            QuickCubeActionsRow(
                canUndo = state.canUndo,
                lastRecordedMove = state.lastRecordedMove,
                isVoiceEnabled = state.isVoiceEnabled,
                onUndo = onUndo,
                onToggleVoice = onToggleVoice,
                onScramble = onScramble,
                onReset = onReset,
                onRotateCube = onRotateCube
            )

            CenterAnalysisCard(
                validation = state.validation,
                onOpenInput = onOpenInput
            )

            SolutionPlayerSection(
                state = state,
                onStepForward = onStepForward,
                onStepBackward = onStepBackward,
                onTogglePlay = onTogglePlay,
                onCycleSpeed = onCycleSpeed,
                onJumpToStep = onJumpToStep,
                onSpeakCurrentStep = onSpeakCurrentStep
            )

            ManualTurnBar(
                canUndo = state.canUndo,
                lastRecordedMove = state.lastRecordedMove,
                onUndo = onUndo,
                onManualMove = onManualMove
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SolveStopwatchBar(
    stats: SessionStatsState,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(stats.stopwatchState, stats.activeAttemptStartMs) {
        if (stats.stopwatchState == StopwatchState.RUNNING && stats.activeAttemptStartMs != null) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(75L)
            }
        }
    }

    val elapsedMillis = when (stats.stopwatchState) {
        StopwatchState.RUNNING -> {
            val start = stats.activeAttemptStartMs ?: nowMs
            (nowMs - start).coerceAtLeast(0L)
        }
        StopwatchState.STOPPED -> stats.lastSolveDurationMillis ?: 0L
        StopwatchState.READY -> 0L
    }

    val accentColor = when (stats.stopwatchState) {
        StopwatchState.RUNNING -> CyanReticle
        StopwatchState.STOPPED -> EmeraldSuccess
        StopwatchState.READY -> TextSecondary
    }
    val statusText = when (stats.stopwatchState) {
        StopwatchState.RUNNING -> "Timing · ${stats.activeAttemptMoves} moves"
        StopwatchState.STOPPED -> "Stopped · Saved to Stats"
        StopwatchState.READY -> "Ready · Starts on 1st move"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CarbonSurface, RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = when (stats.stopwatchState) {
                    StopwatchState.RUNNING -> CyanReticle.copy(alpha = 0.55f)
                    StopwatchState.STOPPED -> EmeraldSuccess.copy(alpha = 0.65f)
                    StopwatchState.READY -> SlateBorder
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag("solve_stopwatch_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (stats.stopwatchState == StopwatchState.STOPPED) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.Timer
                },
                contentDescription = "Solve Stopwatch",
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Stopwatch",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("solve_stopwatch_status")
                )
            }
        }
        Text(
            text = SessionStatsFormatter.formatStopwatch(elapsedMillis),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.testTag("solve_stopwatch_text")
        )
    }
}

@Composable
private fun SolvePercentageBar(
    state: CenterCubeUiState,
    modifier: Modifier = Modifier
) {
    val targetFraction = state.solveProgressFraction.coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 280),
        label = "solvePercentageFill"
    )
    val percent = (targetFraction * 100f).roundToInt().coerceIn(0, 100)
    val isComplete = state.cube.isSolved() || percent == 100
    val stageLabel = when {
        state.cube.isSolved() -> "Solved!"
        state.nextStep != null -> state.nextStep!!.stage.title
        else -> "Fix stickers to solve"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CarbonSurface, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (isComplete) EmeraldSuccess.copy(alpha = 0.65f) else SlateBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("solve_percentage_bar"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stageLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (isComplete) EmeraldSuccess else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isComplete) EmeraldSuccess else CyanReticle,
                modifier = Modifier.testTag("solve_percentage_text")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(CarbonElevated)
                .border(1.dp, SlateBorder, CircleShape)
        ) {
            if (animatedFraction > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            brush = if (isComplete) {
                                Brush.horizontalGradient(
                                    colors = listOf(CyanReticle, EmeraldSuccess)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(CyanReticle, IndigoAccent)
                                )
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun QuickCubeActionsRow(
    canUndo: Boolean,
    lastRecordedMove: Move?,
    isVoiceEnabled: Boolean,
    onUndo: () -> Unit,
    onToggleVoice: () -> Unit,
    onScramble: () -> Unit,
    onReset: () -> Unit,
    onRotateCube: (CubeAxis) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onUndo,
            enabled = canUndo,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.testTag("undo_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Undo Last Move",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (lastRecordedMove != null) "Undo ${lastRecordedMove.notation}" else "Undo",
                style = MaterialTheme.typography.labelLarge
            )
        }

        FilledTonalButton(
            onClick = onToggleVoice,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isVoiceEnabled) CyanReticle.copy(alpha = 0.2f) else CarbonElevated,
                contentColor = if (isVoiceEnabled) CyanReticle else TextSecondary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.testTag("voice_toggle_chip")
        ) {
            Icon(
                imageVector = if (isVoiceEnabled) {
                    Icons.AutoMirrored.Rounded.VolumeUp
                } else {
                    Icons.AutoMirrored.Rounded.VolumeOff
                },
                contentDescription = if (isVoiceEnabled) "Voice On" else "Voice Off",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isVoiceEnabled) "Voice ON" else "Voice OFF",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Button(
            onClick = onScramble,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanReticle,
                contentColor = ObsidianBg
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.testTag("scramble_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Scramble",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scramble", style = MaterialTheme.typography.labelLarge)
        }

        OutlinedButton(
            onClick = onReset,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.testTag("reset_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Reset Solved",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Solved", style = MaterialTheme.typography.labelLarge)
        }

        FilledTonalButton(
            onClick = { onRotateCube(CubeAxis.Y) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.testTag("rotate_y_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                contentDescription = "Reorient Center Y",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Yaw Centers", style = MaterialTheme.typography.labelLarge)
        }

        FilledTonalButton(
            onClick = { onRotateCube(CubeAxis.X) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.testTag("rotate_x_button")
        ) {
            Text("Flip Top Center", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CenterAnalysisCard(
    validation: CubeValidationResult,
    onOpenInput: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
            .testTag("center_analysis_card")
    ) {
        when (validation) {
            is CubeValidationResult.Invalid -> {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Invalid Cube",
                            tint = CoralError
                        )
                        Text(
                            text = validation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = CoralError
                        )
                    }
                    Text(
                        text = validation.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedButton(onClick = onOpenInput) {
                        Text("Fix Stickers in Net Painter")
                    }
                }
            }
            is CubeValidationResult.Valid -> {
                val profile: CenterProfile = validation.centerProfile
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CenterFocusStrong,
                                contentDescription = "Center Box Detector",
                                tint = CyanReticle,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Center Box Detection",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanReticle
                            )
                        }
                        Text(
                            text = profile.orientationLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier
                                .background(CarbonElevated, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 6 Detected Center Color Boxes Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Face.entries.forEach { face ->
                            val centerColor = profile.centerColors[face]!!
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CarbonSurfaceVariant, RoundedCornerShape(10.dp))
                                    .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(centerColor.composeColor)
                                        .border(1.5.dp, CyanReticle.copy(alpha = 0.7f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = face.symbol.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = centerColor.onStickerColor
                                    )
                                }
                                Text(
                                    text = centerColor.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text(
                        text = profile.recommendedStrategy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = profile.schemeNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SolutionPlayerSection(
    state: CenterCubeUiState,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onTogglePlay: () -> Unit,
    onCycleSpeed: () -> Unit,
    onJumpToStep: (Int) -> Unit,
    onSpeakCurrentStep: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
            .testTag("solution_player_card")
    ) {
        if (state.cube.isSolved() && state.steps.isEmpty()) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Solved",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Cube Solved!",
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "All 54 stickers align with their face's center color box. Tap Scramble or Yaw Centers to test another orientation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            return@Surface
        }

        if (state.steps.isEmpty()) {
            return@Surface
        }

        val total = state.steps.size
        val displayStep = state.nextStep ?: state.currentStep ?: state.steps.first()
        val completedCount = (state.currentStepIndex + 1).coerceIn(0, total)
        val isFinished = state.currentStepIndex == total - 1
        val listState = rememberLazyListState()

        LaunchedEffect(state.currentStepIndex) {
            val target = (state.currentStepIndex + 1).coerceIn(0, state.steps.lastIndex)
            listState.animateScrollToItem(target)
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Algorithm Solution",
                        tint = IndigoAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isFinished) "Solve Complete" else displayStep.stage.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFinished) EmeraldSuccess else TextPrimary
                    )
                }
                Text(
                    text = "${(state.solveProgressFraction * 100f).roundToInt().coerceIn(0, 100)}% • $completedCount/$total",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFinished) EmeraldSuccess else CyanReticle
                )
            }

            // Active Algorithm & Next Move Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonSurfaceVariant, RoundedCornerShape(14.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isFinished) EmeraldSuccess else CyanReticle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFinished) "OK" else displayStep.move.notation,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBg
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "${displayStep.algorithmName}  •  ${displayStep.formula}",
                        style = MaterialTheme.typography.labelLarge,
                        color = IndigoAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isFinished) "All layers match their center color boxes." else displayStep.move.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = displayStep.centerReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                FilledTonalIconButton(
                    onClick = onSpeakCurrentStep,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (state.isVoiceEnabled) CyanReticle.copy(alpha = 0.2f) else CarbonElevated,
                        contentColor = if (state.isVoiceEnabled) CyanReticle else TextSecondary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("speak_step_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = "Narrate current step"
                    )
                }
            }

            if (state.isVoiceEnabled && !state.lastSpokenText.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, CyanReticle.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("voice_caption_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = CyanReticle,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.lastSpokenText,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onStepBackward,
                    enabled = state.currentStepIndex >= 0,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("step_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                        contentDescription = "Previous Move"
                    )
                }

                Button(
                    onClick = onTogglePlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanReticle,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isPlaying) "Pause" else if (isFinished) "Replay Solve" else "Auto Play",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalIconButton(
                    onClick = onStepForward,
                    enabled = state.currentStepIndex < state.steps.lastIndex,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("step_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                        contentDescription = "Next Move"
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onCycleSpeed,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("speed_button")
                ) {
                    Text(
                        text = "${state.playbackSpeed}x",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Scrollable Move Chip Strip
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(state.steps) { idx, step ->
                    val isDone = idx <= state.currentStepIndex
                    val isNext = idx == state.currentStepIndex + 1
                    val bg = when {
                        isNext -> CyanReticle
                        isDone -> EmeraldSuccess.copy(alpha = 0.22f)
                        else -> CarbonElevated
                    }
                    val fg = when {
                        isNext -> ObsidianBg
                        isDone -> EmeraldSuccess
                        else -> TextPrimary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(
                                width = 1.dp,
                                color = if (isNext) CyanReticle else SlateBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onJumpToStep(idx) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.move.notation,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualTurnBar(
    canUndo: Boolean,
    lastRecordedMove: Move?,
    onUndo: () -> Unit,
    onManualMove: (Move) -> Unit
) {
    var primeMode by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Face Turns",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (canUndo) CarbonElevated else CarbonSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (canUndo) CyanReticle.copy(alpha = 0.55f) else SlateBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = canUndo, onClick = onUndo)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("manual_undo_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) CyanReticle else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (lastRecordedMove != null) "Undo ${lastRecordedMove.notation}" else "Undo",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canUndo) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (primeMode) IndigoAccent else CarbonElevated)
                            .clickable { primeMode = !primeMode }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (primeMode) "Counter-CW (')" else "Clockwise",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (primeMode) ObsidianBg else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Face.entries.forEach { face ->
                    val move = Move.fromFaceAndTurns(face, if (primeMode) 3 else 1)!!
                    FilledTonalIconButton(
                        onClick = { onManualMove(move) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = CarbonElevated,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("manual_move_${face.symbol}")
                    ) {
                        Text(
                            text = move.notation,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
