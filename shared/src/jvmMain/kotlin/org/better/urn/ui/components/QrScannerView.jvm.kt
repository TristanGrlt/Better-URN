package org.better.urn.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.better.urn.data.QrCodeDecoder
import org.better.urn.data.QrScanResult
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun QrScannerView(
    onQrCodeScanned: (QrScanResult) -> Unit,
    isTorchEnabled: Boolean,
    modifier: Modifier,
) {
    var imagePathInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var loadedImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    fun processImageFile(file: File) {
        try {
            selectedFileName = file.name
            imagePathInput = file.absolutePath
            errorMessage = null

            val image: BufferedImage? = ImageIO.read(file)
            if (image == null) {
                errorMessage = "Impossible de lire le fichier image sélectionné."
                loadedImageBitmap = null
                return
            }

            loadedImageBitmap = image.toComposeImageBitmap()

            val width = image.width
            val height = image.height
            val pixels = IntArray(width * height)
            image.getRGB(0, 0, width, height, pixels, 0, width)

            val result = QrCodeDecoder.decodeRgbPixels(pixels, width, height)
            if (result is QrScanResult.Success) {
                onQrCodeScanned(result)
            } else {
                errorMessage = "Aucun code QR valide d'emploi du temps n'a été trouvé dans cette image."
            }
        } catch (e: Exception) {
            errorMessage = "Erreur lors de la lecture de l'image : ${e.localizedMessage ?: "Fichier invalide"}"
            loadedImageBitmap = null
        }
    }

    fun openImagePickerSafely() {
        SwingUtilities.invokeLater {
            try {
                // Force Java CrossPlatform LookAndFeel to avoid GIO/GTK native crashes on Linux/NixOS
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
                val chooser = JFileChooser().apply {
                    dialogTitle = "Sélectionner une image de QR Code"
                    fileFilter = FileNameExtensionFilter(
                        "Images QR Code (*.png, *.jpg, *.jpeg, *.bmp, *.webp)",
                        "png", "jpg", "jpeg", "bmp", "webp"
                    )
                    isAcceptAllFileFilterUsed = true
                }

                val returnVal = chooser.showOpenDialog(null)
                if ((returnVal == JFileChooser.APPROVE_OPTION) && (chooser.selectedFile != null)) {
                    val file = chooser.selectedFile
                    processImageFile(file)
                }
            } catch (e: Exception) {
                errorMessage = "Erreur d'ouverture du sélecteur de fichier : ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Importer un QR Code depuis un fichier",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sélectionnez une image (PNG, JPG) ou collez le chemin vers l'image du QR Code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Browse button card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { openImagePickerSafely() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedFileName ?: "Parcourir vos fichiers image...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Formats acceptés : PNG, JPG, JPEG, WEBP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { openImagePickerSafely() }
                    ) {
                        Text("Parcourir")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Path Input
            OutlinedTextField(
                value = imagePathInput,
                onValueChange = { input ->
                    imagePathInput = input
                    val file = File(input.trim())
                    if (file.exists() && file.isFile) {
                        processImageFile(file)
                    }
                },
                label = { Text("Chemin du fichier image") },
                placeholder = { Text("/chemin/vers/image_qr.png") },
                trailingIcon = {
                    IconButton(onClick = { openImagePickerSafely() }) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Ouvrir dossier"
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Loaded Image Preview
            loadedImageBitmap?.let { bitmap ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Aperçu de l'image scannée",
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
