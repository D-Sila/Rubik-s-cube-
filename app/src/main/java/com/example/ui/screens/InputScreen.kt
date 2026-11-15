package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cube.CubeColor
import com.example.cube.CubeState
import com.example.cube.CubeValidationResult
import com.example.cube.Face
import com.example.cube.Move
import com.example.ui.CenterCubeUiState
import com.example.ui.InputSubMode
import com.example.ui.components.CameraScannerView
import com.example.ui.components.CubeNetPainter
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun InputScreen(
    state: CenterCubeUiState,
    onSelectSubMode: (InputSubMode) -> Unit,
    onSelectColor: (CubeColor) -> Unit,
    onPaintSticker: (Face, Int) -> Unit,
    onCaptureFace: (Face, List<CubeColor>) -> Unit,
    onApplyPreset: (CubeState, String) -> Unit,
    onSolveNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-mode switcher: 2D Net Painter vs Camera Center Scanner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface, RoundedCornerShape(14.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SubModePill(
                label = "2D Net Painter",
                icon = Icons.Rounded.GridView,
                selected = state.inputSubMode == InputSubMode.NET_PAINTER,
                onClick = { onSelectSubMode(InputSubMode.NET_PAINTER) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_net_painter")
            )
            SubModePill(
                label = "Camera Scanner",
                icon = Icons.Rounded.CameraAlt,
                selected = state.inputSubMode == InputSubMode.CAMERA_SCANNER,
                onClick = { onSelectSubMode(InputSubMode.CAMERA_SCANNER) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_camera_scanner")
            )
        }

        if (state.inputSubMode == InputSubMode.NET_PAINTER) {
            Text(
                text = "Tap any sticker to paint. Highlighted center boxes [U, F, R, L, B, D] lock each face's target color and determine the solving algorithm.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            CubeNetPainter(
                cube = state.cube,
                selectedColor = state.selectedPaintColor,
                onSelectColor = onSelectColor,
                onPaintSticker = onPaintSticker
            )

            // Quick Center-Orientation & Stage Presets
            Text(
                text = "Quick Center-Box Presets",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val flipped = CubeState.solved()
                            .rotateWholeX()
                            .rotateWholeX()
                            .applyMoves(Move.parseNotation("R U R' U R U2 R'"))
                        onApplyPreset(flipped, "Yellow Top Center · Sune Stage Loaded")
                    }
                ) {
                    Text("Yellow Top · Sune")
                }
                FilledTonalButton(
                    onClick = {
                        val blueTop = CubeState.solved()
                            .rotateWholeX()
                            .rotateWholeY()
                            .applyMoves(Move.parseNotation("F U R U' R' F'"))
                        onApplyPreset(blueTop, "Green/Red Center · OLL Cross Loaded")
                    }
                ) {
                    Text("Rotated · OLL Cross")
                }
                FilledTonalButton(
                    onClick = {
                        val custom = CubeState.solved(
                            u = CubeColor.BLUE,
                            d = CubeColor.GREEN,
                            f = CubeColor.YELLOW,
                            b = CubeColor.WHITE,
                            l = CubeColor.RED,
                            r = CubeColor.ORANGE
                        ).applyMoves(Move.parseNotation("U R U' R' U' F' U F"))
                        onApplyPreset(custom, "Custom Center Scheme · Middle Edge Loaded")
                    }
                ) {
                    Text("Custom Centers · Mid Edge")
                }
            }
        } else {
            CameraScannerView(
                cube = state.cube,
                scannedFaces = state.scannedFaces,
                onCaptureFace = onCaptureFace,
                onSwitchToPainter = { onSelectSubMode(InputSubMode.NET_PAINTER) }
            )
        }

        CenterAnalysisCard(
            validation = state.validation,
            onOpenInput = { onSelectSubMode(InputSubMode.NET_PAINTER) }
        )

        Button(
            onClick = onSolveNow,
            enabled = state.validation is CubeValidationResult.Valid,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanReticle,
                contentColor = ObsidianBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("solve_from_input_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = "Solve Cube"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.validation is CubeValidationResult.Valid) {
                    "Solve with Detected Center Boxes (${state.steps.size} moves)"
                } else {
                    "Complete Valid Cube to Solve"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SubModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CarbonElevated else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) CyanReticle.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) CyanReticle else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}
