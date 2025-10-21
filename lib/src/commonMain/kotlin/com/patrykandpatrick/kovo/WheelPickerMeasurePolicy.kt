package com.patrykandpatrick.kovo

import androidx.compose.runtime.State
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

internal class WheelPickerMeasurePolicy(
    private val state: WheelPickerState,
    private val itemExtent: Int,
    private val onItemSelected: State<(Int) -> Unit>,
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val itemPlaceables = measurables[1].map { it.measure(constraints) }
        var width = itemPlaceables.maxOf { it.width }
        var maxItemHeight = itemPlaceables.maxOf { it.height }
        var visibleItemCount = (itemExtent * 2 + 1).coerceAtMost(itemPlaceables.size)
        if (visibleItemCount % 2 == 0) visibleItemCount++
        val highlightConstraints = constraints.copy(minWidth = width, minHeight = maxItemHeight)
        val highlightPlaceables = measurables[0].map { it.measure(highlightConstraints) }
        width = max(width, highlightPlaceables.maxOfOrNull { it.width } ?: 0)
        maxItemHeight = max(maxItemHeight, highlightPlaceables.maxOfOrNull { it.height } ?: 0)
        val height = visibleItemCount * maxItemHeight
        val centerLine = height / 2
        state.itemCount = itemPlaceables.size
        state.maxItemHeight = maxItemHeight

        if (!state.initialScrollCalculated) {
            state.setScroll(
                value = (height - maxItemHeight) / 2 - state.currentItem * maxItemHeight.toFloat(),
                minScroll = -itemPlaceables.size * maxItemHeight + (height + maxItemHeight) / 2f,
                maxScroll = (height - maxItemHeight) / 2f,
            )
        }

        return layout(width, height) {
            highlightPlaceables.forEach { placeable ->
                placeable.place(0, (height - placeable.height) / 2)
            }

            val firstItemIndex =
                (((state.maxScroll - state.scroll.floatValue) / maxItemHeight).toInt() -
                        visibleItemCount / 2)
                    .coerceAtLeast(0)
            val lastItemIndex =
                (firstItemIndex + visibleItemCount).coerceAtMost(itemPlaceables.lastIndex)
            for (index in firstItemIndex..lastItemIndex) {
                val placeable = itemPlaceables[index]
                val itemY = (state.scroll.floatValue + maxItemHeight * index)
                val placementY = itemY + (maxItemHeight - placeable.height) / 2
                placeable.place((width - placeable.width) / 2, placementY.toInt())
                val listPickerItemSpecNode = placeable.parentData as? ListPickerItemSpecNode
                if (
                    itemY < centerLine &&
                    itemY + maxItemHeight > centerLine &&
                    state.currentItem != index
                ) {
                    state.currentItem = index
                    onItemSelected.value(index)
                }

                if (state.currentItem == index) {
                    state.currentItemOffset =
                        ((centerLine - itemY - maxItemHeight / 2) / maxItemHeight).coerceIn(
                            -.5f,
                            .5f,
                        )
                }

                if (listPickerItemSpecNode != null) {
                    val selectedPositionOffset =
                        (itemY + maxItemHeight / 2 - centerLine) / maxItemHeight
                    val viewPortOffset = (itemY + maxItemHeight / 2 - centerLine) / (height / 2)
                    listPickerItemSpecNode.onPositionChange?.invoke(
                        selectedPositionOffset.coerceIn(-1f, 1f),
                        viewPortOffset.coerceIn(-1f, 1f),
                    )
                }
            }
        }
    }
}
