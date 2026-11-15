package com.example.cube

object CenterAwareSolver {

    data class SolveOutput(
        val centerProfile: CenterProfile,
        val steps: List<SolveStep>,
        val rawMoveCount: Int,
        val simplifiedMoveCount: Int
    )

    private data class TaggedMove(
        val move: Move,
        val stage: SolveStage,
        val algorithmName: String,
        val formula: String,
        val centerReason: String
    )

    private val dEdgeIndices = intArrayOf(1, 5, 7, 3) // DF, DR, DB, DL on D
    private val uEdgeIndices = intArrayOf(7, 5, 1, 3) // UF, UR, UB, UL on U
    private val sideFaces = arrayOf(Face.F, Face.R, Face.B, Face.L)

    fun solve(initialCube: CubeState): Result<SolveOutput> {
        val validation = CubeValidator.validate(initialCube)
        if (validation is CubeValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException("${validation.title}: ${validation.reason}"))
        }
        val profile = (validation as CubeValidationResult.Valid).centerProfile
        if (initialCube.isSolved()) {
            return Result.success(
                SolveOutput(
                    centerProfile = profile,
                    steps = emptyList(),
                    rawMoveCount = 0,
                    simplifiedMoveCount = 0
                )
            )
        }

        val colorToFace = profile.centerColors.entries.associate { (face, color) -> color to face }

        // Check if within 4 moves using fast bidirectional BFS
        val shortMoves = findShortSolution(initialCube)
        if (shortMoves != null) {
            val tagged = shortMoves.map { m ->
                TaggedMove(
                    move = m,
                    stage = SolveStage.SHORT_OPTIMAL,
                    algorithmName = "Optimal Shortest Path",
                    formula = shortMoves.joinToString(" ") { it.notation },
                    centerReason = "Cube is ${shortMoves.size} moves from aligning all stickers with their ${profile.orientationLabel} center boxes."
                )
            }
            return Result.success(buildSolveOutput(initialCube, profile, tagged, tagged.size))
        }

        // Run deterministic 7-stage center-aware Layer-by-Layer solver
        val rawTagged = mutableListOf<TaggedMove>()
        var cube = initialCube

        fun emit(
            moves: List<Move>,
            stage: SolveStage,
            algorithmName: String,
            formula: String,
            reason: String
        ) {
            for (m in moves) {
                cube = cube.applyMove(m)
                rawTagged.add(
                    TaggedMove(
                        move = m,
                        stage = stage,
                        algorithmName = algorithmName,
                        formula = formula,
                        centerReason = reason
                    )
                )
            }
        }

        fun targetFace(color: CubeColor): Face = colorToFace[color]!!

