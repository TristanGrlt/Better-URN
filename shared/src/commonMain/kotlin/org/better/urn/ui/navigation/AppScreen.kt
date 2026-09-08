package org.better.urn.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen(val title: String, val icon: ImageVector) {
    UNIVERSITICE("Universitice", Icons.Rounded.Book),
    IZLY("Izly", Icons.Rounded.AccountBalanceWallet),
    EDT("EDT", Icons.Rounded.DateRange),
    AUTRE("Paramètres", Icons.Rounded.Settings)
}
