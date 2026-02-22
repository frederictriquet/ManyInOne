package fr.triquet.manyinone.loyalty

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.triquet.manyinone.data.local.LoyaltyCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: LoyaltyCardsViewModel = viewModel(),
) {
    val context = LocalContext.current
    var card by remember { mutableStateOf<LoyaltyCard?>(null) }

    LaunchedEffect(cardId) {
        card = viewModel.getById(cardId)
    }

    // Set brightness to max on enter, restore on exit
    val activity = context as? Activity
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        window?.let {
            val params = it.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            it.attributes = params
        }
        onDispose {
            window?.let {
                val params = it.attributes
                params.screenBrightness = originalBrightness
                it.attributes = params
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.name ?: "Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    card?.let { c ->
                        IconButton(onClick = { onEdit(c.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.deleteCard(c)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        val currentCard = card
        if (currentCard == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val is2D = BarcodeFormatMapper.is2D(currentCard.barcodeFormat)
                BarcodeImage(
                    value = currentCard.barcodeValue,
                    format = currentCard.barcodeFormat,
                    width = 800,
                    height = if (is2D) 800 else 400,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = currentCard.barcodeValue,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentCard.barcodeFormat,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
