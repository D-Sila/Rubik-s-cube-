package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.cube.ColorClassifier
import com.example.cube.CubeColor
import com.example.cube.CubeState
import com.example.cube.Face
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CyanReticle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors

@Composable
fun CameraScannerView(
    cube: CubeState,
    scannedFaces: Set<Face>,
    onCaptureFace: (Face, List<CubeColor>) -> Unit,
    onSwitchToPainter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    var liveGrid by remember {
        mutableStateOf(List(9) { CubeColor.GREEN })
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Map detected center color box to its matching face on the cube
    val detectedCenterColor = liveGrid[4]
    val autoMatchedFace = remember(detectedCenterColor, cube) {
        Face.entries.firstOrNull { cube.centerColor(it) == detectedCenterColor } ?: Face.F
    }
    var overrideFace by remember { mutableStateOf<Face?>(null) }
    val targetFace = overrideFace ?: autoMatchedFace

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Camera Scanner",
                    tint = CyanReticle,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.camera_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.camera_permission_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("grant_camera_button")
                    ) {
                        Text(stringResource(R.string.camera_grant_button))
                    }
                    OutlinedButton(
                        onClick = onSwitchToPainter,
                        modifier = Modifier.testTag("fallback_net_painter_button")
                    ) {
                        Text(stringResource(R.string.use_net_painter))
                    }
                }
            }
            return@Column
        }

        // Camera Preview + 3x3 Center-Box Reticle HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ObsidianBg)
                .border(1.5.dp, SlateBorder, RoundedCornerShape(22.dp))
        ) {
            if (cameraError == null) {
                CameraPreviewSurface(
                    onGridDetected = { result -> liveGrid = result.stickers },
                    onError = { msg -> cameraError = msg },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera unavailable on this device ($cameraError). Use the 2D Net Painter or test capture below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3x3 Scanner Reticle Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(186.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0 until 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until 3) {
                            val idx = r * 3 + c
                            val isCenter = idx == 4
                            val color = liveGrid[idx]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.composeColor.copy(alpha = 0.35f))
                                    .border(
                                        width = if (isCenter) 3.dp else 1.2.dp,
                                        color = if (isCenter) CyanReticle else Color.White.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCenter) {
                                    Text(
                                        text = targetFace.symbol.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Center-Box Detection Banner
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(10.dp)
                    .background(CarbonSurface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .border(1.dp, CyanReticle.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CenterFocusStrong,
                    contentDescription = "Detected Center Box",
                    tint = CyanReticle,
                    modifier = Modifier.size(18.dp)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(detectedCenterColor.composeColor)
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                )
                Text(
                    text = "Center Box: ${detectedCenterColor.label} → ${targetFace.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
            }
        }

        // Face Progress Chips & Manual Face Override
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Face.entries.forEach { face ->
                val isScanned = face in scannedFaces
                val isTargeted = face == targetFace
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTargeted) CarbonElevated else Color.Transparent)
                        .border(
                            width = if (isTargeted) 1.5.dp else 0.dp,
                            color = if (isTargeted) CyanReticle else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { overrideFace = if (overrideFace == face) null else face }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = face.symbol.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isTargeted) CyanReticle else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (isScanned) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Scanned",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cube.centerColor(face).composeColor)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                onCaptureFace(targetFace, liveGrid)
                overrideFace = null
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanReticle,
                contentColor = ObsidianBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("capture_face_button")
        ) {
            Text(
                text = "Capture ${targetFace.displayName} (${scannedFaces.size}/6 Scanned)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CameraPreviewSurface(
    onGridDetected: (ColorClassifier.GridSampleResult) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                try {
                                    val result = ColorClassifier.analyzeImage(imageProxy)
                                    onGridDetected(result)
                                } finally {
                                    imageProxy.close()
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Camera init failed")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
