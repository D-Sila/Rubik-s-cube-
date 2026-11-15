package com.example.cube

import android.graphics.Color as AndroidColor
import androidx.camera.core.ImageProxy
import kotlin.math.abs

object ColorClassifier {

    data class GridSampleResult(
        val stickers: List<CubeColor>,
        val centerColor: CubeColor,
        val sampledRgb: List<Int>
    )

    /**
     * Samples a centered 3x3 square grid from a CameraX YUV_420_888 ImageProxy
     * and classifies each cell into the closest Rubik's CubeColor.
     */
    fun analyzeImage(image: ImageProxy): GridSampleResult {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val minDim = minOf(width, height)
        val gridSpan = (minDim * 0.54f).toInt().coerceAtLeast(30)
        val startX = (width - gridSpan) / 2
        val startY = (height - gridSpan) / 2
        val cellSize = gridSpan / 3

        val rotation = image.imageInfo.rotationDegrees
        val rawStickers = MutableList(9) { CubeColor.WHITE }
        val rawRgb = MutableList(9) { AndroidColor.WHITE }

        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val cx = startX + col * cellSize + cellSize / 2
                val cy = startY + row * cellSize + cellSize / 2
                val patchRadius = (cellSize / 6).coerceIn(2, 8)

                var rSum = 0L
                var gSum = 0L
                var bSum = 0L
                var count = 0

                for (dy in -patchRadius..patchRadius step 2) {
                    for (dx in -patchRadius..patchRadius step 2) {
                        val px = (cx + dx).coerceIn(0, width - 1)
                        val py = (cy + dy).coerceIn(0, height - 1)

                        val yIdx = py * yRowStride + px * yPixelStride
                        val uvIdx = (py / 2) * uvRowStride + (px / 2) * uvPixelStride

                        if (yIdx < yBuffer.limit() && uvIdx < uBuffer.limit() && uvIdx < vBuffer.limit()) {
                            val yVal = (yBuffer.get(yIdx).toInt() and 0xFF)
                            val uVal = (uBuffer.get(uvIdx).toInt() and 0xFF) - 128
                            val vVal = (vBuffer.get(uvIdx).toInt() and 0xFF) - 128

                            val r = (yVal + 1.370705f * vVal).toInt().coerceIn(0, 255)
                            val g = (yVal - 0.337633f * uVal - 0.698001f * vVal).toInt().coerceIn(0, 255)
                            val b = (yVal + 1.732446f * uVal).toInt().coerceIn(0, 255)

                            rSum += r
                            gSum += g
                            bSum += b
                            count++
                        }
                    }
                }

                val avgR = if (count > 0) (rSum / count).toInt() else 200
                val avgG = if (count > 0) (gSum / count).toInt() else 200
                val avgB = if (count > 0) (bSum / count).toInt() else 200

                // Map according to sensor rotation so grid matches what user sees on screen
                val (mappedRow, mappedCol) = when (rotation) {
                    90 -> col to (2 - row)
                    180 -> (2 - row) to (2 - col)
                    270 -> (2 - col) to row
                    else -> row to col
                }
                val idx = mappedRow * 3 + mappedCol
                rawRgb[idx] = AndroidColor.rgb(avgR, avgG, avgB)
                rawStickers[idx] = classifyRgb(avgR, avgG, avgB)
            }
        }

        return GridSampleResult(
            stickers = rawStickers,
            centerColor = rawStickers[4],
            sampledRgb = rawRgb
        )
    }

    fun classifyRgb(r: Int, g: Int, b: Int): CubeColor {
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]        // 0..360
        val sat = hsv[1]        // 0..1
        val value = hsv[2]      // 0..1

        // Low saturation + bright = White
        if (sat < 0.24f && value > 0.42f) {
            return CubeColor.WHITE
        }

        // Hue-based classification tuned for Rubik's stickers under varied lighting
        return when {
            hue < 14f || hue >= 338f -> CubeColor.RED
            hue in 14f..42f -> {
                // Distinguish Orange vs Yellow by hue & green channel ratio
                if (hue < 32f && (g < r * 0.78f)) CubeColor.ORANGE else CubeColor.YELLOW
            }
            hue in 42f..78f -> CubeColor.YELLOW
            hue in 78f..168f -> CubeColor.GREEN
            hue in 168f..265f -> CubeColor.BLUE
            else -> {
                // Fallback Euclidean RGB distance
                val targets = mapOf(
                    CubeColor.WHITE to Triple(240, 240, 240),
                    CubeColor.YELLOW to Triple(240, 215, 20),
                    CubeColor.RED to Triple(220, 40, 40),
                    CubeColor.ORANGE to Triple(240, 120, 20),
                    CubeColor.BLUE to Triple(30, 90, 220),
                    CubeColor.GREEN to Triple(20, 170, 90)
                )
                targets.minBy { (_, rgb) ->
                    abs(r - rgb.first) + abs(g - rgb.second) + abs(b - rgb.third)
                }.key
            }
        }
    }
}