        // -----------------------------------------------------------------
        // STAGE 1: Bottom (D) Cross aligned to D and side Center Color Boxes
        // -----------------------------------------------------------------
        val dCenterLabel = profile.bottomCenter.label
        for (k in 0 until 4) {
            val targetSide = sideFaces[k]
            val targetSideColor = cube.centerColor(targetSide)
            val dIdx = dEdgeIndices[k]

            fun isEdgeSolved(): Boolean =
                targetFace(cube.sticker(Face.D, dIdx)) == Face.D &&
                    targetFace(cube.sticker(targetSide, 7)) == targetSide

            if (isEdgeSolved()) continue

            // Locate the (D, targetSide) edge across all 12 edge slots
            val slot = findEdgeSlot(cube, colorToFace, Face.D, targetSide)
            if (slot in 4..7) {
                // In D layer (wrong slot or flipped): lift to U with S2
                val m = slot - 4
                val sMove = Move.fromFaceAndTurns(sideFaces[m], 2)!!
                emit(
                    moves = listOf(sMove),
                    stage = SolveStage.BOTTOM_CROSS,
                    algorithmName = "Lift Base Edge",
                    formula = sMove.notation,
                    reason = "Located $dCenterLabel-${targetSideColor.label} edge in ${sideFaces[m].symbol} base slot; lifting to Top (${profile.topCenter.label}) layer."
                )
            } else if (slot in 8..11) {
                // In middle layer: FR=8, RB=9? Careful! CubeValidator.edgeCoords: 8=FR, 9=FL, 10=BR, 11=BL
                val extractMoves = when (slot) {
                    8 -> listOf(Move.R, Move.U, Move.R_PRIME)
                    9 -> listOf(Move.L_PRIME, Move.U_PRIME, Move.L)
                    10 -> listOf(Move.R_PRIME, Move.U_PRIME, Move.R)
                    else -> listOf(Move.L, Move.U, Move.L_PRIME)
                }
                emit(
                    moves = extractMoves,
                    stage = SolveStage.BOTTOM_CROSS,
                    algorithmName = "Extract Middle Edge",
                    formula = extractMoves.joinToString(" ") { it.notation },
                    reason = "Popping $dCenterLabel-${targetSideColor.label} edge out of middle layer while preserving solved $dCenterLabel cross edges."
                )
            }

            // Now edge is guaranteed in U layer (slots 0..3)
            val uSlot = findEdgeSlot(cube, colorToFace, Face.D, targetSide)
            val uTurnsNeeded = ((uSlot - k) % 4 + 4) % 4
            // Wait: U cw (1 turn) moves F(0)->L(3)->B(2)->R(1)->F(0), so index decreases by 1 mod 4!
            // Let's compute exact U turns to move uSlot -> k:
            // Each U cw move changes slot: 0->3->2->1->0 (i.e. (slot + 3) % 4)
            // So from uSlot to k takes ((uSlot - k) % 4 + 4) % 4 cw turns!
            Move.fromFaceAndTurns(Face.U, uTurnsNeeded)?.let { uAlign ->
                emit(
                    moves = listOf(uAlign),
                    stage = SolveStage.BOTTOM_CROSS,
                    algorithmName = "Align Over Center Box",
                    formula = uAlign.notation,
                    reason = "Moving $dCenterLabel-${targetSideColor.label} edge above the ${targetSideColor.label} (${targetSide.symbol}) center color box."
                )
            }

            // Check orientation at U + targetSide
            val uSticker = cube.sticker(Face.U, uEdgeIndices[k])
            if (targetFace(uSticker) == Face.D) {
                val s2 = Move.fromFaceAndTurns(targetSide, 2)!!
                emit(
                    moves = listOf(s2),
                    stage = SolveStage.BOTTOM_CROSS,
                    algorithmName = "Direct Cross Drop",
                    formula = s2.notation,
                    reason = "Side sticker matches ${targetSideColor.label} center box; dropping directly into $dCenterLabel cross."
                )
            } else {
                // Flipped: U' R' F R rotated by k
                val flipInsert = rotateMovesY(
                    listOf(Move.U_PRIME, Move.R_PRIME, Move.F, Move.R),
                    k
                )
                emit(
                    moves = flipInsert,
                    stage = SolveStage.BOTTOM_CROSS,
                    algorithmName = "Flipped Cross Insert",
                    formula = flipInsert.joinToString(" ") { it.notation },
                    reason = "Edge is flipped relative to ${targetSideColor.label} center box; using rotated flip-insert to preserve solved cross edges."
                )
            }
        }

        // -----------------------------------------------------------------
        // STAGE 2: First Layer (D) Corners
        // -----------------------------------------------------------------
        // Corner indices in CubeValidator.cornerCoords:
        // U layer: 0=UFR, 1=UFL, 2=UBL, 3=UBR
        // D layer: 4=DFR, 5=DFL, 6=DBL, 7=DBR
        // Let's map slot k in 0..3 -> (DFR, DBR, DBL, DFL) = (4, 7, 6, 5)
        // And above slot k -> (UFR, UBR, UBL, UFL) = (0, 3, 2, 1)
        val dCornerSlotForK = intArrayOf(4, 7, 6, 5)
        val uCornerSlotForK = intArrayOf(0, 3, 2, 1)
        val dCornerTargetFaces = arrayOf(
            setOf(Face.D, Face.F, Face.R),
            setOf(Face.D, Face.R, Face.B),
            setOf(Face.D, Face.B, Face.L),
            setOf(Face.D, Face.L, Face.F)
        )

