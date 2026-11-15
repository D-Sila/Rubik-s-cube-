package com.example.cube

object AlgorithmCatalog {
    val items: List<AlgorithmCatalogItem> = listOf(
        AlgorithmCatalogItem(
            id = "center_cross",
            name = "Flipped Cross Insert",
            category = "Stage 1 · Base Cross",
            notation = "U' R' F R",
            centerBoxRule = "Triggered when a Base-color edge sits above Front with its side sticker matching the Front center box instead of the Base center box.",
            description = "Flips and slots a base cross edge into place without disturbing the other three solved cross edges.",
            setupInverseMoves = "R' F' R U"
        ),
        AlgorithmCatalogItem(
            id = "right_trigger",
            name = "Right Corner Trigger",
            category = "Stage 2 · First Layer",
            notation = "R U R' U'",
            centerBoxRule = "Triggered when the Base corner's Base-color sticker faces the Right center color box.",
            description = "The foundational 4-move trigger used to insert first-layer corners and cycle pieces cleanly.",
            setupInverseMoves = "U R U' R'"
        ),
        AlgorithmCatalogItem(
            id = "middle_right",
            name = "Middle Right Edge Insert",
            category = "Stage 3 · Middle Layer",
            notation = "U R U' R' U' F' U F",
            centerBoxRule = "Front sticker matches Front center box AND Top sticker matches Right center box → routes edge right.",
            description = "Inserts a top-layer edge into the Front-Right middle slot based on matching the Front and Right center boxes.",
            setupInverseMoves = "F' U' F U R U R' U'"
        ),
        AlgorithmCatalogItem(
            id = "middle_left",
            name = "Middle Left Edge Insert",
            category = "Stage 3 · Middle Layer",
            notation = "U' L' U L U F U' F'",
            centerBoxRule = "Front sticker matches Front center box AND Top sticker matches Left center box → routes edge left.",
            description = "Mirror algorithm that slots a top-layer edge into the Front-Left middle slot based on the Left center box.",
            setupInverseMoves = "F U F' U' L' U' L U"
        ),
        AlgorithmCatalogItem(
            id = "oll_fruruf",
            name = "OLL Top Cross (FRURUF)",
            category = "Stage 4 · Top Center Cross",
            notation = "F R U R' U' F'",
            centerBoxRule = "Top face shows only the Center Color Box (Dot), an L-shape at 9 o'clock, or a horizontal Line matching the Top center.",
            description = "Transforms Dot → L-Shape → Horizontal Line → Full Cross matching the Top center color box.",
            setupInverseMoves = "F U R U' R' F'"
        ),
        AlgorithmCatalogItem(
            id = "sune_oll",
            name = "Sune Edge Permutation",
            category = "Stage 5 · Top Edge Align",
            notation = "R U R' U R U2 R'",
            centerBoxRule = "Top cross matches Top center box, but only 2 side top-edges match their side center boxes.",
            description = "Cycles three top-layer edges while preserving the Front-Left corner and edge relationship.",
            setupInverseMoves = "R U2 R' U' R U' R'"
        ),
        AlgorithmCatalogItem(
            id = "niklas_pll",
            name = "Niklas 3-Corner Cycle",
            category = "Stage 6 · Corner Position",
            notation = "U R U' L' U R' U' L",
            centerBoxRule = "Top-Front-Right corner contains the 3 colors of the U, F, R center boxes; remaining 3 corners are out of position.",
            description = "Anchors the U-F-R corner and rotates the remaining three corners while keeping all top edges aligned to their center boxes.",
            setupInverseMoves = "L' U R U' L U R' U'"
        ),
        AlgorithmCatalogItem(
            id = "corner_twist",
            name = "Corner Orientation Twist",
            category = "Stage 7 · Final Twist",
            notation = "R' D' R D R' D' R D",
            centerBoxRule = "Corner is in its correct slot but its Top-color sticker doesn't match the Top center color box.",
            description = "Twists the U-F-R corner in place clockwise/counter-clockwise until its top sticker matches the Top center box.",
            setupInverseMoves = "D' R' D R D' R' D R U D' R' D R D' R' D R D' R' D R D' R' D R U'"
        )
    )
}
