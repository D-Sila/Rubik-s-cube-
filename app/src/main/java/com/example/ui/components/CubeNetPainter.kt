package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.cube.CubeColor
import com.example.cube.CubeState
import com.example.cube.Face
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextSecondary

@Composable
fun CubeNetPainter(
    cube: CubeState,
    selectedColor: CubeColor,
    onSelectColor: (CubeColor) -> Unit,
    onPaintSticker: (Face, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorCounts = CubeColor.entries.associateWith { color ->
        cube.stickers.count { it == color }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cross-shaped 2D Unfolded Net:
        //      [U]
        // [L]  [F]  [R]  [B]
        //      [D]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface, RoundedCornerShape(20.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: [empty] [U] [empty] [empty]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                FaceGrid3x3(
                    face = Face.U,
                    cube = cube,
                    onTapCell = { idx -> onPaintSticker(Face.U, idx) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }

            // Row 2: [L] [F] [R] [B]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(Face.L, Face.F, Face.R, Face.B).forEach { face ->
                    FaceGrid3x3(
                        face = face,
                        cube = cube,
                        onTapCell = { idx -> onPaintSticker(face, idx) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 3: [empty] [D] [empty] [empty]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                FaceGrid3x3(
                    face = Face.D,
                    cube = cube,
                    onTapCell = { idx -> onPaintSticker(Face.D, idx) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Color Palette Picker with live 9/9 count badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CubeColor.entries.forEach { color ->
                val isSelected = color == selectedColor
                val count = colorCounts[color] ?: 0
                val countOk = count == 9
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectColor(color) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("palette_color_${color.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.composeColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) CyanReticle else Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = color.shortCode.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color.onStickerColor
                        )
                    }
                    Text(
                        text = "$count/9",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (countOk) TextSecondary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FaceGrid3x3(
    face: Face,
    cube: CubeState,
    onTapCell: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val stickerColor = cube.sticker(face, idx)
                    val isCenterBox = idx == 4

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(stickerColor.composeColor)
                            .border(
                                width = if (isCenterBox) 2.dp else 0.75.dp,
                                color = if (isCenterBox) CyanReticle else Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onTapCell(idx) }
                            .testTag("net_${face.symbol}_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCenterBox) {
                            Text(
                                text = face.symbol.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = stickerColor.onStickerColor
                            )
                        }
                    }
                }
            }
        }
    }
}