        for (k in 0 until 4) {
            val targetDSlot = dCornerSlotForK[k]
            val targetSet = dCornerTargetFaces[k]

            fun isCornerSolved(): Boolean {
                return CubeValidator.cornerCoords[targetDSlot].all { coord ->
                    targetFace(cube.sticker(coord.face, coord.index)) == coord.face
                }
            }
            if (isCornerSolved()) continue

            val currentSlot = findCornerSlot(cube, colorToFace, targetSet)
            if (currentSlot in 4..7) {
                val m = dCornerSlotForK.indexOf(currentSlot)
                val popMoves = rotateMovesY(listOf(Move.R, Move.U, Move.R_PRIME), m)
                emit(
                    moves = popMoves,
                    stage = SolveStage.FIRST_LAYER_CORNERS,
                    algorithmName = "Lift Base Corner",
                    formula = popMoves.joinToString(" ") { it.notation },
                    reason = "Extracting misplaced $dCenterLabel corner into Top layer for center-matched insertion."
                )
            }

            // Now corner is in U layer (0..3). Move it to uCornerSlotForK[k]
            val uSlot = findCornerSlot(cube, colorToFace, targetSet)
            // Under U cw: UBR(3)->UFR(0)->UFL(1)->UBL(2)->UBR(3)
            // Note our k order is k=0(UFR=0), k=1(UBR=3), k=2(UBL=2), k=3(UFL=1)
            // Under U cw: k=1 -> k=0 -> k=3 -> k=2 -> k=1 (k decreases by 1 mod 4!)
            val currentKAbove = uCornerSlotForK.indexOf(uSlot)
            val uTurnsNeeded = ((currentKAbove - k) % 4 + 4) % 4
            Move.fromFaceAndTurns(Face.U, uTurnsNeeded)?.let { uMove ->
                emit(
                    moves = listOf(uMove),
                    stage = SolveStage.FIRST_LAYER_CORNERS,
                    algorithmName = "Position Corner Over Slot",
                    formula = uMove.notation,
                    reason = "Aligning corner above its matching ${sideFaces[k].symbol}/${sideFaces[(k + 1) % 4].symbol} center color boxes."
                )
            }

            // Inspect orientation at uCornerSlotForK[k]: (U, R-side, F-side)
            val coords = CubeValidator.cornerCoords[uCornerSlotForK[k]]
            // coords[0] = U sticker, coords[1] = right-clockwise sticker (R for k=0), coords[2] = front-clockwise sticker (F for k=0)
            val baseInsert = when {
                targetFace(cube.sticker(coords[1].face, coords[1].index)) == Face.D ->
                    listOf(Move.R, Move.U, Move.R_PRIME)
                targetFace(cube.sticker(coords[2].face, coords[2].index)) == Face.D ->
                    listOf(Move.F_PRIME, Move.U_PRIME, Move.F)
                else ->
                    listOf(Move.R, Move.U2, Move.R_PRIME, Move.U_PRIME, Move.R, Move.U, Move.R_PRIME)
            }
            val insertMoves = rotateMovesY(baseInsert, k)
            emit(
                moves = insertMoves,
                stage = SolveStage.FIRST_LAYER_CORNERS,
                algorithmName = "Corner Trigger Insert",
                formula = insertMoves.joinToString(" ") { it.notation },
                reason = "Detected $dCenterLabel sticker facing ${if (baseInsert.size == 3) "side" else "top"}; running matching corner trigger."
            )
        }

