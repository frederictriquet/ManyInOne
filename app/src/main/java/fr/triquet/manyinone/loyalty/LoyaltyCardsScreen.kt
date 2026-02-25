package fr.triquet.manyinone.loyalty

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.triquet.manyinone.data.local.LoyaltyCard
import fr.triquet.manyinone.ui.dragHandle
import fr.triquet.manyinone.ui.draggedItem
import fr.triquet.manyinone.ui.rememberDragDropListState

@Composable
fun LoyaltyCardsScreen(
    onAddCard: () -> Unit,
    onCardClick: (Long) -> Unit,
    onCardLongPress: (Long) -> Unit = {},
    viewModel: LoyaltyCardsViewModel = viewModel(),
) {
    val cards by viewModel.cards.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropListState(listState) { from, to ->
        viewModel.moveCard(from, to)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            if (cards.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillParentMaxHeight(0.8f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.height(64.dp).width(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No loyalty cards yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    LoyaltyCardItem(
                        modifier = Modifier
                            .draggedItem(dragDropState, index)
                            .animateItem(),
                        card = card,
                        onClick = { onCardClick(card.id) },
                        onLongPress = { onCardLongPress(card.id) },
                        dragDropState = dragDropState,
                        itemProvider = { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } },
                        onDragStopped = { viewModel.commitCardOrder() },
                    )
                }
            }
            item(key = "add") {
                FilledTonalButton(
                    onClick = onAddCard,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.height(18.dp).width(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter une carte")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoyaltyCardItem(
    card: LoyaltyCard,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
    dragDropState: fr.triquet.manyinone.ui.DragDropListState? = null,
    itemProvider: (() -> androidx.compose.foundation.lazy.LazyListItemInfo?)? = null,
    onDragStopped: (() -> Unit)? = null,
) {
    val bgColor = card.backgroundColor
    val textColor = card.contrastTextColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Row(
            modifier = Modifier
                .background(bgColor)
                .padding(start = 4.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dragDropState != null && itemProvider != null) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Réordonner",
                    modifier = Modifier
                        .size(24.dp)
                        .dragHandle(dragDropState, itemProvider, onDragStopped),
                    tint = textColor.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            BarcodeImage(
                value = card.barcodeValue,
                format = card.barcodeFormat,
                width = if (BarcodeFormatMapper.is2D(card.barcodeFormat)) 60 else 100,
                height = 60,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
