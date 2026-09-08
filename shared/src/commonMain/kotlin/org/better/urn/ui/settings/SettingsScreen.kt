package org.better.urn.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.better.urn.data.AppTheme
import org.better.urn.ui.components.BetterUrnTopBar

/**
 * Settings screen Composable complying strictly with Material 3 design guidelines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChanged: (AppTheme) -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onClearCacheClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onToggleLegalDialog: (Boolean?) -> Unit,
    modifier: Modifier = Modifier,
    onToggleServerDialog: ((Boolean?) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    var isLocalServerDialogOpen by remember { mutableStateOf(false) }
    var tempServerUrl by remember(state.moodleUrl) { mutableStateOf(state.moodleUrl) }

    val showServerDialog = state.isServerDialogOpen || isLocalServerDialogOpen
    val dismissServerDialog = {
        if (onToggleServerDialog != null) {
            onToggleServerDialog(false)
        }
        isLocalServerDialogOpen = false
    }

    Scaffold(
        topBar = {
            BetterUrnTopBar(
                title = "Paramètres",
                onBackClick = onBackClick
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Account section
            item {
                SettingsSectionHeader(title = "Compte")
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = state.currentUser?.fullname ?: "Non connecté",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            text = if (state.currentUser != null) "Compte Moodle actif" else "Connectez-vous via l'onglet Universitice",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            if (state.currentUser != null) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Se déconnecter",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable(onClick = onLogoutClicked)
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Server section
            item {
                SettingsSectionHeader(title = "Serveur")
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "URL du serveur Moodle",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = state.moodleUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        tempServerUrl = state.moodleUrl
                        if (onToggleServerDialog != null) {
                            onToggleServerDialog(true)
                        } else {
                            isLocalServerDialogOpen = true
                        }
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Appearance section
            item {
                SettingsSectionHeader(title = "Apparence")
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Thème de l'application",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            val themes = listOf(
                                AppTheme.SYSTEM to "Système",
                                AppTheme.LIGHT to "Clair",
                                AppTheme.DARK to "Sombre"
                            )
                            themes.forEachIndexed { index, (themeOption, label) ->
                                SegmentedButton(
                                    selected = state.theme == themeOption,
                                    onClick = { onThemeChanged(themeOption) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = themes.size
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Storage section
            item {
                SettingsSectionHeader(title = "Stockage")
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Vider le cache",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Supprimer les fichiers temporaires et données en cache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable(onClick = onClearCacheClicked)
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Legal section
            item {
                SettingsSectionHeader(title = "Légal")
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Mentions légales & Confidentialité",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Informations juridiques et politique de confidentialité",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Policy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { onToggleLegalDialog(true) }
                )
            }
        }

        // Server URL configuration modal dialog
        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = dismissServerDialog,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(text = "URL du serveur Moodle")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Saisissez l'adresse URL principale du serveur Moodle.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = tempServerUrl,
                            onValueChange = { tempServerUrl = it },
                            label = { Text("URL du serveur") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onServerUrlChanged(tempServerUrl)
                            dismissServerDialog()
                        }
                    ) {
                        Text("Enregistrer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = dismissServerDialog) {
                        Text("Annuler")
                    }
                }
            )
        }

        // Legal notice dialog
        if (state.isLegalDialogOpen) {
            AlertDialog(
                onDismissRequest = { onToggleLegalDialog(false) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(text = "Mentions légales & Confidentialité")
                },
                text = {
                    Text(
                        text = "Better URN est un client alternatif open-source développé pour faciliter l'accès aux cours et documents universitaires.\n\nCette application n'est pas affiliée officiellement aux services Moodle de l'Université de Rouen Normandie.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onToggleLegalDialog(false) }) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