        // -----------------------------------------------------------------
        // STAGE 3: Middle Layer Edges (FR=8, RB=10, BL=11, LF=9)
        // -----------------------------------------------------------------
        val midSlotForK = intArrayOf(8, 10, 11, 9)
        val rightInsertBase = listOf(Move.U, Move.R, Move.U_PRIME, Move.R_PRIME, Move.U_PRIME, Move.F_PRIME, Move.U, Move.F)
        val leftInsertBase = listOf(Move.U_PRIME, Move.L_PRIME, Move.U, Move.L, Move.U, Move.F, Move.U_PRIME, Move.F_PRIME)

        for (k in 0 until 4) {
            val fA = sideFaces[k]
            val fB = sideFaces[(k + 1) % 4]
            val midSlot = midSlotForK[k]

            fun isMidSolved(): Boolean =
                CubeValidator.edgeCoords[midSlot].all { coord ->
                    targetFace(cube.sticker(coord.face, coord.index)) == coord.face
                }
            if (isMidSolved()) continue

            val foundSlot = findEdgeSlot(cube, colorToFace, fA, fB)
            if (foundSlot >= 8) {
                val m = midSlotForK.indexOf(foundSlot)
                val popMid = rotateMovesY(rightInsertBase, m)
                emit(
                    moves = popMid,
                    stage = SolveStage.MIDDLE_LAYER_EDGES,
                    algorithmName = "Eject Middle Edge",
                    formula = popMid.joinToString(" ") { it.notation },
                    reason = "Ejecting trapped ${cube.centerColor(fA).label}-${cube.centerColor(fB).label} edge into Top layer."
                )
            }

            // Now edge is in U layer (0..3)
            val uSlot = findEdgeSlot(cube, colorToFace, fA, fB)
            // Check which color is on the side vs on U
            val uCoord = CubeValidator.edgeCoords[uSlot][0]
            val sideCoord = CubeValidator.edgeCoords[uSlot][1]
            val sideTarget = targetFace(cube.sticker(sideCoord.face, sideCoord.index))

            if (sideTarget == fA) {
                // Align side sticker over fA center box (uSlot -> k), then Right Insert from k
                val uTurns = ((uSlot - k) % 4 + 4) % 4
                Move.fromFaceAndTurns(Face.U, uTurns)?.let { uMove ->
                    emit(
                        moves = listOf(uMove),
                        stage = SolveStage.MIDDLE_LAYER_EDGES,
                        algorithmName = "Match Side Center Box",
                        formula = uMove.notation,
                        reason = "Aligning ${cube.centerColor(fA).label} edge sticker directly over the ${cube.centerColor(fA).label} center color box."
                    )
                }
                val ins = rotateMovesY(rightInsertBase, k)
                emit(
                    moves = ins,
                    stage = SolveStage.MIDDLE_LAYER_EDGES,
                    algorithmName = "Right Edge Insertion",
                    formula = ins.joinToString(" ") { it.notation },
                    reason = "Top sticker matches Right (${cube.centerColor(fB).label}) center box → Right Insertion Algorithm."
                )
            } else {
                // Side sticker matches fB! Align over fB center box (uSlot -> (k+1)%4), then Left Insert from (k+1)%4
                val kRight = (k + 1) % 4
                val uTurns = ((uSlot - kRight) % 4 + 4) % 4
                Move.fromFaceAndTurns(Face.U, uTurns)?.let { uMove ->
                    emit(
                        moves = listOf(uMove),
                        stage = SolveStage.MIDDLE_LAYER_EDGES,
                        algorithmName = "Match Side Center Box",
                        formula = uMove.notation,
                        reason = "Aligning ${cube.centerColor(fB).label} edge sticker directly over the ${cube.centerColor(fB).label} center color box."
                    )
                }
                val ins = rotateMovesY(leftInsertBase, kRight)
                emit(
                    moves = ins,
                    stage = SolveStage.MIDDLE_LAYER_EDGES,
                    algorithmName = "Left Edge Insertion",
                    formula = ins.joinToString(" ") { it.notation },
                    reason = "Top sticker matches Left (${cube.centerColor(fA).label}) center box → Left Insertion Algorithm."
                )
            }
        }

