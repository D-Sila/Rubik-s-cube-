package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.cube.CenterProfile
import com.example.cube.Face
import com.example.cube.Move
import com.example.cube.SolveStage
import com.example.cube.SolveStep
import java.util.Locale

interface VoiceNarrator {
    val isReady: Boolean
    val isUnavailable: Boolean
    fun speak(text: String, flushQueue: Boolean = true, speechRate: Float = 1.0f)
    fun isSpeaking(): Boolean
    fun stop()
    fun shutdown()
}

class NoOpVoiceNarrator : VoiceNarrator {
    override val isReady: Boolean = false
    override val isUnavailable: Boolean = false
    override fun speak(text: String, flushQueue: Boolean, speechRate: Float) = Unit
    override fun isSpeaking(): Boolean = false
    override fun stop() = Unit
    override fun shutdown() = Unit
}

class AndroidVoiceNarrator(
    context: Context,
    private val onAvailabilityChanged: (available: Boolean) -> Unit = {}
) : VoiceNarrator {

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var unavailable = false
    private var pendingUtterance: Triple<String, Boolean, Float>? = null

    override val isReady: Boolean
        get() = initialized && !unavailable

    override val isUnavailable: Boolean
        get() = unavailable

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val langResult = tts?.setLanguage(Locale.US)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.getDefault()
                    }
                    initialized = true
                    unavailable = false
                    onAvailabilityChanged(true)
                    pendingUtterance?.let { (text, flush, rate) ->
                        pendingUtterance = null
                        speak(text, flush, rate)
                    }
                } else {
                    initialized = false
                    unavailable = true
                    pendingUtterance = null
                    onAvailabilityChanged(false)
                }
            }
        } catch (_: Throwable) {
            tts = null
            initialized = false
            unavailable = true
            onAvailabilityChanged(false)
        }
    }

    override fun speak(text: String, flushQueue: Boolean, speechRate: Float) {
        if (text.isBlank() || unavailable) return
        if (!initialized) {
            pendingUtterance = Triple(text, flushQueue, speechRate)
            return
        }
        val engine = tts ?: return
        try {
            engine.setSpeechRate(speechRate.coerceIn(0.75f, 1.8f))
            val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(text, queueMode, null, "centercube_${System.nanoTime()}")
        } catch (_: Throwable) {
            // Keep app resilient if TTS service disconnects transiently
        }
    }

    override fun isSpeaking(): Boolean {
        return try {
            initialized && (tts?.isSpeaking == true)
        } catch (_: Throwable) {
            false
        }
    }

    override fun stop() {
        pendingUtterance = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
            // Ignore stop failures
        }
    }

    override fun shutdown() {
        pendingUtterance = null
        initialized = false
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
            // Ignore shutdown failures
        } finally {
            tts = null
        }
    }
}

object CubeSpeechFormatter {

    fun faceSpokenName(face: Face): String = when (face) {
        Face.U -> "Top"
        Face.D -> "Bottom"
        Face.F -> "Front"
        Face.B -> "Back"
        Face.L -> "Left"
        Face.R -> "Right"
    }

    fun stageSpokenTitle(stage: SolveStage): String = when (stage) {
        SolveStage.SOLVED -> "Solved"
        SolveStage.SHORT_OPTIMAL -> "Direct Reduction"
        SolveStage.BOTTOM_CROSS -> "Stage 1, Base Cross"
        SolveStage.FIRST_LAYER_CORNERS -> "Stage 2, Base Corners"
        SolveStage.MIDDLE_LAYER_EDGES -> "Stage 3, Middle Edges"
        SolveStage.TOP_CROSS -> "Stage 4, Top Center Cross"
        SolveStage.TOP_CROSS_ALIGN -> "Stage 5, Top Edge Align"
        SolveStage.TOP_CORNERS_POSITION -> "Stage 6, Corner Position"
        SolveStage.TOP_CORNERS_ORIENT -> "Stage 7, Corner Twist"
    }

    fun formatMoveInstruction(move: Move, centerProfile: CenterProfile?): String {
        val faceName = faceSpokenName(move.face)
        val centerColor = centerProfile?.centerColors?.get(move.face)?.label
        val centerPhrase = if (centerColor != null) ", $centerColor center," else ""
        val turnPhrase = when (move.quarterTurns) {
            1 -> "clockwise"
            2 -> "180 degrees"
            else -> "counter-clockwise"
        }
        return "Turn $faceName face$centerPhrase $turnPhrase"
    }

    fun formatStepNarration(
        step: SolveStep,
        totalSteps: Int,
        centerProfile: CenterProfile?,
        includeStage: Boolean
    ): String {
        val stepNumber = step.index
        val moveInstruction = formatMoveInstruction(step.move, centerProfile)
        return if (includeStage) {
            "Step $stepNumber of $totalSteps, ${stageSpokenTitle(step.stage)}. $moveInstruction."
        } else {
            "Step $stepNumber of $totalSteps. $moveInstruction."
        }
    }
}
