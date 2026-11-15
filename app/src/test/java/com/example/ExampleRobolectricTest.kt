package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ui.CenterCubeApp
import com.example.ui.CenterCubeViewModel
import com.example.ui.theme.CenterCubeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches CenterCube`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CenterCube", appName)
    }

    @Test
    fun `navigate across Solve Input and Algorithms tabs and verify Center Box UI`() {
        val vm = CenterCubeViewModel()
        composeTestRule.setContent {
            CenterCubeTheme {
                CenterCubeApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cube_3d_viewport").assertIsDisplayed()
        composeTestRule.onNodeWithTag("solve_stopwatch_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("solve_percentage_bar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("solve_percentage_text").assertTextEquals("0%")
        composeTestRule.onNodeWithTag("cube_3d_viewport").performScrollTo()
        composeTestRule.onNodeWithTag("arrow_guide_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_arrows_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("arrow_guide_badge").assertDoesNotExist()
        composeTestRule.onNodeWithTag("toggle_arrows_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("arrow_guide_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("step_forward_button").performScrollTo().performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeTestRule.waitForIdle()
        assertTrue(vm.uiState.value.solveProgressFraction > 0f)
        val cubeBeforeMistake = vm.uiState.value.cube
        val stepIndexBeforeMistake = vm.uiState.value.currentStepIndex
        // Make a manual move (mistake) and verify Undo reverts back to the exact previous state
        val wrongMove = com.example.cube.Move.entries.first { it != vm.uiState.value.nextStep?.move }
        vm.applyManualMove(wrongMove)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("solve_percentage_bar").performScrollTo()
        composeTestRule.onNodeWithTag("undo_button").performScrollTo().performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeTestRule.waitForIdle()
        assertEquals(cubeBeforeMistake, vm.uiState.value.cube)
        assertEquals(stepIndexBeforeMistake, vm.uiState.value.currentStepIndex)

        composeTestRule.onNodeWithTag("solve_percentage_bar").performScrollTo()
        composeTestRule.onNodeWithTag("reset_button").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("solve_percentage_text").assertTextEquals("100%")
        composeTestRule.onNodeWithTag("scramble_button").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("solve_percentage_text").assertTextEquals("0%")
        composeTestRule.onNodeWithTag("rotate_y_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("center_analysis_card").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_input").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mode_net_painter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("solve_from_input_button").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_algorithms").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("algorithm_card_center_cross").assertIsDisplayed()
    }

    @Test
    fun `voice narration speaks current step and respects mute toggle`() {
        val spokenLines = mutableListOf<String>()
        var stoppedCount = 0
        val recordingNarrator = object : com.example.audio.VoiceNarrator {
            override val isReady: Boolean = true
            override val isUnavailable: Boolean = false
            override fun speak(text: String, flushQueue: Boolean, speechRate: Float) {
                spokenLines += text
            }
            override fun isSpeaking(): Boolean = false
            override fun stop() {
                stoppedCount++
            }
            override fun shutdown() = Unit
        }
        val vm = CenterCubeViewModel(initialVoiceNarrator = recordingNarrator)
        composeTestRule.setContent {
            CenterCubeTheme {
                CenterCubeApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Tap Speak Current Step button on active move card
        composeTestRule.onNodeWithTag("speak_step_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(spokenLines.isNotEmpty())
        assertTrue(spokenLines.last().contains("Step 1 of"))
        assertTrue(spokenLines.last().contains("center"))
        composeTestRule.onNodeWithTag("voice_caption_chip").performScrollTo().assertIsDisplayed()

        // Step forward narrates step automatically
        val countBeforeStep = spokenLines.size
        composeTestRule.onNodeWithTag("step_forward_button").performScrollTo().performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeTestRule.waitForIdle()
        assertTrue(spokenLines.size > countBeforeStep)

        // Mute via top bar toggle stops TTS and prevents automatic speech
        composeTestRule.onNodeWithTag("voice_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(false, vm.uiState.value.isVoiceEnabled)
        assertTrue(stoppedCount > 0)

        val countWhileMuted = spokenLines.size
        composeTestRule.onNodeWithTag("step_forward_button").performScrollTo().performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeTestRule.waitForIdle()
        assertEquals(countWhileMuted, spokenLines.size)

        // Re-enable via top bar toggle speaks confirmation + current step
        composeTestRule.onNodeWithTag("voice_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, vm.uiState.value.isVoiceEnabled)
        assertTrue(spokenLines.last().startsWith("Voice guide on."))
    }

    @Test
    fun `stats dashboard displays recorded solve count average moves and total time`() {
        var fakeClockMs = 5_000L
        val vm = CenterCubeViewModel(timeSource = { fakeClockMs })
        // Load a quick 1-move puzzle from solved cube: apply R', so solving takes 1 move (R)
        val oneMovePuzzle = com.example.cube.CubeState.solved().applyMove(com.example.cube.Move.R_PRIME)
        vm.applyInputPreset(oneMovePuzzle, "1-move test")

        composeTestRule.setContent {
            CenterCubeTheme {
                CenterCubeApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("solve_stopwatch_text").assertTextEquals("00:00.0")
        composeTestRule.onNodeWithTag("solve_stopwatch_status").assertTextEquals("Ready · Starts on 1st move")

        // Navigate to Stats tab before solving -> empty state shows 0 solves
        composeTestRule.onNodeWithTag("nav_stats").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stats_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_total_solves_value").assertTextEquals("0")
        composeTestRule.onNodeWithTag("stat_avg_moves_value").assertTextEquals("—")
        composeTestRule.onNodeWithTag("stats_dashboard")
            .performScrollToNode(hasTestTag("stats_empty_state"))
        composeTestRule.onNodeWithTag("stats_empty_state").assertIsDisplayed()

        // Return to Solve via CTA button and complete the 1-move solve
        composeTestRule.onNodeWithTag("stats_start_solving_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        fakeClockMs += 1_500L
        composeTestRule.onNodeWithTag("step_forward_button").performScrollTo().performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("solve_stopwatch_card").performScrollTo()
        composeTestRule.onNodeWithTag("solve_stopwatch_status").assertTextEquals("Stopped · Saved to Stats")
        assertEquals(com.example.stats.StopwatchState.STOPPED, vm.uiState.value.stats.stopwatchState)

        // Verify Stats tab reflects 1 solve, 1.0 average moves, and recorded solve row
        composeTestRule.onNodeWithTag("nav_stats").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stat_total_solves_value").assertTextEquals("1")
        composeTestRule.onNodeWithTag("stat_avg_moves_value").assertTextEquals("1.0")
        composeTestRule.onNodeWithTag("stats_dashboard")
            .performScrollToNode(hasTestTag("solve_record_row_1"))
        composeTestRule.onNodeWithTag("solve_record_row_1").assertIsDisplayed()

        // Clear session stats resets back to 0
        composeTestRule.onNodeWithTag("stats_dashboard")
            .performScrollToIndex(0)
        composeTestRule.onNodeWithTag("reset_stats_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stat_total_solves_value").assertTextEquals("0")
        composeTestRule.onNodeWithTag("stats_dashboard")
            .performScrollToNode(hasTestTag("stats_empty_state"))
        composeTestRule.onNodeWithTag("stats_empty_state").assertIsDisplayed()
    }
}