        // -----------------------------------------------------------------
        // STAGE 4: Top (U) Center Cross (F R U R' U' F')
        // -----------------------------------------------------------------
        val fruruf = listOf(Move.F, Move.R, Move.U, Move.R_PRIME, Move.U_PRIME, Move.F_PRIME)
        val uCenterLabel = profile.topCenter.label
        var guard = 0
        while (guard < 4) {
            guard++
            val up = targetFace(cube.sticker(Face.U, 1)) == Face.U
            val right = targetFace(cube.sticker(Face.U, 5)) == Face.U
            val down = targetFace(cube.sticker(Face.U, 7)) == Face.U
            val left = targetFace(cube.sticker(Face.U, 3)) == Face.U

            if (up && right && down && left) break

            val isLine = (left && right) || (up && down)
            if (isLine) {
                if (up && down) {
                    emit(
                        moves = listOf(Move.U),
                        stage = SolveStage.TOP_CROSS,
                        algorithmName = "Align OLL Line",
                        formula = "U",
                        reason = "Noticed horizontal/vertical $uCenterLabel center line; rotating U so line lies across Left–Right."
                    )
                }
                emit(
                    moves = fruruf,
                    stage = SolveStage.TOP_CROSS,
                    algorithmName = "FRURUF Line → Cross",
                    formula = "F R U R' U' F'",
                    reason = "Detected $uCenterLabel center box + horizontal line → Executing F R U R' U' F' to complete Top Cross."
                )
            } else if ((up && left) || (up && right) || (right && down) || (down && left)) {
                // Rotate L-shape so the two matching edges sit at Back (U[1]) and Left (U[3])
                val uAdjust = when {
                    up && left -> null
                    up && right -> Move.U_PRIME
                    right && down -> Move.U2
                    else -> Move.U
                }
                if (uAdjust != null) {
                    emit(
                        moves = listOf(uAdjust),
                        stage = SolveStage.TOP_CROSS,
                        algorithmName = "Position OLL L-Shape",
                        formula = uAdjust.notation,
                        reason = "Noticed $uCenterLabel L-shape around center box; positioning at 9 o'clock (Back–Left)."
                    )
                }
                emit(
                    moves = fruruf,
                    stage = SolveStage.TOP_CROSS,
                    algorithmName = "FRURUF L-Shape → Line",
                    formula = "F R U R' U' F'",
                    reason = "Detected $uCenterLabel L-shape around Top center box → Executing F R U R' U' F'."
                )
            } else {
                // Dot case (only center box matches)
                emit(
                    moves = fruruf,
                    stage = SolveStage.TOP_CROSS,
                    algorithmName = "FRURUF Dot → L-Shape",
                    formula = "F R U R' U' F'",
                    reason = "Only Top $uCenterLabel center box matches → Executing F R U R' U' F' to form L-shape."
                )
            }
        }

