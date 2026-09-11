package org.better.urn.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.better.urn.data.QrScanResult
import org.better.urn.getPlatform

/**
 * Material 3 Modal Bottom Sheet for scanning QR codes and confirming calendar import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDesktop = remember { getPlatform().name.contains("Java") || getPlatform().name.contains("JVM") }

    var scannedResult by remember { mutableStateOf<QrScanResult.Success?>(null) }
    var calendarName by remember { mutableStateOf("") }
    var isTorchEnabled by remember { mutableStateOf(value = false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (scannedResult == null) {
                                if (isDesktop) "Importer un QR Code" else "Scanner un QR Code"
                            } else "Calendrier détecté",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (scannedResult == null) {
                                if (isDesktop) "Sélectionnez un fichier image ou collez son chemin" else "Flashez le code QR généré sur ADE"
                            } else "Vérifiez et validez les informations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    if ((scannedResult == null) && !isDesktop) {
                        IconButton(
                            onClick = { isTorchEnabled = !isTorchEnabled },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isTorchEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = "Activer le flash",
                                tint = if (isTorchEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentResult = scannedResult
            if (currentResult == null) {
                if (isDesktop) {
                    // Desktop Mode: File Picker View without camera reticle laser overlay
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QrScannerView(
                            onQrCodeScanned = { result ->
                                if (result is QrScanResult.Success) {
                                    scannedResult = result
                                    calendarName = result.suggestedName
                                }
                            },
                            isTorchEnabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Mobile Camera Mode: Camera View with Frame Overlay & Animated Laser
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        QrScannerView(
                            onQrCodeScanned = { result ->
                                if (result is QrScanResult.Success) {
                                    scannedResult = result
                                    calendarName = result.suggestedName
                                }
                            },
                            isTorchEnabled = isTorchEnabled,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlay with framing reticle & scanning laser
                        QrScanningReticleOverlay()
                    }
                }
            } else {
                // Confirmation State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Code QR scanné avec succès",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = calendarName,
                            onValueChange = { calendarName = it },
                            label = { Text("Nom du calendrier") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = currentResult.url,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("URL d'importation (ICS)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scannedResult = null
                                }
                            ) {
                                Text("Resscanner")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    if (calendarName.isNotBlank()) {
                                        onConfirm(calendarName.trim(), currentResult.url)
                                    }
                                },
                                enabled = calendarName.isNotBlank()
                            ) {
                                Text("Ajouter")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Animated Material 3 viewfinder overlay with corner indicators and a scanning laser.
 */
@Composable
private fun QrScanningReticleOverlay() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition()

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val boxSize = minOf(width, height) * 0.68f
        val left = (width - boxSize) / 2f
        val top = (height - boxSize) / 2f
        val cornerLength = 32.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        // Draw dark semi-transparent backdrop around central cutout
        drawRect(
            color = Color.Black.copy(alpha = 0.45f)
        )

        // Draw corner brackets
        // Top-Left
        drawLine(
            color = primaryColor,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor,
            start = Offset(left, top),
            end = Offset(left, top + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top-Right
        drawLine(
            color = primaryColor,
            start = Offset(left + boxSize, top),
            end = Offset(left + boxSize - cornerLength, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor,
            start = Offset(left + boxSize, top),
            end = Offset(left + boxSize, top + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Left
        drawLine(
            color = primaryColor,
            start = Offset(left, top + boxSize),
            end = Offset(left + cornerLength, top + boxSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor,
            start = Offset(left, top + boxSize),
            end = Offset(left, top + boxSize - cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Right
        drawLine(
            color = primaryColor,
            start = Offset(left + boxSize, top + boxSize),
            end = Offset(left + boxSize - cornerLength, top + boxSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor,
            start = Offset(left + boxSize, top + boxSize),
            end = Offset(left + boxSize, top + boxSize - cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw animated laser scanning line
        val lineY = top + boxSize * scanLineProgress
        drawLine(
            color = primaryColor.copy(alpha = 0.85f),
            start = Offset(left + 12.dp.toPx(), lineY),
            end = Offset(left + boxSize - 12.dp.toPx(), lineY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
