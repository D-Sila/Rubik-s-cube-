package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cube.AlgorithmCatalog
import com.example.cube.AlgorithmCatalogItem
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AlgorithmsScreen(
    onTryAlgorithm: (AlgorithmCatalogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CenterFocusStrong,
                        contentDescription = "Center Color Box Rules",
                        tint = CyanReticle
                    )
                    Text(
                        text = "How Center Boxes Choose Algorithms",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Because center stickers never move relative to each other, CenterCube reads all 6 center boxes first to lock your orientation, then checks surrounding edges and corners against those centers to pick the exact formula below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        items(AlgorithmCatalog.items, key = { it.id }) { item ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CarbonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(18.dp))
                    .testTag("algorithm_card_${item.id}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoAccent
                        )
                        Text(
                            text = item.notation,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanReticle,
                            modifier = Modifier
                                .background(CarbonElevated, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonSurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Center Color Box Trigger",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanReticle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.centerBoxRule,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = { onTryAlgorithm(item) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CyanReticle,
                                contentColor = ObsidianBg
                            ),
                            modifier = Modifier.testTag("try_algo_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Try on 3D Cube",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Load on 3D Cube",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