        // -----------------------------------------------------------------
        // STAGE 5: Align Top Cross Edges to Side Center Color Boxes (Sune)
        // -----------------------------------------------------------------
        val sune = listOf(Move.R, Move.U, Move.R_PRIME, Move.U, Move.R, Move.U2, Move.R_PRIME)
        var suneGuard = 0
        while (suneGuard < 4) {
            suneGuard++
            // Find U turn (0..3) that maximizes matching side edges
            var bestTurns = 0
            var bestMatches = -1
            for (t in 0..3) {
                val test = if (t == 0) cube else cube.applyMove(Move.fromFaceAndTurns(Face.U, t)!!)
                val matches = sideFaces.count { f -> targetFace(test.sticker(f, 1)) == f }
                if (matches > bestMatches) {
                    bestMatches = matches
                    bestTurns = t
                }
            }
            Move.fromFaceAndTurns(Face.U, bestTurns)?.let { uAlign ->
                emit(
                    moves = listOf(uAlign),
                    stage = SolveStage.TOP_CROSS_ALIGN,
                    algorithmName = "Maximize Center Matches",
                    formula = uAlign.notation,
                    reason = "Turning U to align $bestMatches/4 top edges with side center color boxes."
                )
            }
            val matched = sideFaces.map { f -> targetFace(cube.sticker(f, 1)) == f }
            if (matched.all { it }) break

            // matched indices: 0=F, 1=R, 2=B, 3=L
            // Sune (R U R' U R U2 R') preserves F? Let's check which edge Sune keeps fixed!
            // Actually let's check: R U R' U R U2 R' cycles 3 edges (UF, UL, UB or UR, UB, UL?):
            // Let's check F[1] under R U R' U R U2 R':
            // U_total = U + U + U2 = 4 turns = 0! Look: R moves UF->UB? No, R doesn't touch UF!
            // Wait: in R U R' U R U2 R':
            // 1. R: UR -> BR (off U)
            // 2. U: UF -> UL, UL -> UB, UB -> UR
            // 3. R': BR -> UR (wait: UF never touched R, so UF is now at UL; UB is now at UR -> wait, R' puts old UB->DR? No, old UR->BR->FR? Wait: R puts UR->BR, R' puts BR->UR! Wait: during R U R', UR went to BR, UB moved to UR, so R' moves UB from UR to FR!)
            // Better: in standard Sune, the edge that stays fixed is **FL? No, let's check which of {F, R, B, L} stays fixed under Sune + U!**
            // Wait: we can literally test the 4 y-rotations of Sune on `cube` right here in 0.01ms to pick the EXACT y-rotation that gets all 4 edges aligned (or turns opposite into adjacent)!
            val chosenRot = (0..3).firstOrNull { rot ->
                val candidate = cube.applyMoves(rotateMovesY(sune, rot))
                (0..3).any { uAfter ->
                    val aligned = if (uAfter == 0) candidate else candidate.applyMove(Move.fromFaceAndTurns(Face.U, uAfter)!!)
                    sideFaces.all { f -> targetFace(aligned.sticker(f, 1)) == f }
                }
            } ?: 0

            val suneMoves = rotateMovesY(sune, chosenRot)
            emit(
                moves = suneMoves,
                stage = SolveStage.TOP_CROSS_ALIGN,
                algorithmName = "Sune Edge Permutation",
                formula = suneMoves.joinToString(" ") { it.notation },
                reason = "Only 2 top edges match their side center boxes → Executing Sune to permute top edges."
            )
        }

        // -----------------------------------------------------------------
        // STAGE 6: Top Corners Position (Niklas: U R U' L' U R' U' L)
        // -----------------------------------------------------------------
        val niklas = listOf(Move.U, Move.R, Move.U_PRIME, Move.L_PRIME, Move.U, Move.R_PRIME, Move.U_PRIME, Move.L)
        var niklasGuard = 0
        while (niklasGuard < 4) {
            niklasGuard++
            // U corners in k order: 0=UFR, 3=UBR, 2=UBL, 1=UFL
            val uCornerSlots = intArrayOf(0, 3, 2, 1)
            val correctRot = uCornerSlots.indexOfFirst { slot ->
                val actualSet = CubeValidator.cornerCoords[slot].map { targetFace(cube.sticker(it.face, it.index)) }.toSet()
                val expectedSet = CubeValidator.cornerCoords[slot].map { it.face }.toSet()
                actualSet == expectedSet
            }
            val allPositioned = uCornerSlots.all { slot ->
                val actualSet = CubeValidator.cornerCoords[slot].map { targetFace(cube.sticker(it.face, it.index)) }.toSet()
                val expectedSet = CubeValidator.cornerCoords[slot].map { it.face }.toSet()
                actualSet == expectedSet
            }
            if (allPositioned) break

            val rot = if (correctRot >= 0) correctRot else 0
            val niklasMoves = rotateMovesY(niklas, rot)
            emit(
                moves = niklasMoves,
                stage = SolveStage.TOP_CORNERS_POSITION,
                algorithmName = "Niklas Corner 3-Cycle",
                formula = niklasMoves.joinToString(" ") { it.notation },
                reason = if (correctRot >= 0) {
                    "Anchored solved corner matching 3 center color boxes; cycling remaining 3 corners with Niklas."
                } else {
                    "No corner matches its 3 adjacent center boxes yet → Running Niklas to anchor one corner."
                }
            )
        }

