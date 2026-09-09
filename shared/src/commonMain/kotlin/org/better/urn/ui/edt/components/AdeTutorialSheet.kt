package org.better.urn.ui.edt.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import betterurn.shared.generated.resources.Res
import betterurn.shared.generated.resources.ade_step1_dark
import betterurn.shared.generated.resources.ade_step1_light
import betterurn.shared.generated.resources.ade_step2_dark
import betterurn.shared.generated.resources.ade_step2_light
import betterurn.shared.generated.resources.ade_step3_dark
import betterurn.shared.generated.resources.ade_step3_light
import betterurn.shared.generated.resources.ade_step4_dark
import betterurn.shared.generated.resources.ade_step4_light
import org.jetbrains.compose.resources.painterResource

// Data structure representing each step of the ADE export tutorial.
private data class AdeTutorialStep(
    val stepNumber: Int,
    val title: String,
    val description: String
)

private val TUTORIAL_STEPS = listOf(
    AdeTutorialStep(
        stepNumber = 1,
        title = "Sélectionner votre groupe",
        description = "Dans la liste des ressources de votre établissement sur la gauche, développez l'arborescence jusqu'à trouver votre groupe, puis cliquez dessus pour l'afficher sur le calendrier."
    ),
    AdeTutorialStep(
        stepNumber = 2,
        title = "Ouvrir les options d'export",
        description = "En bas à gauche de votre écran, dans le petit panneau « OPTIONS », cliquez sur l'icône d'export (qui ressemble à un calendrier avec une flèche sortante)."
    ),
    AdeTutorialStep(
        stepNumber = 3,
        title = "Générer l'URL ICalendar",
        description = "Une fenêtre s'ouvre. Assurez-vous que le format « ICalendar » est bien coché. Vous n'avez pas besoin de modifier les dates de début ou de fin. Cliquez simplement sur le bouton « Générer URL » en bas au centre."
    ),
    AdeTutorialStep(
        stepNumber = 4,
        title = "Copier l'URL ou flasher le QR code",
        description = "Une nouvelle fenêtre affiche un code QR et un lien web. Copiez entièrement l'URL longue indiquée ou flashez le QR code : c'est ce lien/QR code qu'il faudra coller dans l'application pour importer et synchroniser votre emploi du temps !"
    )
)

/**
 * Bottom sheet component displaying a step-by-step visual tutorial for retrieving an ADE schedule export.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdeTutorialSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(initialPage = 0) { TUTORIAL_STEPS.size }
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header bar with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tutoriel ADE",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Étape ${pagerState.currentPage + 1} sur ${TUTORIAL_STEPS.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fermer"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step progress bar
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1) / TUTORIAL_STEPS.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pager containing tutorial step cards
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val step = TUTORIAL_STEPS[pageIndex]
                val stepPainter = when (step.stepNumber) {
                    1 -> if (isDarkTheme) painterResource(Res.drawable.ade_step1_dark) else painterResource(Res.drawable.ade_step1_light)
                    2 -> if (isDarkTheme) painterResource(Res.drawable.ade_step2_dark) else painterResource(Res.drawable.ade_step2_light)
                    3 -> if (isDarkTheme) painterResource(Res.drawable.ade_step3_dark) else painterResource(Res.drawable.ade_step3_light)
                    else -> if (isDarkTheme) painterResource(Res.drawable.ade_step4_dark) else painterResource(Res.drawable.ade_step4_light)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Illustration image for the current step
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            painter = stepPainter,
                            contentDescription = "Illustration étape ${step.stepNumber}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step header and narrative text
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "Étape ${step.stepNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Précédent")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (pagerState.currentPage < TUTORIAL_STEPS.size - 1) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("Suivant")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Button(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("J'ai compris")
                    }
                }
            }
        }
    }
}
