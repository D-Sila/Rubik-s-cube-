package com.example.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AndroidVoiceNarrator
import com.example.audio.CubeSpeechFormatter
import com.example.audio.NoOpVoiceNarrator
import com.example.audio.VoiceNarrator
import com.example.cube.AlgorithmCatalogItem
import com.example.cube.CenterAwareSolver
import com.example.cube.CenterProfile
import com.example.cube.CubeColor
import com.example.cube.CubeState
import com.example.cube.CubeValidationResult
import com.example.cube.CubeValidator
import com.example.cube.Face
import com.example.cube.Move
import com.example.cube.SolveStep
import com.example.stats.InMemorySessionStatsTracker
import com.example.stats.SessionStatsState
import com.example.stats.SolveCompletionMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    SOLVE,
    INPUT,
    ALGORITHMS,
    STATS
}

enum class InputSubMode {
    NET_PAINTER,
    CAMERA_SCANNER
}

enum class CubeAxis {
    X, Y, Z
}

data class MoveHistoryEntry(
    val move: Move,
    val previousCube: CubeState,
    val previousBaseScrambledCube: CubeState,
    val previousSteps: List<SolveStep>,
    val previousStepIndex: Int,
    val previousSessionTotalMoves: Int
)

data class CenterCubeUiState(
    val cube: CubeState = CubeState.solved(),
    val baseScrambledCube: CubeState = CubeState.solved(),
    val validation: CubeValidationResult = CubeValidator.validate(CubeState.solved()),
    val steps: List<SolveStep> = emptyList(),
    val currentStepIndex: Int = -1,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    val activeTab: AppTab = AppTab.SOLVE,
    val inputSubMode: InputSubMode = InputSubMode.NET_PAINTER,
    val selectedPaintColor: CubeColor = CubeColor.BLUE,
    val scannedFaces: Set<Face> = emptySet(),
    val activeAnimationMove: Move? = null,
    val moveProgress: Float = 0f,
    val lastScrambleNotation: String = "",
    val statusBanner: String? = null,
    val showMoveArrows: Boolean = true,
    val sessionTotalMoves: Int = 0,
    val moveHistory: List<MoveHistoryEntry> = emptyList(),
    val isVoiceEnabled: Boolean = true,
    val isTtsAvailable: Boolean = true,
    val lastSpokenText: String? = null,
    val stats: SessionStatsState = SessionStatsState()
) {
    val canUndo: Boolean
        get() = moveHistory.isNotEmpty()

    val lastRecordedMove: Move?
        get() = moveHistory.lastOrNull()?.move
    val validProfile: CenterProfile?
        get() = (validation as? CubeValidationResult.Valid)?.centerProfile

    val currentStep: SolveStep?
        get() = if (currentStepIndex in steps.indices) steps[currentStepIndex] else null

    val nextStep: SolveStep?
        get() = if (currentStepIndex + 1 in steps.indices) steps[currentStepIndex + 1] else null

    val solveProgressFraction: Float
        get() = when {
            cube.isSolved() -> 1f
            steps.isEmpty() -> 0f
            currentStepIndex >= 0 -> ((currentStepIndex + 1).coerceIn(0, steps.size)).toFloat() / steps.size.toFloat()
            sessionTotalMoves > 0 -> ((sessionTotalMoves - steps.size).coerceAtLeast(0)).toFloat() / sessionTotalMoves.toFloat()
            else -> 0f
        }
}