        // -----------------------------------------------------------------
        // STAGE 7: Top Corners Orientation (R' D' R D) + Final U Align
        // -----------------------------------------------------------------
        val rdrd = listOf(Move.R_PRIME, Move.D_PRIME, Move.R, Move.D)
        for (cornerStep in 0 until 4) {
            var twistCount = 0
            while (targetFace(cube.sticker(Face.U, 8)) != Face.U && twistCount < 5) {
                twistCount++
                emit(
                    moves = rdrd,
                    stage = SolveStage.TOP_CORNERS_ORIENT,
                    algorithmName = "Corner Twist (R' D' R D)",
                    formula = "R' D' R D",
                    reason = "Corner sticker does not match Top $uCenterLabel center color box → Applying R' D' R D twist cycle."
                )
            }
            if (cornerStep < 3) {
                emit(
                    moves = listOf(Move.U),
                    stage = SolveStage.TOP_CORNERS_ORIENT,
                    algorithmName = "Advance Next Corner",
                    formula = "U",
                    reason = "Corner now matches Top $uCenterLabel center box; rotating U to bring next corner into U-F-R slot."
                )
            }
        }

        // Final U alignment so F[1] matches F center box
        for (t in 0..3) {
            if (cube.isSolved()) break
            if (t > 0) {
                val test = cube.applyMove(Move.fromFaceAndTurns(Face.U, t)!!)
                if (test.isSolved()) {
                    val uFinal = Move.fromFaceAndTurns(Face.U, t)!!
                    emit(
                        moves = listOf(uFinal),
                        stage = SolveStage.TOP_CORNERS_ORIENT,
                        algorithmName = "Final Layer Align",
                        formula = uFinal.notation,
                        reason = "Snapping Top layer into full alignment with all 6 center color boxes!"
                    )
                    break
                }
            }
        }

