package com.example.stats

import java.util.Locale

enum class SolveCompletionMode(val label: String) {
    GUIDED_STEP("Guided Step"),
    AUTO_PLAY("Auto Play"),
    MANUAL("Manual Turns")
}

enum class StopwatchState {
    READY,
    RUNNING,
    STOPPED
}

data class SolveRecord(
    val id: Long,
    val moveCount: Int,
    val durationMillis: Long,
    val orientationLabel: String,
    val completionMode: SolveCompletionMode,
    val completedAtEpochMs: Long
)

data class SessionStatsState(
    val totalSolves: Int = 0,
    val totalMovesAcrossSolves: Int = 0,
    val totalTimeMillis: Long = 0L,
    val fastestSolveMillis: Long? = null,
    val bestMoveCount: Int? = null,
    val activeAttemptMoves: Int = 0,
    val activeAttemptStartMs: Long? = null,
    val lastSolveDurationMillis: Long? = null,
    val stopwatchState: StopwatchState = StopwatchState.READY,
    val records: List<SolveRecord> = emptyList()
) {
    val averageMoveCount: Double
        get() = if (totalSolves == 0) 0.0 else totalMovesAcrossSolves.toDouble() / totalSolves.toDouble()

    val averageTimeMillis: Long
        get() = if (totalSolves == 0) 0L else totalTimeMillis / totalSolves.toLong()
}

class InMemorySessionStatsTracker(
    private val timeSource: () -> Long = { System.currentTimeMillis() }
) {
    private var state = SessionStatsState()
    private var nextRecordId = 1L
    private var attemptRecorded = false

    fun currentState(): SessionStatsState = state

    fun beginNewCubeAttempt() {
        attemptRecorded = false
        state = state.copy(
            activeAttemptMoves = 0,
            activeAttemptStartMs = null,
            lastSolveDurationMillis = null,
            stopwatchState = StopwatchState.READY
        )
    }

    fun clearActiveAttempt() {
        state = state.copy(
            activeAttemptMoves = 0,
            activeAttemptStartMs = null,
            lastSolveDurationMillis = null,
            stopwatchState = StopwatchState.READY
        )
    }

    fun startStopwatchIfNeeded(): SessionStatsState {
        if (attemptRecorded) return state
        if (state.activeAttemptStartMs == null) {
            state = state.copy(
                activeAttemptStartMs = timeSource(),
                lastSolveDurationMillis = null,
                stopwatchState = StopwatchState.RUNNING
            )
        }
        return state
    }

    fun recordMoveStarted() {
        if (attemptRecorded) return
        val startedAt = state.activeAttemptStartMs ?: timeSource()
        state = state.copy(
            activeAttemptMoves = state.activeAttemptMoves + 1,
            activeAttemptStartMs = startedAt,
            lastSolveDurationMillis = null,
            stopwatchState = StopwatchState.RUNNING
        )
    }

    fun recordMoveUndone() {
        if (attemptRecorded) return
        val updatedMoves = (state.activeAttemptMoves - 1).coerceAtLeast(0)
        state = state.copy(
            activeAttemptMoves = updatedMoves,
            activeAttemptStartMs = if (updatedMoves == 0) null else state.activeAttemptStartMs,
            stopwatchState = if (updatedMoves == 0) StopwatchState.READY else StopwatchState.RUNNING
        )
    }

    fun recordSolveCompletedIfNeeded(
        orientationLabel: String,
        completionMode: SolveCompletionMode
    ): SessionStatsState {
        if (attemptRecorded) return state
        val now = timeSource()
        val start = state.activeAttemptStartMs ?: now
        val duration = (now - start).coerceAtLeast(1L)
        val moves = state.activeAttemptMoves.coerceAtLeast(1)

        val record = SolveRecord(
            id = nextRecordId++,
            moveCount = moves,
            durationMillis = duration,
            orientationLabel = orientationLabel,
            completionMode = completionMode,
            completedAtEpochMs = now
        )

        attemptRecorded = true
        val prevFastest = state.fastestSolveMillis
        val prevBestMoves = state.bestMoveCount
        state = state.copy(
            totalSolves = state.totalSolves + 1,
            totalMovesAcrossSolves = state.totalMovesAcrossSolves + moves,
            totalTimeMillis = state.totalTimeMillis + duration,
            fastestSolveMillis = if (prevFastest == null) duration else minOf(prevFastest, duration),
            bestMoveCount = if (prevBestMoves == null) moves else minOf(prevBestMoves, moves),
            activeAttemptMoves = 0,
            activeAttemptStartMs = null,
            lastSolveDurationMillis = duration,
            stopwatchState = StopwatchState.STOPPED,
            records = listOf(record) + state.records
        )
        return state
    }

    fun resetSessionStats(): SessionStatsState {
        attemptRecorded = false
        state = SessionStatsState()
        return state
    }
}

object SessionStatsFormatter {
    fun formatStopwatch(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val totalTenths = safeMillis / 100L
        val tenths = totalTenths % 10L
        val totalSeconds = totalTenths / 10L
        val seconds = totalSeconds % 60L
        val totalMinutes = totalSeconds / 60L
        val minutes = totalMinutes % 60L
        val hours = totalMinutes / 60L

        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
        } else {
            String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
        }
    }

    fun formatDuration(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val totalTenths = safeMillis / 100L
        val tenths = totalTenths % 10L
        val totalSeconds = totalTenths / 10L
        val seconds = totalSeconds % 60L
        val totalMinutes = totalSeconds / 60L
        val minutes = totalMinutes % 60L
        val hours = totalMinutes / 60L

        return when {
            hours > 0L -> String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
            totalMinutes > 0L -> String.format(Locale.US, "%dm %02d.%ds", totalMinutes, seconds, tenths)
            else -> String.format(Locale.US, "%d.%ds", seconds, tenths)
        }
    }

    fun formatAverageMoves(avgMoves: Double, totalSolves: Int): String {
        if (totalSolves <= 0) return "—"
        return String.format(Locale.US, "%.1f", avgMoves)
    }
}