class CenterCubeViewModel(
    initialVoiceNarrator: VoiceNarrator = NoOpVoiceNarrator(),
    timeSource: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private var voiceNarrator: VoiceNarrator = initialVoiceNarrator
    private val statsTracker = InMemorySessionStatsTracker(timeSource = timeSource)
    private val _uiState = MutableStateFlow(CenterCubeUiState())
    val uiState: StateFlow<CenterCubeUiState> = _uiState.asStateFlow()

    private var playJob: Job? = null
    private var stepAnimJob: Job? = null

    init {
        // Start with a short, engaging scramble so the user immediately sees Center-Box detection in action
        scramble(length = 12, seed = 42L, announceVoice = false)
    }

    fun resetSessionStats() {
        val cleared = statsTracker.resetSessionStats()
        _uiState.update {
            it.copy(
                stats = cleared,
                statusBanner = "Session statistics cleared"
            )
        }
    }

    fun attachAndroidVoiceNarrator(context: Context) {
        if (voiceNarrator is NoOpVoiceNarrator) {
            voiceNarrator = AndroidVoiceNarrator(context.applicationContext) { available ->
                _uiState.update { curr ->
                    curr.copy(
                        isTtsAvailable = available,
                        statusBanner = if (!available && curr.isVoiceEnabled) {
                            "Text-to-Speech engine unavailable on this device"
                        } else {
                            curr.statusBanner
                        }
                    )
                }
            }
        }
    }

    override fun onCleared() {
        voiceNarrator.shutdown()
        super.onCleared()
    }

    fun selectTab(tab: AppTab) {
        stopPlayback()
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectInputSubMode(mode: InputSubMode) {
        _uiState.update { it.copy(inputSubMode = mode) }
    }

    fun selectPaintColor(color: CubeColor) {
        _uiState.update { it.copy(selectedPaintColor = color) }
    }

    fun toggleMoveArrows() {
        _uiState.update { it.copy(showMoveArrows = !it.showMoveArrows) }
    }

    fun toggleVoiceGuide() {
        val nextEnabled = !_uiState.value.isVoiceEnabled
        if (!nextEnabled) {
            voiceNarrator.stop()
            _uiState.update {
                it.copy(
                    isVoiceEnabled = false,
                    statusBanner = "Voice narration muted"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isVoiceEnabled = true,
                    statusBanner = "Voice narration enabled"
                )
            }
            speakCurrentStep(force = true, prefix = "Voice guide on. ")
        }
    }

    fun speakCurrentStep(force: Boolean = true, prefix: String = "") {
        val state = _uiState.value
        if (!force && !state.isVoiceEnabled) return
        val message = when {
            state.cube.isSolved() -> "${prefix}Cube is solved! All layers match their center color boxes."
            state.validation is CubeValidationResult.Invalid ->
                "${prefix}Cube has invalid stickers. Fix stickers in Net Painter to solve."
            state.nextStep != null -> {
                val step = state.nextStep!!
                val narration = CubeSpeechFormatter.formatStepNarration(
                    step = step,
                    totalSteps = state.steps.size,
                    centerProfile = state.validProfile,
                    includeStage = true
                )
                "$prefix$narration"
            }
            else -> "${prefix}No active solve steps."
        }
        speakText(message)
    }

    fun resetSolved() {
        stopPlayback()
        stepAnimJob?.cancel()
        voiceNarrator.stop()
        statsTracker.clearActiveAttempt()
        val solved = CubeState.solved()
        _uiState.update {
            it.copy(
                cube = solved,
                baseScrambledCube = solved,
                validation = CubeValidator.validate(solved),
                steps = emptyList(),
                currentStepIndex = -1,
                activeAnimationMove = null,
                moveProgress = 0f,
                lastScrambleNotation = "",
                scannedFaces = emptySet(),
                sessionTotalMoves = 0,
                moveHistory = emptyList(),
                stats = statsTracker.currentState(),
                statusBanner = "Reset to Standard Solved Cube (White Top · Green Front)"
            )
        }
    }

    fun scramble(
        length: Int = 14,
        seed: Long = System.currentTimeMillis(),
        announceVoice: Boolean = true
    ) {
        stopPlayback()
        stepAnimJob?.cancel()
        statsTracker.beginNewCubeAttempt()
        val (scrambled, moves) = CubeState.generateScramble(length = length, seed = seed)
        val validation = CubeValidator.validate(scrambled)
        val solveResult = CenterAwareSolver.solve(scrambled).getOrNull()
        val totalMoves = solveResult?.steps?.size ?: 0
        _uiState.update {
            it.copy(
                cube = scrambled,
                baseScrambledCube = scrambled,
                validation = validation,
                steps = solveResult?.steps ?: emptyList(),
                currentStepIndex = -1,
                activeAnimationMove = null,
                moveProgress = 0f,
                lastScrambleNotation = moves.joinToString(" ") { m -> m.notation },
                sessionTotalMoves = totalMoves,
                moveHistory = emptyList(),
                stats = statsTracker.currentState(),
                statusBanner = null
            )
        }
        if (announceVoice && _uiState.value.isVoiceEnabled && totalMoves > 0) {
            speakCurrentStep(force = false, prefix = "New scramble, $totalMoves moves. ")
        }
    }

    fun applyManualMove(move: Move) {
        stopPlayback()
        stepAnimJob?.cancel()
        stepAnimJob = viewModelScope.launch {
            val preMove = _uiState.value
            if (preMove.cube.isSolved()) {
                statsTracker.beginNewCubeAttempt()
            }
            statsTracker.startStopwatchIfNeeded()
            _uiState.update { it.copy(stats = statsTracker.currentState()) }

            animateSingleMove(move)
            val curr = _uiState.value
            val historyEntry = MoveHistoryEntry(
                move = move,
                previousCube = curr.cube,
                previousBaseScrambledCube = curr.baseScrambledCube,
                previousSteps = curr.steps,
                previousStepIndex = curr.currentStepIndex,
                previousSessionTotalMoves = curr.sessionTotalMoves
            )
            statsTracker.recordMoveStarted()
            val next = curr.nextStep
            if (next != null && next.move == move) {
                val nextIdx = curr.currentStepIndex + 1
                if (next.resultingCube.isSolved()) {
                    statsTracker.recordSolveCompletedIfNeeded(
                        orientationLabel = curr.validProfile?.orientationLabel ?: "Standard Orientation",
                        completionMode = SolveCompletionMode.MANUAL
                    )
                }
                _uiState.update {
                    it.copy(
                        cube = next.resultingCube,
                        currentStepIndex = nextIdx,
                        activeAnimationMove = null,
                        moveProgress = 0f,
                        moveHistory = curr.moveHistory + historyEntry,
                        stats = statsTracker.currentState(),
                        statusBanner = "Applied ${move.notation} (Step ${nextIdx + 1}/${curr.steps.size})"
                    )
                }
                if (_uiState.value.isVoiceEnabled) {
                    val updated = _uiState.value
                    val upcoming = updated.nextStep
                    if (updated.cube.isSolved()) {
                        speakText("Cube solved! All layers match their center color boxes.")
                    } else if (upcoming != null) {
                        val cue = CubeSpeechFormatter.formatStepNarration(
                            step = upcoming,
                            totalSteps = updated.steps.size,
                            centerProfile = updated.validProfile,
                            includeStage = upcoming.stage != next.stage
                        )
                        speakText("Good. Next, $cue")
                    }
                }
            } else {
                val nextCube = curr.cube.applyMove(move)
                val validation = CubeValidator.validate(nextCube)
                val solveResult = CenterAwareSolver.solve(nextCube).getOrNull()
                val newSteps = solveResult?.steps ?: emptyList()
                val newSessionMax = maxOf(curr.sessionTotalMoves, newSteps.size)
                if (nextCube.isSolved()) {
                    val profileLabel = (validation as? CubeValidationResult.Valid)?.centerProfile?.orientationLabel
                        ?: "Standard Orientation"
                    statsTracker.recordSolveCompletedIfNeeded(
                        orientationLabel = profileLabel,
                        completionMode = SolveCompletionMode.MANUAL
                    )
                }
                _uiState.update {
                    it.copy(
                        cube = nextCube,
                        baseScrambledCube = nextCube,
                        validation = validation,
                        steps = newSteps,
                        currentStepIndex = -1,
                        activeAnimationMove = null,
                        moveProgress = 0f,
                        sessionTotalMoves = if (nextCube.isSolved()) curr.sessionTotalMoves else newSessionMax,
                        moveHistory = curr.moveHistory + historyEntry,
                        stats = statsTracker.currentState(),
                        statusBanner = "Applied ${move.notation} (Tap Undo to revert)"
                    )
                }
                if (_uiState.value.isVoiceEnabled) {
                    if (nextCube.isSolved()) {
                        speakText("Cube solved! All layers match their center color boxes.")
                    } else {
                        speakCurrentStep(force = false, prefix = "Recalculated. ")
                    }
                }
            }
        }
    }

    fun undoLastMove() {
        stopPlayback()
        val curr = _uiState.value
        val lastEntry = curr.moveHistory.lastOrNull() ?: return
        val remainingHistory = curr.moveHistory.dropLast(1)

        stepAnimJob?.cancel()
        stepAnimJob = viewModelScope.launch {
            animateSingleMove(lastEntry.move.inverse())
            statsTracker.recordMoveUndone()
            _uiState.update {
                it.copy(
                    cube = lastEntry.previousCube,
                    baseScrambledCube = lastEntry.previousBaseScrambledCube,
                    validation = CubeValidator.validate(lastEntry.previousCube),
                    steps = lastEntry.previousSteps,
                    currentStepIndex = lastEntry.previousStepIndex,
                    sessionTotalMoves = lastEntry.previousSessionTotalMoves,
                    moveHistory = remainingHistory,
                    activeAnimationMove = null,
                    moveProgress = 0f,
                    stats = statsTracker.currentState(),
                    statusBanner = "Undid ${lastEntry.move.notation}"
                )
            }
            if (_uiState.value.isVoiceEnabled) {
                val restored = _uiState.value
                val next = restored.nextStep
                val msg = if (next != null) {
                    val cue = CubeSpeechFormatter.formatMoveInstruction(next.move, restored.validProfile)
                    "Undid ${lastEntry.move.notation}. Next, Step ${next.index}: $cue."
                } else {
                    "Undid ${lastEntry.move.notation}."
                }
                speakText(msg)
            }
        }
    }

    fun rotateWholeCube(axis: CubeAxis) {
        stopPlayback()
        stepAnimJob?.cancel()
        val current = _uiState.value.cube
        val rotated = when (axis) {
            CubeAxis.X -> current.rotateWholeX()
            CubeAxis.Y -> current.rotateWholeY()
            CubeAxis.Z -> current.rotateWholeZ()
        }
        val validation = CubeValidator.validate(rotated)
        val solveResult = CenterAwareSolver.solve(rotated).getOrNull()
        val profile = (validation as? CubeValidationResult.Valid)?.centerProfile
        _uiState.update {
            it.copy(
                cube = rotated,
                baseScrambledCube = rotated,
                validation = validation,
                steps = solveResult?.steps ?: emptyList(),
                currentStepIndex = -1,
                activeAnimationMove = null,
                moveProgress = 0f,
                moveHistory = emptyList(),
                statusBanner = profile?.let { p ->
                    "Re-oriented → Noticed ${p.orientationLabel} center boxes"
                }
            )
        }
    }

    fun paintSticker(face: Face, index: Int) {
        stopPlayback()
        stepAnimJob?.cancel()
        val color = _uiState.value.selectedPaintColor
        val updated = _uiState.value.cube.withSticker(face, index, color)
        val validation = CubeValidator.validate(updated)
        val solveResult = if (validation is CubeValidationResult.Valid) {
            CenterAwareSolver.solve(updated).getOrNull()
        } else {
            null
        }
        _uiState.update {
            it.copy(
                cube = updated,
                baseScrambledCube = updated,
                validation = validation,
                steps = solveResult?.steps ?: emptyList(),
                currentStepIndex = -1,
                moveHistory = emptyList(),
                statusBanner = if (index == 4) {
                    "Updated ${face.displayName} center color box to ${color.label}"
                } else {
                    null
                }
            )
        }
    }

    fun applyScannedFace(face: Face, stickers: List<CubeColor>) {
        stopPlayback()
        stepAnimJob?.cancel()
        val updated = _uiState.value.cube.withFace(face, stickers)
        val validation = CubeValidator.validate(updated)
        val solveResult = if (validation is CubeValidationResult.Valid) {
            CenterAwareSolver.solve(updated).getOrNull()
        } else {
            null
        }
        val newScanned = _uiState.value.scannedFaces + face
        _uiState.update {
            it.copy(
                cube = updated,
                baseScrambledCube = updated,
                validation = validation,
                steps = solveResult?.steps ?: emptyList(),
                currentStepIndex = -1,
                scannedFaces = newScanned,
                moveHistory = emptyList(),
                statusBanner = "Captured ${face.displayName} (${newScanned.size}/6 faces)"
            )
        }
    }

    fun applyInputPreset(presetCube: CubeState, banner: String) {
        stopPlayback()
        stepAnimJob?.cancel()
        val validation = CubeValidator.validate(presetCube)
        val solveResult = CenterAwareSolver.solve(presetCube).getOrNull()
        val presetSteps = solveResult?.steps ?: emptyList()
        if (!presetCube.isSolved()) {
            statsTracker.beginNewCubeAttempt()
        } else {
            statsTracker.clearActiveAttempt()
        }
        _uiState.update {
            it.copy(
                cube = presetCube,
                baseScrambledCube = presetCube,
                validation = validation,
                steps = presetSteps,
                currentStepIndex = -1,
                sessionTotalMoves = presetSteps.size,
                moveHistory = emptyList(),
                stats = statsTracker.currentState(),
                activeTab = AppTab.SOLVE,
                statusBanner = banner
            )
        }
    }

    fun solveAndJumpToSolveTab() {
        stopPlayback()
        stepAnimJob?.cancel()
        val cube = _uiState.value.cube
        val validation = CubeValidator.validate(cube)
        val solveResult = CenterAwareSolver.solve(cube).getOrNull()
        val newSteps = solveResult?.steps ?: emptyList()
        if (!cube.isSolved()) {
            statsTracker.beginNewCubeAttempt()
        }
        _uiState.update {
            it.copy(
                baseScrambledCube = cube,
                validation = validation,
                steps = newSteps,
                currentStepIndex = -1,
                sessionTotalMoves = newSteps.size,
                moveHistory = emptyList(),
                stats = statsTracker.currentState(),
                activeTab = AppTab.SOLVE,
                statusBanner = if (validation is CubeValidationResult.Valid) {
                    "Solved in ${newSteps.size} center-guided moves"
                } else {
                    "Fix cube parity/stickers before solving"
                }
            )
        }
        if (_uiState.value.isVoiceEnabled && validation is CubeValidationResult.Valid) {
            speakCurrentStep(force = false, prefix = "Ready in ${newSteps.size} moves. ")
        }
    }

    fun loadAlgorithmDemo(item: AlgorithmCatalogItem) {
        stopPlayback()
        stepAnimJob?.cancel()
        val setupMoves = Move.parseNotation(item.setupInverseMoves)
        val demoCube = CubeState.solved().applyMoves(setupMoves)
        val validation = CubeValidator.validate(demoCube)
        val solveResult = CenterAwareSolver.solve(demoCube).getOrNull()
        val newSteps = solveResult?.steps ?: emptyList()
        statsTracker.beginNewCubeAttempt()
        _uiState.update {
            it.copy(
                cube = demoCube,
                baseScrambledCube = demoCube,
                validation = validation,
                steps = newSteps,
                currentStepIndex = -1,
                sessionTotalMoves = newSteps.size,
                moveHistory = emptyList(),
                stats = statsTracker.currentState(),
                activeTab = AppTab.SOLVE,
                statusBanner = "Loaded ${item.name} pattern (${item.notation})"
            )
        }
        if (_uiState.value.isVoiceEnabled && newSteps.isNotEmpty()) {
            speakCurrentStep(force = false, prefix = "${item.name} loaded. ")
        }
    }

    fun stepForward() {
        stopPlayback()
        val state = _uiState.value
        val nextIdx = state.currentStepIndex + 1
        if (nextIdx !in state.steps.indices) return
        val step = state.steps[nextIdx]
        val prevStage = state.steps.getOrNull(nextIdx - 1)?.stage
        val includeStage = nextIdx == 0 || prevStage != step.stage

        stepAnimJob?.cancel()
        stepAnimJob = viewModelScope.launch {
            statsTracker.startStopwatchIfNeeded()
            _uiState.update { it.copy(stats = statsTracker.currentState()) }
            if (_uiState.value.isVoiceEnabled) {
                val cue = CubeSpeechFormatter.formatStepNarration(
                    step = step,
                    totalSteps = state.steps.size,
                    centerProfile = state.validProfile,
                    includeStage = includeStage
                )
                speakText(cue)
            }
            animateSingleMove(step.move)
            val curr = _uiState.value
            statsTracker.recordMoveStarted()
            if (step.resultingCube.isSolved()) {
                statsTracker.recordSolveCompletedIfNeeded(
                    orientationLabel = curr.validProfile?.orientationLabel ?: "Standard Orientation",
                    completionMode = SolveCompletionMode.GUIDED_STEP
                )
            }
            val entry = MoveHistoryEntry(
                move = step.move,
                previousCube = curr.cube,
                previousBaseScrambledCube = curr.baseScrambledCube,
                previousSteps = curr.steps,
                previousStepIndex = curr.currentStepIndex,
                previousSessionTotalMoves = curr.sessionTotalMoves
            )
            _uiState.update {
                it.copy(
                    cube = step.resultingCube,
                    currentStepIndex = nextIdx,
                    moveHistory = curr.moveHistory + entry,
                    stats = statsTracker.currentState(),
                    activeAnimationMove = null,
                    moveProgress = 0f
                )
            }
            if (_uiState.value.isVoiceEnabled && nextIdx == curr.steps.lastIndex) {
                speakText("Cube solved! All layers match their center color boxes.", flushQueue = false)
            }
        }
    }

    fun stepBackward() {
        stopPlayback()
        val state = _uiState.value
        if (state.currentStepIndex < 0) return
        val currentStep = state.steps[state.currentStepIndex]
        val prevIdx = state.currentStepIndex - 1
        val targetCube = if (prevIdx >= 0) state.steps[prevIdx].resultingCube else state.baseScrambledCube

        stepAnimJob?.cancel()
        stepAnimJob = viewModelScope.launch {
            animateSingleMove(currentStep.move.inverse())
            _uiState.update {
                it.copy(
                    cube = targetCube,
                    currentStepIndex = prevIdx,
                    activeAnimationMove = null,
                    moveProgress = 0f
                )
            }
            if (_uiState.value.isVoiceEnabled) {
                val updated = _uiState.value
                updated.nextStep?.let { next ->
                    val cue = CubeSpeechFormatter.formatMoveInstruction(next.move, updated.validProfile)
                    speakText("Stepped back. Next, Step ${next.index}: $cue.")
                }
            }
        }
    }

    fun jumpToStep(targetIndex: Int) {
        stopPlayback()
        stepAnimJob?.cancel()
        val state = _uiState.value
        val clamped = targetIndex.coerceIn(-1, state.steps.lastIndex)
        val targetCube = if (clamped >= 0) state.steps[clamped].resultingCube else state.baseScrambledCube
        _uiState.update {
            it.copy(
                cube = targetCube,
                currentStepIndex = clamped,
                activeAnimationMove = null,
                moveProgress = 0f
            )
        }
        if (_uiState.value.isVoiceEnabled) {
            speakCurrentStep(force = false)
        }
    }

    fun togglePlay() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
            return
        }
        val state = _uiState.value
        if (state.steps.isEmpty()) return

        // If already at the end, restart from beginning before playing
        if (state.currentStepIndex >= state.steps.lastIndex) {
            statsTracker.beginNewCubeAttempt()
            _uiState.update {
                it.copy(
                    cube = it.baseScrambledCube,
                    currentStepIndex = -1,
                    moveHistory = emptyList(),
                    stats = statsTracker.currentState()
                )
            }
        }

        _uiState.update { it.copy(isPlaying = true) }
        playJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                val curr = _uiState.value
                val nextIdx = curr.currentStepIndex + 1
                if (nextIdx !in curr.steps.indices) {
                    _uiState.update { it.copy(isPlaying = false) }
                    break
                }
                val step = curr.steps[nextIdx]
                val prevStage = curr.steps.getOrNull(nextIdx - 1)?.stage
                val includeStage = nextIdx == 0 || prevStage != step.stage
                statsTracker.startStopwatchIfNeeded()
                _uiState.update { it.copy(stats = statsTracker.currentState()) }
                if (curr.isVoiceEnabled) {
                    val cue = CubeSpeechFormatter.formatStepNarration(
                        step = step,
                        totalSteps = curr.steps.size,
                        centerProfile = curr.validProfile,
                        includeStage = includeStage
                    )
                    speakText(cue)
                }
                animateSingleMove(step.move)
                val latest = _uiState.value
                statsTracker.recordMoveStarted()
                if (step.resultingCube.isSolved()) {
                    statsTracker.recordSolveCompletedIfNeeded(
                        orientationLabel = latest.validProfile?.orientationLabel ?: "Standard Orientation",
                        completionMode = SolveCompletionMode.AUTO_PLAY
                    )
                }
                val entry = MoveHistoryEntry(
                    move = step.move,
                    previousCube = latest.cube,
                    previousBaseScrambledCube = latest.baseScrambledCube,
                    previousSteps = latest.steps,
                    previousStepIndex = latest.currentStepIndex,
                    previousSessionTotalMoves = latest.sessionTotalMoves
                )
                _uiState.update {
                    it.copy(
                        cube = step.resultingCube,
                        currentStepIndex = nextIdx,
                        moveHistory = latest.moveHistory + entry,
                        stats = statsTracker.currentState(),
                        activeAnimationMove = null,
                        moveProgress = 0f
                    )
                }
                val afterStep = _uiState.value
                if (afterStep.isVoiceEnabled && nextIdx == afterStep.steps.lastIndex) {
                    speakText("Cube solved! All layers match their center color boxes.", flushQueue = false)
                } else if (afterStep.isVoiceEnabled && voiceNarrator.isReady) {
                    var waitedMs = 0L
                    while (_uiState.value.isPlaying && voiceNarrator.isSpeaking() && waitedMs < 3500L) {
                        delay(100L)
                        waitedMs += 100L
                    }
                }
                delay((220f / _uiState.value.playbackSpeed).toLong())
            }
        }
    }

    fun cycleSpeed() {
        val next = when (_uiState.value.playbackSpeed) {
            1f -> 1.5f
            1.5f -> 2.5f
            else -> 1f
        }
        _uiState.update { it.copy(playbackSpeed = next) }
    }

    private fun speakText(text: String, flushQueue: Boolean = true) {
        val speed = _uiState.value.playbackSpeed
        val speechRate = when {
            speed >= 2.4f -> 1.45f
            speed >= 1.4f -> 1.2f
            else -> 1.0f
        }
        _uiState.update { it.copy(lastSpokenText = text) }
        voiceNarrator.speak(text = text, flushQueue = flushQueue, speechRate = speechRate)
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        voiceNarrator.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private suspend fun animateSingleMove(move: Move) {
        val speed = _uiState.value.playbackSpeed.coerceAtLeast(0.5f)
        val stepsCount = 12
        val frameDelayMs = (20f / speed).toLong().coerceIn(6L, 30L)
        _uiState.update { it.copy(activeAnimationMove = move, moveProgress = 0f) }
        for (i in 1..stepsCount) {
            delay(frameDelayMs)
            val eased = FastOutSlowInEasing.transform(i.toFloat() / stepsCount.toFloat())
            _uiState.update { it.copy(moveProgress = eased) }
        }
    }
}