        return Result.success(buildSolveOutput(initialCube, profile, rawTagged, rawTagged.size))
    }

    private fun buildSolveOutput(
        initialCube: CubeState,
        profile: CenterProfile,
        rawTagged: List<TaggedMove>,
        rawCount: Int
    ): SolveOutput {
        val simplified = simplifyTaggedMoves(rawTagged)
        var runningCube = initialCube
        val steps = simplified.mapIndexed { idx, tm ->
            runningCube = runningCube.applyMove(tm.move)
            SolveStep(
                index = idx + 1,
                move = tm.move,
                stage = tm.stage,
                algorithmName = tm.algorithmName,
                formula = tm.formula,
                centerReason = tm.centerReason,
                resultingCube = runningCube
            )
        }
        return SolveOutput(
            centerProfile = profile,
            steps = steps,
            rawMoveCount = rawCount,
            simplifiedMoveCount = steps.size
        )
    }

    private fun simplifyTaggedMoves(input: List<TaggedMove>): List<TaggedMove> {
        val stack = mutableListOf<TaggedMove>()
        for (item in input) {
            if (stack.isNotEmpty() && stack.last().move.face == item.move.face) {
                val prev = stack.removeAt(stack.lastIndex)
                val combinedTurns = (prev.move.quarterTurns + item.move.quarterTurns) % 4
                val combinedMove = Move.fromFaceAndTurns(prev.move.face, combinedTurns)
                if (combinedMove != null) {
                    stack.add(item.copy(move = combinedMove))
                }
            } else {
                stack.add(item)
            }
        }
        return stack
    }

    private fun findEdgeSlot(
        cube: CubeState,
        colorToFace: Map<CubeColor, Face>,
        faceA: Face,
        faceB: Face
    ): Int {
        for (slot in CubeValidator.edgeCoords.indices) {
            val f0 = colorToFace[cube.sticker(CubeValidator.edgeCoords[slot][0].face, CubeValidator.edgeCoords[slot][0].index)]!!
            val f1 = colorToFace[cube.sticker(CubeValidator.edgeCoords[slot][1].face, CubeValidator.edgeCoords[slot][1].index)]!!
            if ((f0 == faceA && f1 == faceB) || (f0 == faceB && f1 == faceA)) {
                return slot
            }
        }
        error("Edge ($faceA, $faceB) not found on valid cube")
    }

    private fun findCornerSlot(
        cube: CubeState,
        colorToFace: Map<CubeColor, Face>,
        targetFaces: Set<Face>
    ): Int {
        for (slot in CubeValidator.cornerCoords.indices) {
            val actual = CubeValidator.cornerCoords[slot].map {
                colorToFace[cube.sticker(it.face, it.index)]!!
            }.toSet()
            if (actual == targetFaces) return slot
        }
        error("Corner $targetFaces not found on valid cube")
    }

    private fun rotateFaceY(face: Face, yTurns: Int): Face {
        var f = face
        repeat(((yTurns % 4) + 4) % 4) {
            f = when (f) {
                Face.F -> Face.R
                Face.R -> Face.B
                Face.B -> Face.L
                Face.L -> Face.F
                Face.U -> Face.U
                Face.D -> Face.D
            }
        }
        return f
    }

    private fun rotateMovesY(moves: List<Move>, yTurns: Int): List<Move> {
        val t = ((yTurns % 4) + 4) % 4
        if (t == 0) return moves
        return moves.map { m ->
            Move.fromFaceAndTurns(rotateFaceY(m.face, t), m.quarterTurns)!!
        }
    }

    /**
     * Bidirectional BFS up to depth 4 (covers 1..4 move states instantaneously).
     */
    private fun findShortSolution(start: CubeState): List<Move>? {
        val solved = CubeState.solved(
            u = start.centerColor(Face.U),
            d = start.centerColor(Face.D),
            f = start.centerColor(Face.F),
            b = start.centerColor(Face.B),
            l = start.centerColor(Face.L),
            r = start.centerColor(Face.R)
        )
        if (start == solved) return emptyList()

        val forward = HashMap<CubeState, List<Move>>()
        val fQueue = ArrayDeque<CubeState>()
        forward[start] = emptyList()
        fQueue.add(start)

        repeat(2) {
            val levelSize = fQueue.size
            repeat(levelSize) {
                val curr = fQueue.removeFirst()
                val path = forward[curr]!!
                for (move in Move.entries) {
                    if (path.isNotEmpty() && path.last().face == move.face) continue
                    val next = curr.applyMove(move)
                    if (next == solved) return path + move
                    if (next !in forward) {
                        forward[next] = path + move
                        fQueue.add(next)
                    }
                }
            }
        }

        val backward = HashMap<CubeState, List<Move>>()
        val bQueue = ArrayDeque<CubeState>()
        backward[solved] = emptyList()
        bQueue.add(solved)

        repeat(2) {
            val levelSize = bQueue.size
            repeat(levelSize) {
                val curr = bQueue.removeFirst()
                val path = backward[curr]!!
                for (move in Move.entries) {
                    if (path.isNotEmpty() && path.last().face == move.face) continue
                    val next = curr.applyMove(move)
                    val fwdMatch = forward[next]
                    if (fwdMatch != null) {
                        val backMoves = (path + move).reversed().map { it.inverse() }
                        return fwdMatch + backMoves
                    }
                    if (next !in backward) {
                        backward[next] = path + move
                        bQueue.add(next)
                    }
                }
            }
        }
        return null
    }
}
