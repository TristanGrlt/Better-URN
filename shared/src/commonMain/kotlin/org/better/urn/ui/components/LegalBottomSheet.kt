package org.better.urn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Material 3 Modal Bottom Sheet displaying the legal notices and privacy policy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Policy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Mentions légales & Confidentialité",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider()

            LegalSection(
                title = "1. Éditeur de l'application",
                content = "L'application Better URN est développée et maintenue de manière indépendante. Elle agit comme un client logiciel tiers et n'est ni affiliée, ni sponsorisée, ni officiellement validée par les établissements d'enseignement dont elle permet d'interroger les plateformes, ni par le projet Moodle."
            )

            LegalSection(
                title = "2. Données collectées et Stockage Local",
                content = "Better URN est conçue selon le principe du respect de la vie privée dès la conception (Privacy by Design). L'application opère comme une passerelle de communication directe entre votre appareil et le serveur de votre établissement.",
                bulletPoints = listOf(
                    "Absence de serveurs tiers" to "Vos identifiants, résultats, emplois du temps et fichiers de cours sont stockés exclusivement sur la mémoire locale de votre appareil.",
                    "Calendriers & Emplois du temps" to "Les URL d'emplois du temps au format ICS (ex. Celcat, ADE), la liste des cours masqués et vos préférences d'affichage (onglet au démarrage, vue par défaut, jours affichés) sont enregistrés uniquement sur votre appareil. La synchronisation interroge directement les serveurs d'origine sans aucun intermédiaire.",
                    "Sécurité des accès" to "Le jeton de connexion (token) assurant l'authentification est chiffré et sauvegardé de manière sécurisée en s'appuyant sur les mécanismes de protection natifs de votre système d'exploitation.",
                    "Gestion du cache & préférences" to "L'application conserve temporairement certaines données (miniatures, arborescence des cours, documents) et vos choix de configuration pour garantir un fonctionnement hors ligne et accélérer la navigation. Ce cache local peut être purgé à tout moment depuis les paramètres de l'application."
                )
            )

            LegalSection(
                title = "3. Téléchargements et Fichiers",
                content = "Les documents téléchargés via l'application sont enregistrés dans le dossier de téléchargement standard de votre système. Cette approche vous permet de les ouvrir librement avec les applications de votre choix. L'application ne surveille ni ne trace l'utilisation de ces fichiers une fois qu'ils sont enregistrés sur votre appareil."
            )

            LegalSection(
                title = "4. Traitement des Données Personnelles (RGPD) et Suivi",
                content = "L'application n'intègre aucun outil de pistage publicitaire, de télémétrie distante ou d'analyse de comportement.\n\nConformément à la réglementation en vigueur sur la protection des données, vous disposez d'un contrôle absolu sur vos informations. L'utilisation de la fonction de déconnexion au sein de l'application détruit instantanément et de façon irréversible votre jeton d'accès local ainsi que les données de session associées."
            )

            LegalSection(
                title = "5. Limites de Responsabilité",
                content = "L'application est fournie \"en l'état\". L'éditeur décline toute responsabilité concernant les pannes éventuelles des serveurs distants, les inexactitudes d'affichage ou la perte éventuelle de données locales. La vérification finale des informations critiques, telles que les dates d'examen ou les échéances de rendu, doit systématiquement être effectuée sur la plateforme web officielle de votre établissement."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text("Fermer")
            }
        }
    }
}

@Composable
private fun LegalSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    bulletPoints: List<Pair<String, String>> = emptyList()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (bulletPoints.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bulletPoints.forEach { (label, detail) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                                    append("$label : ")
                                }
                                append(detail)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
