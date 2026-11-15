package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.stats.SessionStatsFormatter
import com.example.stats.SessionStatsState
import com.example.stats.SolveRecord
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun StatsScreen(
    stats: SessionStatsState,
    isWideLayout: Boolean,
    onResetStats: () -> Unit,
    onGoToSolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(stats.activeAttemptStartMs) {
        if (stats.activeAttemptStartMs != null) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(200L)
            }
        }
    }

    val activeElapsedMs = stats.activeAttemptStartMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("stats_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SessionHeaderCard(
                stats = stats,
                activeElapsedMs = activeElapsedMs,
                onResetStats = onResetStats
            )
        }

        item {
            StatsKpiGrid(
                stats = stats,
                isWideLayout = isWideLayout
            )
        }

        if (stats.records.isEmpty()) {
            item {
                EmptyStatsCard(onGoToSolve = onGoToSolve)
            }
        } else {
            item {
                MoveEfficiencyBreakdownCard(records = stats.records)
            }

            item {
                Text(
                    text = "Session Solve Log",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            itemsIndexed(stats.records, key = { _, rec -> rec.id }) { idx, record ->
                SolveRecordRow(
                    solveNumber = stats.totalSolves - idx,
                    record = record
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SessionHeaderCard(
    stats: SessionStatsState,
    activeElapsedMs: Long,
    onResetStats: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
    ) {
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = "Session Statistics",
                        tint = CyanReticle
                    )
                    Column {
                        Text(
                            text = "Session Stats Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "In-memory session tracker · Resets when app closes",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                if (stats.totalSolves > 0) {
                    OutlinedButton(
                        onClick = onResetStats,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("reset_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear Session Stats",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (stats.activeAttemptStartMs != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, CyanReticle.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("active_solve_timer_banner"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyanReticle)
                        )
                        Text(
                            text = "Current Attempt · ${stats.activeAttemptMoves} moves",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = SessionStatsFormatter.formatDuration(activeElapsedMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanReticle
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsKpiGrid(
    stats: SessionStatsState,
    isWideLayout: Boolean
) {
    val totalSolvesText = stats.totalSolves.toString()
    val avgMovesText = SessionStatsFormatter.formatAverageMoves(stats.averageMoveCount, stats.totalSolves)
    val totalTimeText = SessionStatsFormatter.formatDuration(stats.totalTimeMillis)
    val avgTimeText = if (stats.totalSolves == 0) "—" else SessionStatsFormatter.formatDuration(stats.averageTimeMillis)
    val bestMovesSub = stats.bestMoveCount?.let { "Best: $it moves" } ?: "Complete 1 solve"
    val fastestTimeSub = stats.fastestSolveMillis?.let {
        "Fastest: ${SessionStatsFormatter.formatDuration(it)}"
    } ?: "No solves yet"

    if (isWideLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatKpiCard(
                title = "Total Solves",
                value = totalSolvesText,
                subtitle = if (stats.totalSolves == 1) "1 completed cube" else "${stats.totalSolves} completed cubes",
                icon = Icons.Rounded.CheckCircle,
                accent = EmeraldSuccess,
                modifier = Modifier.weight(1f).testTag("stat_total_solves_card"),
                valueTag = "stat_total_solves_value"
            )
            StatKpiCard(
                title = "Avg Moves / Solve",
                value = avgMovesText,
                subtitle = bestMovesSub,
                icon = Icons.Rounded.Speed,
                accent = CyanReticle,
                modifier = Modifier.weight(1f).testTag("stat_avg_moves_card"),
                valueTag = "stat_avg_moves_value"
            )
            StatKpiCard(
                title = "Total Time Spent",
                value = totalTimeText,
                subtitle = "Across all solves",
                icon = Icons.Rounded.Timer,
                accent = IndigoAccent,
                modifier = Modifier.weight(1f).testTag("stat_total_time_card"),
                valueTag = "stat_total_time_value"
            )
            StatKpiCard(
                title = "Avg Solve Time",
                value = avgTimeText,
                subtitle = fastestTimeSub,
                icon = Icons.Rounded.AvTimer,
                accent = CyanReticle,
                modifier = Modifier.weight(1f).testTag("stat_avg_time_card"),
                valueTag = "stat_avg_time_value"
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard(
                    title = "Total Solves",
                    value = totalSolvesText,
                    subtitle = if (stats.totalSolves == 1) "1 completed cube" else "${stats.totalSolves} completed cubes",
                    icon = Icons.Rounded.CheckCircle,
                    accent = EmeraldSuccess,
                    modifier = Modifier.weight(1f).testTag("stat_total_solves_card"),
                    valueTag = "stat_total_solves_value"
                )
                StatKpiCard(
                    title = "Avg Moves / Solve",
                    value = avgMovesText,
                    subtitle = bestMovesSub,
                    icon = Icons.Rounded.Speed,
                    accent = CyanReticle,
                    modifier = Modifier.weight(1f).testTag("stat_avg_moves_card"),
                    valueTag = "stat_avg_moves_value"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard(
                    title = "Total Time Spent",
                    value = totalTimeText,
                    subtitle = "Across all solves",
                    icon = Icons.Rounded.Timer,
                    accent = IndigoAccent,
                    modifier = Modifier.weight(1f).testTag("stat_total_time_card"),
                    valueTag = "stat_total_time_value"
                )
                StatKpiCard(
                    title = "Avg Solve Time",
                    value = avgTimeText,
                    subtitle = fastestTimeSub,
                    icon = Icons.Rounded.AvTimer,
                    accent = CyanReticle,
                    modifier = Modifier.weight(1f).testTag("stat_avg_time_card"),
                    valueTag = "stat_avg_time_value"
                )
            }
        }
    }
}

@Composable
private fun StatKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    valueTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = modifier.border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag(valueTag)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoveEfficiencyBreakdownCard(records: List<SolveRecord>) {
    val recent = records.take(6).reversed()
    val maxMoves = (recent.maxOfOrNull { it.moveCount } ?: 1).coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Recent Solve Efficiency",
                    tint = IndigoAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Recent Solve Moves",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            recent.forEachIndexed { idx, rec ->
                val fraction = (rec.moveCount.toFloat() / maxMoves.toFloat()).coerceIn(0.12f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "#${records.size - recent.size + idx + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.width(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(CarbonElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CyanReticle, IndigoAccent)
                                    )
                                )
                        )
                    }
                    Text(
                        text = "${rec.moveCount}m · ${SessionStatsFormatter.formatDuration(rec.durationMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SolveRecordRow(
    solveNumber: Int,
    record: SolveRecord
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .testTag("solve_record_row_$solveNumber")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$solveNumber",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${record.moveCount} moves · ${record.completionMode.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = record.orientationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = SessionStatsFormatter.formatDuration(record.durationMillis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyanReticle
            )
        }
    }
}

@Composable
private fun EmptyStatsCard(onGoToSolve: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
            .testTag("stats_empty_state")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = "No Solves Yet",
                tint = TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No Completed Solves Yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "Finish a scramble using Auto Play, Next Step, or Manual Turns to track total solves, average moves, and time spent in this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Button(
                onClick = onGoToSolve,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanReticle,
                    contentColor = ObsidianBg
                ),
                modifier = Modifier.testTag("stats_start_solving_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Go to Solve",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Go to Solve",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
