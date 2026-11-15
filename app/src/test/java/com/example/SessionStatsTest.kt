package com.example

import com.example.stats.InMemorySessionStatsTracker
import com.example.stats.SessionStatsFormatter
import com.example.stats.SolveCompletionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStatsTest {

    @Test
    fun `tracker accumulates total solves average moves and total time accurately`() {
        var nowMs = 1_000L
        val tracker = InMemorySessionStatsTracker(timeSource = { nowMs })
        assertEquals(com.example.stats.StopwatchState.READY, tracker.currentState().stopwatchState)

        // First solve: 4 moves over 2,400ms (with 1 undo -> 4 net moves)
        tracker.beginNewCubeAttempt()
        tracker.startStopwatchIfNeeded()
        assertEquals(com.example.stats.StopwatchState.RUNNING, tracker.currentState().stopwatchState)
        tracker.recordMoveStarted() // 1
        tracker.recordMoveStarted() // 2
        tracker.recordMoveUndone()  // 1
        tracker.recordMoveStarted() // 2
        tracker.recordMoveStarted() // 3
        tracker.recordMoveStarted() // 4
        nowMs += 2_400L
        val afterFirst = tracker.recordSolveCompletedIfNeeded(
            orientationLabel = "White Top · Green Front",
            completionMode = SolveCompletionMode.GUIDED_STEP
        )
        assertEquals(com.example.stats.StopwatchState.STOPPED, afterFirst.stopwatchState)
        assertEquals(2_400L, afterFirst.lastSolveDurationMillis)
        assertEquals(1, afterFirst.totalSolves)
        assertEquals(4, afterFirst.totalMovesAcrossSolves)
        assertEquals(2_400L, afterFirst.totalTimeMillis)
        assertEquals(4.0, afterFirst.averageMoveCount, 0.01)

        // Duplicate completion call on same cube does not double-count
        val duplicate = tracker.recordSolveCompletedIfNeeded(
            orientationLabel = "White Top · Green Front",
            completionMode = SolveCompletionMode.GUIDED_STEP
        )
        assertEquals(1, duplicate.totalSolves)

        // Second solve: 10 moves over 5,000ms -> avg 7.0 moves, 7,400ms total
        nowMs += 10_000L // idle gap before starting next solve is excluded
        tracker.beginNewCubeAttempt()
        assertEquals(com.example.stats.StopwatchState.READY, tracker.currentState().stopwatchState)
        repeat(10) {
            tracker.recordMoveStarted()
        }
        nowMs += 5_000L
        val afterSecond = tracker.recordSolveCompletedIfNeeded(
            orientationLabel = "Yellow Top · Blue Front",
            completionMode = SolveCompletionMode.AUTO_PLAY
        )
        assertEquals(2, afterSecond.totalSolves)
        assertEquals(14, afterSecond.totalMovesAcrossSolves)
        assertEquals(7_400L, afterSecond.totalTimeMillis)
        assertEquals(7.0, afterSecond.averageMoveCount, 0.01)
        assertEquals(4, afterSecond.bestMoveCount)
        assertEquals(2_400L, afterSecond.fastestSolveMillis)
        assertEquals(2, afterSecond.records.size)

        // Formatting checks
        assertEquals("00:07.4", SessionStatsFormatter.formatStopwatch(afterSecond.totalTimeMillis))
        assertEquals("01:05.2", SessionStatsFormatter.formatStopwatch(65_200L))
        assertEquals("7.4s", SessionStatsFormatter.formatDuration(afterSecond.totalTimeMillis))
        assertEquals("1m 05.2s", SessionStatsFormatter.formatDuration(65_200L))
        assertEquals("7.0", SessionStatsFormatter.formatAverageMoves(afterSecond.averageMoveCount, afterSecond.totalSolves))

        // Reset clears in-memory session stats
        val cleared = tracker.resetSessionStats()
        assertEquals(0, cleared.totalSolves)
        assertEquals(0L, cleared.totalTimeMillis)
        assertNull(cleared.bestMoveCount)
        assertNull(cleared.fastestSolveMillis)
    }
}
