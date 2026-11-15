package com.example

import com.example.cube.CenterAwareSolver
import com.example.cube.CubeColor
import com.example.cube.CubeState
import com.example.cube.CubeValidationResult
import com.example.cube.CubeValidator
import com.example.cube.Face
import com.example.cube.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterAwareSolverTest {

    @Test
    fun `sexy move repeated 6 times returns cube to solved`() {
        var cube = CubeState.solved()
        val sexy = Move.parseNotation("R U R' U'")
        repeat(6) {
            cube = cube.applyMoves(sexy)
        }
        assertTrue(cube.isSolved())
    }

    @Test
    fun `whole cube rotations preserve solvability and change center profile`() {
        val cube = CubeState.solved()
            .rotateWholeX()
            .rotateWholeY()
            .rotateWholeZ()
        assertTrue(cube.isSolved())
        val validation = CubeValidator.validate(cube)
        assertTrue(validation is CubeValidationResult.Valid)
    }

    @Test
    fun `validator detects duplicate center color box`() {
        val badCube = CubeState.solved().withSticker(Face.U, 4, CubeColor.GREEN)
        val validation = CubeValidator.validate(badCube)
        assertTrue(validation is CubeValidationResult.Invalid)
        val invalid = validation as CubeValidationResult.Invalid
        assertTrue(invalid.title.contains("Center", ignoreCase = true))
    }

    @Test
    fun `validator detects corner twist parity error`() {
        val solved = CubeState.solved()
        // Twist UFR corner in place
        val twisted = solved
            .withSticker(Face.U, 8, solved.sticker(Face.F, 2))
            .withSticker(Face.F, 2, solved.sticker(Face.R, 0))
            .withSticker(Face.R, 0, solved.sticker(Face.U, 8))

        val validation = CubeValidator.validate(twisted)
        assertTrue(validation is CubeValidationResult.Invalid)
    }

    @Test
    fun `solver solves 200 random scrambles across varied center orientations`() {
        for (i in 0 until 200) {
            var (scrambled, _) = CubeState.generateScramble(length = 22, seed = 1000L + i)
            // Apply varied whole-cube rotations so every center color is tested as Top/Front
            repeat(i % 4) { scrambled = scrambled.rotateWholeY() }
            repeat((i / 4) % 4) { scrambled = scrambled.rotateWholeX() }
            repeat((i / 16) % 2) { scrambled = scrambled.rotateWholeZ() }

            val result = CenterAwareSolver.solve(scrambled)
            assertTrue("Solve failed on seed ${1000L + i}: ${result.exceptionOrNull()}", result.isSuccess)
            val output = result.getOrThrow()
            assertFalse("Scrambled cube returned 0 steps if not solved", !scrambled.isSolved() && output.steps.isEmpty())

            val finalCube = scrambled.applyMoves(output.steps.map { it.move })
            assertTrue("Final cube not solved on seed ${1000L + i}", finalCube.isSolved())
        }
    }

    @Test
    fun `solver detects custom non-standard center color scheme`() {
        // Swap Green and Blue centers + stickers cleanly via custom solved cube
        val customSolved = CubeState.solved(
            u = CubeColor.YELLOW,
            d = CubeColor.WHITE,
            f = CubeColor.RED,
            b = CubeColor.ORANGE,
            l = CubeColor.BLUE,
            r = CubeColor.GREEN
        )
        val scrambled = customSolved.applyMoves(Move.parseNotation("R U F' L2 D B' R2 U'"))
        val result = CenterAwareSolver.solve(scrambled)
        assertTrue(result.isSuccess)
        val output = result.getOrThrow()
        assertEquals(CubeColor.YELLOW, output.centerProfile.topCenter)
        assertEquals(CubeColor.RED, output.centerProfile.frontCenter)
        val finalCube = scrambled.applyMoves(output.steps.map { it.move })
        assertTrue(finalCube.isSolved())
    }
}
