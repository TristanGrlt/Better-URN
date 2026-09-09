package org.better.urn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.better.urn.data.QrScanResult

/**
 * Platform-specific QR code scanning preview component.
 * On Android, uses CameraX with real-time frame analysis and permission handling.
 * On Desktop (JVM), provides an image file selector and QR code decoder.
 */
@Composable
expect fun QrScannerView(
    onQrCodeScanned: (QrScanResult) -> Unit,
    isTorchEnabled: Boolean = false,
    modifier: Modifier = Modifier
)
