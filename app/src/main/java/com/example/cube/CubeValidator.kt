package com.example.cube

object CubeValidator {

    internal data class StickerCoord(val face: Face, val index: Int)

    internal val cornerCoords = listOf(
        // U layer (clockwise from U sticker)
        listOf(StickerCoord(Face.U, 8), StickerCoord(Face.R, 0), StickerCoord(Face.F, 2)), // 0: UFR
        listOf(StickerCoord(Face.U, 6), StickerCoord(Face.F, 0), StickerCoord(Face.L, 2)), // 1: UFL
        listOf(StickerCoord(Face.U, 0), StickerCoord(Face.L, 0), StickerCoord(Face.B, 2)), // 2: UBL
        listOf(StickerCoord(Face.U, 2), StickerCoord(Face.B, 0), StickerCoord(Face.R, 2)), // 3: UBR
        // D layer (clockwise from D sticker)
        listOf(StickerCoord(Face.D, 2), StickerCoord(Face.F, 8), StickerCoord(Face.R, 6)), // 4: DFR
        listOf(StickerCoord(Face.D, 0), StickerCoord(Face.L, 8), StickerCoord(Face.F, 6)), // 5: DFL
        listOf(StickerCoord(Face.D, 6), StickerCoord(Face.B, 8), StickerCoord(Face.L, 6)), // 6: DBL
        listOf(StickerCoord(Face.D, 8), StickerCoord(Face.R, 8), StickerCoord(Face.B, 6))  // 7: DBR
    )

    internal val edgeCoords = listOf(
        listOf(StickerCoord(Face.U, 7), StickerCoord(Face.F, 1)), // 0: UF
        listOf(StickerCoord(Face.U, 5), StickerCoord(Face.R, 1)), // 1: UR
        listOf(StickerCoord(Face.U, 1), StickerCoord(Face.B, 1)), // 2: UB
        listOf(StickerCoord(Face.U, 3), StickerCoord(Face.L, 1)), // 3: UL
        listOf(StickerCoord(Face.D, 1), StickerCoord(Face.F, 7)), // 4: DF
        listOf(StickerCoord(Face.D, 5), StickerCoord(Face.R, 7)), // 5: DR
        listOf(StickerCoord(Face.D, 7), StickerCoord(Face.B, 7)), // 6: DB
        listOf(StickerCoord(Face.D, 3), StickerCoord(Face.L, 7)), // 7: DL
        listOf(StickerCoord(Face.F, 5), StickerCoord(Face.R, 3)), // 8: FR
        listOf(StickerCoord(Face.F, 3), StickerCoord(Face.L, 5)), // 9: FL
        listOf(StickerCoord(Face.B, 3), StickerCoord(Face.R, 5)), // 10: BR
        listOf(StickerCoord(Face.B, 5), StickerCoord(Face.L, 3))  // 11: BL
    )

    private val canonicalCornerFaces = cornerCoords.map { triple -> triple.map { it.face } }
    private val canonicalEdgeFaces = edgeCoords.map { pair -> pair.map { it.face } }

