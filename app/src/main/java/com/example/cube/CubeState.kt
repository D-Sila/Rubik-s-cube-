package com.example.cube

import kotlin.random.Random

/**
 * Immutable 3x3 Rubik's Cube representation (54 stickers: 6 faces x 9 stickers).
 * Index 4 on each face is ALWAYS the fixed center color box for that face.
 *
 * Facelet layout looking directly at each face from outside:
 *   0 1 2
 *   3 4 5
 *   6 7 8
 */
data class CubeState(val stickers: List<CubeColor>) {
    init {
        require(stickers.size == 54) { "CubeState must contain exactly 54 stickers." }
    }

    fun sticker(face: Face, index: Int): CubeColor = stickers[face.ordinal * 9 + index]

    fun faceStickers(face: Face): List<CubeColor> {
        val start = face.ordinal * 9
        return stickers.subList(start, start + 9)
    }

    fun centerColor(face: Face): CubeColor = sticker(face, 4)

    fun withSticker(face: Face, index: Int, color: CubeColor): CubeState {
        val mutable = stickers.toMutableList()
        mutable[face.ordinal * 9 + index] = color
        return CubeState(mutable)
    }

    fun withFace(face: Face, faceStickers: List<CubeColor>): CubeState {
        require(faceStickers.size == 9)
        val mutable = stickers.toMutableList()
        val start = face.ordinal * 9
        for (i in 0 until 9) {
            mutable[start + i] = faceStickers[i]
        }
        return CubeState(mutable)
    }

    /**
     * A cube is solved when every sticker on each face matches that face's center color box.
     * This works for any cube orientation and any valid center color scheme.
     */
    fun isSolved(): Boolean {
        val centers = Face.entries.map { centerColor(it) }
        if (centers.distinct().size != 6) return false
        return Face.entries.all { face ->
            val center = centerColor(face)
            (0 until 9).all { idx -> sticker(face, idx) == center }
        }
    }

    fun applyMove(move: Move): CubeState {
        var current = this
        repeat(move.quarterTurns) {
            current = current.applyQuarterTurnCW(move.face)
        }
        return current
    }

    fun applyMoves(moves: List<Move>): CubeState {
        var current = this
        for (move in moves) {
            current = current.applyMove(move)
        }
        return current
    }

    private fun applyQuarterTurnCW(face: Face): CubeState {
        val arr = stickers.toTypedArray()

        fun idx(f: Face, i: Int) = f.ordinal * 9 + i

        // Rotate face stickers clockwise
        val fBase = face.ordinal * 9
        val oldFace = Array(9) { arr[fBase + it] }
        arr[fBase + 0] = oldFace[6]
        arr[fBase + 1] = oldFace[3]
        arr[fBase + 2] = oldFace[0]
        arr[fBase + 3] = oldFace[7]
        arr[fBase + 4] = oldFace[4]
        arr[fBase + 5] = oldFace[1]
        arr[fBase + 6] = oldFace[8]
        arr[fBase + 7] = oldFace[5]
        arr[fBase + 8] = oldFace[2]

        fun cycle4(a: Int, b: Int, c: Int, d: Int) {
            val temp = arr[d]
            arr[d] = arr[c]
            arr[c] = arr[b]
            arr[b] = arr[a]
            arr[a] = temp
        }

        when (face) {
            Face.U -> {
                // F -> L -> B -> R -> F
                for (i in 0..2) {
                    cycle4(idx(Face.F, i), idx(Face.L, i), idx(Face.B, i), idx(Face.R, i))
                }
            }
            Face.D -> {
                // F -> R -> B -> L -> F
                for (i in 6..8) {
                    cycle4(idx(Face.F, i), idx(Face.R, i), idx(Face.B, i), idx(Face.L, i))
                }
            }
            Face.R -> {
                // F[2,5,8] -> U[2,5,8] -> B[6,3,0] -> D[2,5,8] -> F[2,5,8]
                cycle4(idx(Face.F, 2), idx(Face.U, 2), idx(Face.B, 6), idx(Face.D, 2))
                cycle4(idx(Face.F, 5), idx(Face.U, 5), idx(Face.B, 3), idx(Face.D, 5))
                cycle4(idx(Face.F, 8), idx(Face.U, 8), idx(Face.B, 0), idx(Face.D, 8))
            }
            Face.L -> {
                // U[0,3,6] -> F[0,3,6] -> D[0,3,6] -> B[8,5,2] -> U[0,3,6]
                cycle4(idx(Face.U, 0), idx(Face.F, 0), idx(Face.D, 0), idx(Face.B, 8))
                cycle4(idx(Face.U, 3), idx(Face.F, 3), idx(Face.D, 3), idx(Face.B, 5))
                cycle4(idx(Face.U, 6), idx(Face.F, 6), idx(Face.D, 6), idx(Face.B, 2))
            }
            Face.F -> {
                // U[6,7,8] -> R[0,3,6] -> D[2,1,0] -> L[8,5,2] -> U[6,7,8]
                cycle4(idx(Face.U, 6), idx(Face.R, 0), idx(Face.D, 2), idx(Face.L, 8))
                cycle4(idx(Face.U, 7), idx(Face.R, 3), idx(Face.D, 1), idx(Face.L, 5))
                cycle4(idx(Face.U, 8), idx(Face.R, 6), idx(Face.D, 0), idx(Face.L, 2))
            }
            Face.B -> {
                // U[2,1,0] -> L[0,3,6] -> D[6,7,8] -> R[8,5,2] -> U[2,1,0]
                cycle4(idx(Face.U, 2), idx(Face.L, 0), idx(Face.D, 6), idx(Face.R, 8))
                cycle4(idx(Face.U, 1), idx(Face.L, 3), idx(Face.D, 7), idx(Face.R, 5))
                cycle4(idx(Face.U, 0), idx(Face.L, 6), idx(Face.D, 8), idx(Face.R, 2))
            }
        }

        return CubeState(arr.toList())
    }

