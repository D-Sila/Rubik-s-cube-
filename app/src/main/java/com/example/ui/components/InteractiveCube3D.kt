package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cube.CubeState
import com.example.cube.Face
import com.example.cube.Move
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CubeBodyPlastic
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class RenderQuad(
    val points: List<Offset>,
    val depth: Float,
    val stickerColor: Color,
    val onStickerColor: Color,
    val face: Face,
    val stickerIndex: Int,
    val isHighlightedSlice: Boolean,
    val arrowTail: Offset? = null,
    val arrowTip: Offset? = null,
    val isDoubleTurnArrow: Boolean = false
)

@Composable
fun InteractiveCube3D(
    cube: CubeState,
    activeMove: Move?,
    moveProgress: Float,
    modifier: Modifier = Modifier,
    guideMove: Move? = null,
    showMoveArrows: Boolean = true,
    onToggleMoveArrows: (() -> Unit)? = null,
    highlightCenters: Boolean = true
) {
    var yawDeg by remember { mutableFloatStateOf(-36f) }
    var pitchDeg by remember { mutableFloatStateOf(26f) }
    val textMeasurer = rememberTextMeasurer()

    val effectiveGuideMove = activeMove ?: guideMove
    val pulseTransition = rememberInfiniteTransition(label = "arrowPulse")
    val arrowPulse by pulseTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowPulseAlpha"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF172033), Color(0xFF0B0F17))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(1.dp, SlateBorder, RoundedCornerShape(24.dp))
            .testTag("cube_3d_viewport")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yawDeg += dragAmount.x * 0.45f
                        pitchDeg = (pitchDeg + dragAmount.y * 0.45f).coerceIn(-80f, 80f)
                    }
                }
        ) {
            val cubeScale = size.minDimension * 0.19f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val camDist = 11.5f

            val yawRad = (yawDeg * PI / 180.0).toFloat()
            val pitchRad = (pitchDeg * PI / 180.0).toFloat()

            fun rotateCamera(v: Vec3): Vec3 {
                val cy = cos(yawRad)
                val sy = sin(yawRad)
                val x1 = v.x * cy + v.z * sy
                val z1 = -v.x * sy + v.z * cy

                val cp = cos(pitchRad)
                val sp = sin(pitchRad)
                val y2 = v.y * cp - z1 * sp
                val z2 = v.y * sp + z1 * cp
                return Vec3(x1, y2, z2)
            }

            fun project(v: Vec3): Offset {
                val factor = camDist / (camDist - v.z).coerceAtLeast(2f)
                return Offset(
                    x = centerX + v.x * cubeScale * factor,
                    y = centerY - v.y * cubeScale * factor
                )
            }

            fun baseSignForFace(face: Face): Float = when (face) {
                Face.R -> -1f
                Face.L -> 1f
                Face.U -> -1f
                Face.D -> 1f
                Face.F -> -1f
                Face.B -> 1f
            }

            val sliceAngleRad = if (activeMove != null && moveProgress > 0f) {
                val dirTurns = when (activeMove.quarterTurns) {
                    1 -> 1f
                    2 -> 2f
                    3 -> -1f
                    else -> 0f
                }
                (dirTurns * baseSignForFace(activeMove.face) * moveProgress * (PI / 2.0)).toFloat()
            } else {
                0f
            }

            fun belongsToSlice(vCenter: Vec3, face: Face, moveFace: Face): Boolean {
                return when (moveFace) {
                    Face.R -> face == Face.R || vCenter.x > 0.45f
                    Face.L -> face == Face.L || vCenter.x < -0.45f
                    Face.U -> face == Face.U || vCenter.y > 0.45f
                    Face.D -> face == Face.D || vCenter.y < -0.45f
                    Face.F -> face == Face.F || vCenter.z > 0.45f
                    Face.B -> face == Face.B || vCenter.z < -0.45f
                }
            }

            fun rotateSlice(v: Vec3, moveFace: Face, angle: Float): Vec3 {
                val c = cos(angle)
                val s = sin(angle)
                return when (moveFace) {
                    Face.R, Face.L -> Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c)
                    Face.U, Face.D -> Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
                    Face.F, Face.B -> Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z)
                }
            }

            fun computeInPlaneMoveArrow3D(
                tileCenter: Vec3,
                face: Face,
                move: Move
            ): Pair<Vec3, Vec3>? {
                val dirSign = if (move.quarterTurns == 3) -1f else 1f
                val smallAngle = 0.08f * dirSign * baseSignForFace(move.face)
                val stepped = rotateSlice(tileCenter, move.face, smallAngle)
                val dx = stepped.x - tileCenter.x
                val dy = stepped.y - tileCenter.y
                val dz = stepped.z - tileCenter.z

                // Project tangent strictly onto the sticker's face plane
                val planeVec = when (face) {
                    Face.F, Face.B -> Vec3(dx, dy, 0f)
                    Face.R, Face.L -> Vec3(0f, dy, dz)
                    Face.U, Face.D -> Vec3(dx, 0f, dz)
                }
                val mag = hypot(
                    hypot(planeVec.x.toDouble(), planeVec.y.toDouble()),
                    planeVec.z.toDouble()
                ).toFloat()
                if (mag < 0.0001f) return null

                val ux = planeVec.x / mag
                val uy = planeVec.y / mag
                val uz = planeVec.z / mag
                val halfLen = 0.27f

                val tail = Vec3(
                    tileCenter.x - ux * halfLen,
                    tileCenter.y - uy * halfLen,
                    tileCenter.z - uz * halfLen
                )
                val tip = Vec3(
                    tileCenter.x + ux * halfLen,
                    tileCenter.y + uy * halfLen,
                    tileCenter.z + uz * halfLen
                )
                return tail to tip
            }

            val quads = ArrayList<RenderQuad>(54)

            for (face in Face.entries) {
                for (idx in 0 until 9) {
                    val row = idx / 3
                    val col = idx % 3
                    val rawCorners = tileCorners3D(face, row, col)
                    val tileCenter = Vec3(
                        x = rawCorners.sumOf { it.x.toDouble() }.toFloat() / 4f,
                        y = rawCorners.sumOf { it.y.toDouble() }.toFloat() / 4f,
                        z = rawCorners.sumOf { it.z.toDouble() }.toFloat() / 4f
                    )

                    val inActiveAnimSlice = activeMove != null && belongsToSlice(tileCenter, face, activeMove.face)
                    val inGuideSlice = showMoveArrows &&
                        effectiveGuideMove != null &&
                        belongsToSlice(tileCenter, face, effectiveGuideMove.face)
                    val isMovingNonCenterBox = inGuideSlice && !(face == effectiveGuideMove!!.face && idx == 4)

                    val transformedCorners = rawCorners.map { corner ->
                        val sliceRotated = if (inActiveAnimSlice && sliceAngleRad != 0f) {
                            rotateSlice(corner, activeMove!!.face, sliceAngleRad)
                        } else {
                            corner
                        }
                        rotateCamera(sliceRotated)
                    }

                    val p0 = transformedCorners[0]
                    val p1 = transformedCorners[1]
                    val p2 = transformedCorners[2]
                    val ux = p1.x - p0.x
                    val uy = p1.y - p0.y
                    val vx = p2.x - p0.x
                    val vy = p2.y - p0.y
                    val crossZ = ux * vy - uy * vx
                    if (crossZ <= 0.0005f) continue

                    val projected = transformedCorners.map(::project)
                    val avgDepth = transformedCorners.sumOf { it.z.toDouble() }.toFloat() / 4f
                    val color = cube.sticker(face, idx)

                    var projectedTail: Offset? = null
                    var projectedTip: Offset? = null
                    if (isMovingNonCenterBox) {
                        val arrow3D = computeInPlaneMoveArrow3D(tileCenter, face, effectiveGuideMove!!)
                        if (arrow3D != null) {
                            val (rawTail, rawTip) = arrow3D
                            val animTail = if (inActiveAnimSlice && sliceAngleRad != 0f) {
                                rotateSlice(rawTail, activeMove!!.face, sliceAngleRad)
                            } else {
                                rawTail
                            }
                            val animTip = if (inActiveAnimSlice && sliceAngleRad != 0f) {
                                rotateSlice(rawTip, activeMove!!.face, sliceAngleRad)
                            } else {
                                rawTip
                            }
                            projectedTail = project(rotateCamera(animTail))
                            projectedTip = project(rotateCamera(animTip))
                        }
                    }

                    quads.add(
                        RenderQuad(
                            points = projected,
                            depth = avgDepth,
                            stickerColor = color.composeColor,
                            onStickerColor = color.onStickerColor,
                            face = face,
                            stickerIndex = idx,
                            isHighlightedSlice = inActiveAnimSlice || isMovingNonCenterBox,
                            arrowTail = projectedTail,
                            arrowTip = projectedTip,
                            isDoubleTurnArrow = effectiveGuideMove?.quarterTurns == 2
                        )
                    )
                }
            }

            quads.sortBy { it.depth }

            for (quad in quads) {
                drawStickerQuad(
                    quad = quad,
                    highlightCenters = highlightCenters,
                    arrowAlpha = arrowPulse,
                    textMeasurer = textMeasurer
                )
            }
        }

        // Top-left Center tracking badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(CarbonSurface.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CenterFocusStrong,
                contentDescription = "Center Box Tracking",
                tint = CyanReticle,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Drag to orbit · Centers locked",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        // Top-right Move Arrows toggle chip
        if (onToggleMoveArrows != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (showMoveArrows) CyanContainer.copy(alpha = 0.9f)
                        else CarbonSurface.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (showMoveArrows) CyanReticle else SlateBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onToggleMoveArrows)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("toggle_arrows_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DoubleArrow,
                    contentDescription = "Toggle Move Arrows",
                    tint = if (showMoveArrows) CyanReticle else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (showMoveArrows) "Arrows ON" else "Arrows OFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (showMoveArrows) TextPrimary else TextSecondary
                )
            }
        }

        // Bottom-left active guide badge showing which move the arrows represent
        if (showMoveArrows && effectiveGuideMove != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(CarbonElevated.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                    .border(1.dp, CyanReticle.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("arrow_guide_badge"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DoubleArrow,
                    contentDescription = "Moving Boxes Direction",
                    tint = CyanReticle,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Next ${effectiveGuideMove.notation}: Arrows show boxes to move",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun DrawScope.drawStickerQuad(
    quad: RenderQuad,
    highlightCenters: Boolean,
    arrowAlpha: Float,
    textMeasurer: TextMeasurer
) {
    val pts = quad.points
    val outerPath = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        lineTo(pts[1].x, pts[1].y)
        lineTo(pts[2].x, pts[2].y)
        lineTo(pts[3].x, pts[3].y)
        close()
    }

    drawPath(outerPath, color = CubeBodyPlastic)
    drawPath(
        outerPath,
        color = if (quad.isHighlightedSlice) CyanReticle.copy(alpha = 0.85f) else Color(0xFF06080D),
        style = Stroke(width = if (quad.isHighlightedSlice) 3.0f else 2.2f)
    )

    val cx = pts.sumOf { it.x.toDouble() }.toFloat() / 4f
    val cy = pts.sumOf { it.y.toDouble() }.toFloat() / 4f
    val inset = 0.85f
    val innerPts = pts.map { p ->
        Offset(cx + (p.x - cx) * inset, cy + (p.y - cy) * inset)
    }
    val innerPath = Path().apply {
        moveTo(innerPts[0].x, innerPts[0].y)
        lineTo(innerPts[1].x, innerPts[1].y)
        lineTo(innerPts[2].x, innerPts[2].y)
        lineTo(innerPts[3].x, innerPts[3].y)
        close()
    }

    drawPath(innerPath, color = quad.stickerColor)

    drawPath(
        innerPath,
        color = if (quad.isHighlightedSlice) CyanReticle.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.08f),
        style = Stroke(width = if (quad.isHighlightedSlice) 2.2f else 1.4f)
    )

    if (highlightCenters && quad.stickerIndex == 4) {
        val bracketPts = pts.map { p ->
            Offset(cx + (p.x - cx) * 0.70f, cy + (p.y - cy) * 0.70f)
        }
        val bracketPath = Path().apply {
            moveTo(bracketPts[0].x, bracketPts[0].y)
            lineTo(bracketPts[1].x, bracketPts[1].y)
            lineTo(bracketPts[2].x, bracketPts[2].y)
            lineTo(bracketPts[3].x, bracketPts[3].y)
            close()
        }
        drawPath(
            bracketPath,
            color = quad.onStickerColor.copy(alpha = 0.5f),
            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )

        val measured = textMeasurer.measure(
            text = quad.face.symbol.toString(),
            style = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = quad.onStickerColor
            )
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                cx - measured.size.width / 2f,
                cy - measured.size.height / 2f
            )
        )
    }

    // Draw high-contrast directional movement arrow on boxes that need to move
    if (quad.arrowTail != null && quad.arrowTip != null) {
        drawMoveArrow(
            tail = quad.arrowTail,
            tip = quad.arrowTip,
            isDoubleTurn = quad.isDoubleTurnArrow,
            alpha = arrowAlpha
        )
    }
}