    fun validate(cube: CubeState): CubeValidationResult {
        val centers = Face.entries.associateWith { cube.centerColor(it) }

        // 1. Check all 6 center color boxes are unique
        val seenCenters = mutableMapOf<CubeColor, Face>()
        for (face in Face.entries) {
            val c = centers[face]!!
            val existing = seenCenters[c]
            if (existing != null) {
                return CubeValidationResult.Invalid(
                    title = "Duplicate Center Color Box",
                    reason = "${existing.displayName} and ${face.displayName} both have a ${c.label} center box. Each face must have a unique center color."
                )
            }
            seenCenters[c] = face
        }

        // 2. Check color counts (9 stickers per color)
        val counts = cube.stickers.groupingBy { it }.eachCount()
        for (color in CubeColor.entries) {
            val count = counts[color] ?: 0
            if (count != 9) {
                return CubeValidationResult.Invalid(
                    title = "Sticker Count Mismatch",
                    reason = "${color.label} appears $count times (needs exactly 9 stickers)."
                )
            }
        }

        // 3. Map every sticker color to its target face via its center color box
        val colorToFace = centers.entries.associate { (face, color) -> color to face }

        // 4. Validate corners (existence, cyclic orientation, twist sum % 3 == 0)
        val cornerPerm = IntArray(8)
        val cornerSeen = BooleanArray(8)
        var twistSum = 0

        for (slot in 0 until 8) {
            val mapped = cornerCoords[slot].map { coord -> colorToFace[cube.sticker(coord.face, coord.index)]!! }
            if (mapped.distinct().size != 3) {
                return CubeValidationResult.Invalid(
                    title = "Invalid Corner Piece",
                    reason = "Corner #${slot + 1} has duplicate colors (${mapped.joinToString { centers[it]!!.label }})."
                )
            }
            val matchIdx = canonicalCornerFaces.indexOfFirst { canon ->
                mapped.toSet() == canon.toSet()
            }
            if (matchIdx == -1) {
                return CubeValidationResult.Invalid(
                    title = "Impossible Corner Colors",
                    reason = "No corner piece can combine ${mapped.joinToString { centers[it]!!.label }} based on your center boxes."
                )
            }
            if (cornerSeen[matchIdx]) {
                return CubeValidationResult.Invalid(
                    title = "Duplicate Corner Piece",
                    reason = "The ${mapped.joinToString("-") { centers[it]!!.label }} corner appears more than once."
                )
            }
            val canon = canonicalCornerFaces[matchIdx]
            val shift = mapped.indexOf(canon[0])
            // Check cyclic order matches physical handedness
            if (mapped[(shift + 1) % 3] != canon[1] || mapped[(shift + 2) % 3] != canon[2]) {
                return CubeValidationResult.Invalid(
                    title = "Mirrored Corner Stickers",
                    reason = "The ${mapped.joinToString("-") { centers[it]!!.label }} corner has swapped sticker orientation. Check two adjacent stickers on this corner."
                )
            }
            cornerSeen[matchIdx] = true
            cornerPerm[slot] = matchIdx
            twistSum += shift
        }

        if (twistSum % 3 != 0) {
            return CubeValidationResult.Invalid(
                title = "Corner Twist Parity Error",
                reason = "One corner is twisted relative to the center boxes (twist sum $twistSum). Twist one corner or swap two stickers on a corner."
            )
        }

        // 5. Validate edges (existence, flip sum % 2 == 0)
        val edgePerm = IntArray(12)
        val edgeSeen = BooleanArray(12)
        var flipSum = 0

        for (slot in 0 until 12) {
            val mapped = edgeCoords[slot].map { coord -> colorToFace[cube.sticker(coord.face, coord.index)]!! }
            if (mapped[0] == mapped[1]) {
                return CubeValidationResult.Invalid(
                    title = "Invalid Edge Piece",
                    reason = "Edge #${slot + 1} has two ${centers[mapped[0]]!!.label} stickers."
                )
            }
            val matchIdx = canonicalEdgeFaces.indexOfFirst { canon ->
                (mapped[0] == canon[0] && mapped[1] == canon[1]) ||
                    (mapped[0] == canon[1] && mapped[1] == canon[0])
            }
            if (matchIdx == -1) {
                return CubeValidationResult.Invalid(
                    title = "Impossible Edge Colors",
                    reason = "Opposite center colors (${centers[mapped[0]]!!.label} & ${centers[mapped[1]]!!.label}) cannot share an edge."
                )
            }
            if (edgeSeen[matchIdx]) {
                return CubeValidationResult.Invalid(
                    title = "Duplicate Edge Piece",
                    reason = "The ${centers[mapped[0]]!!.label}-${centers[mapped[1]]!!.label} edge appears twice."
                )
            }
            edgeSeen[matchIdx] = true
            edgePerm[slot] = matchIdx
            if (mapped[0] != canonicalEdgeFaces[matchIdx][0]) {
                flipSum += 1
            }
        }

        if (flipSum % 2 != 0) {
            return CubeValidationResult.Invalid(
                title = "Edge Flip Parity Error",
                reason = "An odd number of edges are flipped relative to the center boxes. Flip one edge piece to make the cube solvable."
            )
        }

        // 6. Permutation parity check
        val cornerParity = permutationParity(cornerPerm)
        val edgeParity = permutationParity(edgePerm)
        if (cornerParity != edgeParity) {
            return CubeValidationResult.Invalid(
                title = "Permutation Parity Error",
                reason = "Two edges or two corners are swapped (impossible without reassembling). Swap two pieces to restore parity."
            )
        }

        val profile = buildCenterProfile(cube, centers, colorToFace)
        return CubeValidationResult.Valid(profile)
    }