    /**
     * Whole-cube rotation around Y axis (like U: R becomes F, F becomes L, L becomes B, B becomes R).
     */
    fun rotateWholeY(): CubeState {
        fun rotCW(list: List<CubeColor>): List<CubeColor> = listOf(
            list[6], list[3], list[0],
            list[7], list[4], list[1],
            list[8], list[5], list[2]
        )

        fun rotCCW(list: List<CubeColor>): List<CubeColor> = listOf(
            list[2], list[5], list[8],
            list[1], list[4], list[7],
            list[0], list[3], list[6]
        )

        return CubeState(
            rotCW(faceStickers(Face.U)) +
                rotCCW(faceStickers(Face.D)) +
                faceStickers(Face.R) +
                faceStickers(Face.L) +
                faceStickers(Face.F) +
                faceStickers(Face.B)
        )
    }

    /**
     * Whole-cube rotation around X axis (like R: F becomes U, U becomes B, B becomes D, D becomes F).
     */
    fun rotateWholeX(): CubeState {
        fun rotCW(list: List<CubeColor>): List<CubeColor> = listOf(
            list[6], list[3], list[0],
            list[7], list[4], list[1],
            list[8], list[5], list[2]
        )

        fun rotCCW(list: List<CubeColor>): List<CubeColor> = listOf(
            list[2], list[5], list[8],
            list[1], list[4], list[7],
            list[0], list[3], list[6]
        )

        fun rot180(list: List<CubeColor>): List<CubeColor> = list.reversed()

        // F -> U, U -> B (rotated 180), B -> D (rotated 180), D -> F, R -> CW, L -> CCW
        return CubeState(
            faceStickers(Face.F) +
                rot180(faceStickers(Face.B)) +
                faceStickers(Face.D) +
                rot180(faceStickers(Face.U)) +
                rotCCW(faceStickers(Face.L)) +
                rotCW(faceStickers(Face.R))
        )
    }

    /**
     * Whole-cube rotation around Z axis (like F: L->U, U->R, R->D, D->L).
     */
    fun rotateWholeZ(): CubeState {
        fun rotCW(list: List<CubeColor>): List<CubeColor> = listOf(
            list[6], list[3], list[0],
            list[7], list[4], list[1],
            list[8], list[5], list[2]
        )

        fun rotCCW(list: List<CubeColor>): List<CubeColor> = listOf(
            list[2], list[5], list[8],
            list[1], list[4], list[7],
            list[0], list[3], list[6]
        )

        return CubeState(
            rotCW(faceStickers(Face.L)) +
                rotCW(faceStickers(Face.R)) +
                rotCW(faceStickers(Face.F)) +
                rotCCW(faceStickers(Face.B)) +
                rotCW(faceStickers(Face.D)) +
                rotCW(faceStickers(Face.U))
        )
    }

    companion object {
        /**
         * Standard WCA Solved Cube:
         * U = White, D = Yellow, F = Green, B = Blue, L = Orange, R = Red.
         */
        fun solved(
            u: CubeColor = CubeColor.WHITE,
            d: CubeColor = CubeColor.YELLOW,
            f: CubeColor = CubeColor.GREEN,
            b: CubeColor = CubeColor.BLUE,
            l: CubeColor = CubeColor.ORANGE,
            r: CubeColor = CubeColor.RED
        ): CubeState {
            return CubeState(
                List(9) { u } +
                    List(9) { d } +
                    List(9) { f } +
                    List(9) { b } +
                    List(9) { l } +
                    List(9) { r }
            )
        }

        fun generateScramble(length: Int = 18, seed: Long = System.currentTimeMillis()): Pair<CubeState, List<Move>> {
            val rng = Random(seed)
            val moves = mutableListOf<Move>()
            var lastFace: Face? = null
            var secondLastFace: Face? = null
            val faces = Face.entries

            while (moves.size < length) {
                val face = faces[rng.nextInt(faces.size)]
                if (face == lastFace) continue
                // Avoid redundant opposite-face repetition like R L R
                if (secondLastFace == face && isOpposite(face, lastFace)) continue
                val turns = rng.nextInt(1, 4)
                val move = Move.fromFaceAndTurns(face, turns)!!
                moves.add(move)
                secondLastFace = lastFace
                lastFace = face
            }
            return solved().applyMoves(moves) to moves
        }

        private fun isOpposite(a: Face, b: Face?): Boolean = when (a) {
            Face.U -> b == Face.D
            Face.D -> b == Face.U
            Face.F -> b == Face.B
            Face.B -> b == Face.F
            Face.L -> b == Face.R
            Face.R -> b == Face.L
        }
    }
}
