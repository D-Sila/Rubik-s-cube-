package com.example.cube

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StickerBlue
import com.example.ui.theme.StickerGreen
import com.example.ui.theme.StickerOrange
import com.example.ui.theme.StickerRed
import com.example.ui.theme.StickerWhite
import com.example.ui.theme.StickerYellow

enum class CubeColor(
    val label: String,
    val shortCode: Char,
    val composeColor: Color,
    val onStickerColor: Color
) {
    WHITE("White", 'W', StickerWhite, Color(0xFF0F172A)),
    YELLOW("Yellow", 'Y', StickerYellow, Color(0xFF0F172A)),
    RED("Red", 'R', StickerRed, Color.White),
    ORANGE("Orange", 'O', StickerOrange, Color(0xFF0F172A)),
    BLUE("Blue", 'B', StickerBlue, Color.White),
    GREEN("Green", 'G', StickerGreen, Color(0xFF052E16))
}

enum class Face(val symbol: Char, val displayName: String) {
    U('U', "Top (U)"),
    D('D', "Bottom (D)"),
    F('F', "Front (F)"),
    B('B', "Back (B)"),
    L('L', "Left (L)"),
    R('R', "Right (R)")
}

enum class Move(
    val notation: String,
    val face: Face,
    val quarterTurns: Int, // 1 = CW, 2 = 180, 3 = CCW (')
    val description: String
) {
    U("U", Face.U, 1, "Turn Top face clockwise"),
    U_PRIME("U'", Face.U, 3, "Turn Top face counter-clockwise"),
    U2("U2", Face.U, 2, "Turn Top face 180°"),

    D("D", Face.D, 1, "Turn Bottom face clockwise"),
    D_PRIME("D'", Face.D, 3, "Turn Bottom face counter-clockwise"),
    D2("D2", Face.D, 2, "Turn Bottom face 180°"),

    F("F", Face.F, 1, "Turn Front face clockwise"),
    F_PRIME("F'", Face.F, 3, "Turn Front face counter-clockwise"),
    F2("F2", Face.F, 2, "Turn Front face 180°"),

    B("B", Face.B, 1, "Turn Back face clockwise"),
    B_PRIME("B'", Face.B, 3, "Turn Back face counter-clockwise"),
    B2("B2", Face.B, 2, "Turn Back face 180°"),

    L("L", Face.L, 1, "Turn Left face clockwise"),
    L_PRIME("L'", Face.L, 3, "Turn Left face counter-clockwise"),
    L2("L2", Face.L, 2, "Turn Left face 180°"),

    R("R", Face.R, 1, "Turn Right face clockwise"),
    R_PRIME("R'", Face.R, 3, "Turn Right face counter-clockwise"),
    R2("R2", Face.R, 2, "Turn Right face 180°");

    fun inverse(): Move = fromFaceAndTurns(face, (4 - quarterTurns) % 4)!!

    companion object {
        fun fromFaceAndTurns(face: Face, turnsMod4: Int): Move? {
            val normalized = ((turnsMod4 % 4) + 4) % 4
            if (normalized == 0) return null
            return entries.first { it.face == face && it.quarterTurns == normalized }
        }

        fun parseNotation(notation: String): List<Move> {
            return notation.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.mapNotNull { token ->
                entries.firstOrNull { it.notation == token }
            }
        }
    }
}

enum class SolveStage(
    val title: String,
    val algorithmName: String,
    val formula: String
) {
    SOLVED("Solved", "Identity", "—"),
    SHORT_OPTIMAL("Direct Reduction", "Optimal Shortest Path", "≤ 4-Move BFS"),
    BOTTOM_CROSS("Stage 1 · Base Cross", "Center-Aligned Cross Insert", "S2 / U' R' F R"),
    FIRST_LAYER_CORNERS("Stage 2 · Base Corners", "Right-Left Corner Triggers", "R U R' / F' U' F"),
    MIDDLE_LAYER_EDGES("Stage 3 · Middle Edges", "Center-Matched Edge Insert", "U R U' R' U' F' U F"),
    TOP_CROSS("Stage 4 · Top Center Cross", "OLL Cross Pattern (FRURUF)", "F R U R' U' F'"),
    TOP_CROSS_ALIGN("Stage 5 · Top Edge Align", "Sune Edge Permutation", "R U R' U R U2 R'"),
    TOP_CORNERS_POSITION("Stage 6 · Corner Position", "Niklas 3-Corner Cycle", "U R U' L' U R' U' L"),
    TOP_CORNERS_ORIENT("Stage 7 · Corner Twist", "Down-Right Twist Cycle", "R' D' R D")
}

data class CenterProfile(
    val centerColors: Map<Face, CubeColor>,
    val orientationLabel: String,
    val schemeNote: String,
    val detectedStage: SolveStage,
    val recommendedStrategy: String,
    val topCenter: CubeColor,
    val frontCenter: CubeColor,
    val bottomCenter: CubeColor
)

data class SolveStep(
    val index: Int,
    val move: Move,
    val stage: SolveStage,
    val algorithmName: String,
    val formula: String,
    val centerReason: String,
    val resultingCube: CubeState
)

sealed interface CubeValidationResult {
    data class Valid(val centerProfile: CenterProfile) : CubeValidationResult
    data class Invalid(val title: String, val reason: String) : CubeValidationResult
}

data class AlgorithmCatalogItem(
    val id: String,
    val name: String,
    val category: String,
    val notation: String,
    val centerBoxRule: String,
    val description: String,
    val setupInverseMoves: String
)