    fun buildCenterProfile(
        cube: CubeState,
        centers: Map<Face, CubeColor>,
        colorToFace: Map<CubeColor, Face>
    ): CenterProfile {
        val uColor = centers[Face.U]!!
        val dColor = centers[Face.D]!!
        val fColor = centers[Face.F]!!
        val bColor = centers[Face.B]!!
        val lColor = centers[Face.L]!!
        val rColor = centers[Face.R]!!

        val standardPairs = setOf(
            setOf(CubeColor.WHITE, CubeColor.YELLOW),
            setOf(CubeColor.RED, CubeColor.ORANGE),
            setOf(CubeColor.BLUE, CubeColor.GREEN)
        )
        val actualPairs = setOf(
            setOf(uColor, dColor),
            setOf(fColor, bColor),
            setOf(lColor, rColor)
        )

        val orientationLabel = "${uColor.label} Top · ${fColor.label} Front"
        val schemeNote = when {
            uColor == CubeColor.WHITE && fColor == CubeColor.GREEN && actualPairs == standardPairs ->
                "Standard WCA orientation detected from center color boxes. Base cross targets ${dColor.label} (${Face.D.symbol})."
            actualPairs == standardPairs ->
                "Rotated standard cube detected via center color boxes (${uColor.label} on U, ${dColor.label} on D). Algorithms adapted automatically to your held center colors."
            else ->
                "Custom center color layout detected (${uColor.label}↔${dColor.label}, ${fColor.label}↔${bColor.label}, ${lColor.label}↔${rColor.label}). Solver maps all moves dynamically by center box."
        }

        val detectedStage = detectCurrentStage(cube, colorToFace)
        val strategy = when (detectedStage) {
            SolveStage.SOLVED -> "All 54 stickers match their face's center color box."
            SolveStage.BOTTOM_CROSS -> "Missing ${dColor.label} base cross edges → Stage 1 Center-Aligned Cross + full Layer-by-Layer CFOP sequence."
            SolveStage.FIRST_LAYER_CORNERS -> "${dColor.label} cross matches centers → Stage 2 Corner Triggers (R U R' / F' U' F) selected."
            SolveStage.MIDDLE_LAYER_EDGES -> "${dColor.label} base layer matches centers → Stage 3 Middle Edge Insertion (U R U' R' U' F' U F) selected."
            SolveStage.TOP_CROSS -> "First two layers match centers → Stage 4 ${uColor.label} Top Center Cross (F R U R' U' F') selected."
            SolveStage.TOP_CROSS_ALIGN -> "${uColor.label} top cross formed → Stage 5 Sune Edge Permutation (R U R' U R U2 R') selected."
            SolveStage.TOP_CORNERS_POSITION -> "${uColor.label} cross & side edges aligned → Stage 6 Niklas Corner Cycle (U R U' L' U R' U' L) selected."
            SolveStage.TOP_CORNERS_ORIENT -> "All corners positioned relative to centers → Stage 7 R' D' R D Twist selected."
            SolveStage.SHORT_OPTIMAL -> "Near-solved cube → Optimal Shortest Path selected."
        }

        return CenterProfile(
            centerColors = centers,
            orientationLabel = orientationLabel,
            schemeNote = schemeNote,
            detectedStage = detectedStage,
            recommendedStrategy = strategy,
            topCenter = uColor,
            frontCenter = fColor,
            bottomCenter = dColor
        )
    }

    internal fun detectCurrentStage(cube: CubeState, colorToFace: Map<CubeColor, Face>): SolveStage {
        if (cube.isSolved()) return SolveStage.SOLVED

        fun matches(face: Face, idx: Int): Boolean =
            colorToFace[cube.sticker(face, idx)] == face

        val dCrossDone = matches(Face.D, 1) && matches(Face.F, 7) &&
            matches(Face.D, 5) && matches(Face.R, 7) &&
            matches(Face.D, 7) && matches(Face.B, 7) &&
            matches(Face.D, 3) && matches(Face.L, 7)
        if (!dCrossDone) return SolveStage.BOTTOM_CROSS

        val dCornersDone = (0..8).all { matches(Face.D, it) } &&
            matches(Face.F, 6) && matches(Face.F, 8) &&
            matches(Face.R, 6) && matches(Face.R, 8) &&
            matches(Face.B, 6) && matches(Face.B, 8) &&
            matches(Face.L, 6) && matches(Face.L, 8)
        if (!dCornersDone) return SolveStage.FIRST_LAYER_CORNERS

        val midDone = matches(Face.F, 3) && matches(Face.F, 5) &&
            matches(Face.R, 3) && matches(Face.R, 5) &&
            matches(Face.B, 3) && matches(Face.B, 5) &&
            matches(Face.L, 3) && matches(Face.L, 5)
        if (!midDone) return SolveStage.MIDDLE_LAYER_EDGES

        val uCrossDone = matches(Face.U, 1) && matches(Face.U, 3) &&
            matches(Face.U, 5) && matches(Face.U, 7)
        if (!uCrossDone) return SolveStage.TOP_CROSS

        val uEdgesAligned = matches(Face.F, 1) && matches(Face.R, 1) &&
            matches(Face.B, 1) && matches(Face.L, 1)
        if (!uEdgesAligned) return SolveStage.TOP_CROSS_ALIGN

        val uCornersPositioned = listOf(0, 1, 2, 3).all { slot ->
            val actualSet = cornerCoords[slot].map { colorToFace[cube.sticker(it.face, it.index)]!! }.toSet()
            val expectedSet = cornerCoords[slot].map { it.face }.toSet()
            actualSet == expectedSet
        }
        if (!uCornersPositioned) return SolveStage.TOP_CORNERS_POSITION

        return SolveStage.TOP_CORNERS_ORIENT
    }

    private fun permutationParity(perm: IntArray): Int {
        var inversions = 0
        for (i in perm.indices) {
            for (j in i + 1 until perm.size) {
                if (perm[i] > perm[j]) inversions++
            }
        }
        return inversions % 2
    }
}
