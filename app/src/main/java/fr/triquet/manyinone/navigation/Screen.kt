package fr.triquet.manyinone.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Scanner : Screen(
        route = "scanner",
        label = "Scanner",
        icon = Icons.Default.QrCodeScanner,
    )

    data object LoyaltyCards : Screen(
        route = "loyalty_cards",
        label = "Cards",
        icon = Icons.Default.CreditCard,
    )
}
