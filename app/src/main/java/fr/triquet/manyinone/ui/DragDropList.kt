package fr.triquet.manyinone.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

class DragDropListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
    private val draggingItemIndexState: MutableIntState,
    private val dragOffsetState: MutableFloatState,
) {
    var draggingItemIndex: Int by draggingItemIndexState
        private set
    var dragOffset: Float by dragOffsetState
        private set

    fun onDragStart(offset: Float) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.toInt() in it.offset..(it.offset + it.size) }
            ?.let {
                draggingItemIndex = it.index
                dragOffset = 0f
            }
    }

    fun onDrag(change: Float) {
        dragOffset += change
        val draggingItem = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex } ?: return
        val startOffset = draggingItem.offset + dragOffset
        val endOffset = startOffset + draggingItem.size

        val targetItem = lazyListState.layoutInfo.visibleItemsInfo
            .filterNot { it.index == draggingItemIndex }
            .firstOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                itemCenter in startOffset.toInt()..endOffset.toInt()
            }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            onMove(draggingItemIndex, targetIndex)
            dragOffset += (draggingItemIndex - targetIndex) * draggingItem.size
            draggingItemIndex = targetIndex
        }
    }

    fun onDragEnd() {
        draggingItemIndex = -1
        dragOffset = 0f
    }

    val isDragging: Boolean get() = draggingItemIndex >= 0
}

@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
): DragDropListState {
    val draggingItemIndex = remember { mutableIntStateOf(-1) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    return remember(lazyListState) {
        DragDropListState(lazyListState, onMove, draggingItemIndex, dragOffset)
    }
}

fun Modifier.dragHandle(
    dragDropState: DragDropListState,
    itemInfo: () -> LazyListItemInfo?,
    onDragStopped: (() -> Unit)? = null,
): Modifier = this.pointerInput(dragDropState) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            val info = itemInfo()
            if (info != null) {
                dragDropState.onDragStart((info.offset + offset.y).toFloat())
            }
        },
        onDrag = { change, dragAmount ->
            change.consume()
            dragDropState.onDrag(dragAmount.y)
        },
        onDragEnd = {
            dragDropState.onDragEnd()
            onDragStopped?.invoke()
        },
        onDragCancel = {
            dragDropState.onDragEnd()
            onDragStopped?.invoke()
        },
    )
}

fun Modifier.draggedItem(
    dragDropState: DragDropListState,
    index: Int,
): Modifier {
    val offset = if (index == dragDropState.draggingItemIndex) dragDropState.dragOffset else 0f
    return this
        .zIndex(if (index == dragDropState.draggingItemIndex) 1f else 0f)
        .graphicsLayer { translationY = offset }
}