private fun DrawScope.drawMoveArrow(
    tail: Offset,
    tip: Offset,
    isDoubleTurn: Boolean,
    alpha: Float
) {
    val dx = tip.x - tail.x
    val dy = tip.y - tail.y
    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (len < 3f) return

    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux

    val wingLen = (len * 0.45f).coerceIn(5.5f, 12f)
    val wingSpread = wingLen * 0.65f

    val arrowPath = Path().apply {
        moveTo(tail.x, tail.y)
        lineTo(tip.x, tip.y)
        moveTo(
            tip.x - ux * wingLen + px * wingSpread,
            tip.y - uy * wingLen + py * wingSpread
        )
        lineTo(tip.x, tip.y)
        lineTo(
            tip.x - ux * wingLen - px * wingSpread,
            tip.y - uy * wingLen - py * wingSpread
        )

        if (isDoubleTurn) {
            val backTipX = tip.x - ux * (wingLen * 0.68f)
            val backTipY = tip.y - uy * (wingLen * 0.68f)
            moveTo(
                backTipX - ux * wingLen + px * wingSpread,
                backTipY - uy * wingLen + py * wingSpread
            )
            lineTo(backTipX, backTipY)
            lineTo(
                backTipX - ux * wingLen - px * wingSpread,
                backTipY - uy * wingLen - py * wingSpread
            )
        }
    }

    // Dark halo for 100% contrast on White, Yellow, Orange, Red, Blue, and Green stickers
    drawPath(
        path = arrowPath,
        color = Color(0xEB070A10).copy(alpha = alpha),
        style = Stroke(width = 6.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Bright core arrow
    drawPath(
        path = arrowPath,
        color = Color.White.copy(alpha = alpha),
        style = Stroke(width = 3.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun tileCorners3D(face: Face, row: Int, col: Int): List<Vec3> {
    val u0 = col - 1.5f
    val u1 = u0 + 1f
    val v0 = 1.5f - row
    val v1 = v0 - 1f
    val d = 1.5f

    return when (face) {
        Face.F -> listOf(
            Vec3(u0, v0, d),
            Vec3(u0, v1, d),
            Vec3(u1, v1, d),
            Vec3(u1, v0, d)
        )
        Face.B -> listOf(
            Vec3(-u0, v0, -d),
            Vec3(-u0, v1, -d),
            Vec3(-u1, v1, -d),
            Vec3(-u1, v0, -d)
        )
        Face.R -> listOf(
            Vec3(d, v0, -u0),
            Vec3(d, v1, -u0),
            Vec3(d, v1, -u1),
            Vec3(d, v0, -u1)
        )
        Face.L -> listOf(
            Vec3(-d, v0, u0),
            Vec3(-d, v1, u0),
            Vec3(-d, v1, u1),
            Vec3(-d, v0, u1)
        )
        Face.U -> {
            val z0 = row - 1.5f
            val z1 = z0 + 1f
            listOf(
                Vec3(u0, d, z0),
                Vec3(u0, d, z1),
                Vec3(u1, d, z1),
                Vec3(u1, d, z0)
            )
        }
        Face.D -> {
            val z0 = 1.5f - row
            val z1 = z0 - 1f
            listOf(
                Vec3(u0, -d, z0),
                Vec3(u0, -d, z1),
                Vec3(u1, -d, z1),
                Vec3(u1, -d, z0)
            )
        }
    }
}
