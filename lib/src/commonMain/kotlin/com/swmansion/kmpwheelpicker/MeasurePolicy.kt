package com.swmansion.kmpwheelpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import kotlin.math.ceil
import kotlin.math.max

@Composable
internal fun rememberWheelPickerMeasurePolicy(state: WheelPickerState, bufferSize: Int) =
    remember(state, bufferSize) {
        MultiContentMeasurePolicy { measurables, constraints ->
            val lastIndex = state.itemCount - 1
            val coercedValue = state.value.coerceIn(0f, lastIndex.toFloat())
            val firstVisibleIndex = (coercedValue - bufferSize).toInt().coerceAtLeast(0)
            val lastVisibleIndex = ceil(coercedValue + bufferSize).toInt().coerceAtMost(lastIndex)
            val itemMeasurables = measurables[1]
            val visiblePlaceables =
                (firstVisibleIndex..lastVisibleIndex).map { index ->
                    index to itemMeasurables[index].measure(constraints)
                }
            val measuredPlaceables = buildList {
                addAll(visiblePlaceables)
                if (firstVisibleIndex > 0) add(0 to itemMeasurables[0].measure(constraints))
                if (lastVisibleIndex < lastIndex) {
                    add(lastIndex to itemMeasurables[lastIndex].measure(constraints))
                }
            }
            val itemWidths =
                measuredPlaceables.map { (index, placeable) -> index to placeable.width }
            val maxItemWidth = state.updateMaxItemWidth(itemWidths)
            val itemHeights =
                measuredPlaceables.map { (index, placeable) -> index to placeable.height }
            val maxItemHeight = state.updateMaxItemHeight(itemHeights)
            val windowConstraints =
                constraints.copy(minWidth = maxItemWidth, minHeight = maxItemHeight)
            val windowPlaceables =
                measurables[0].map { measurable -> measurable.measure(windowConstraints) }
            var visibleItemCount = (2 * bufferSize + 1).coerceAtMost(state.itemCount)
            if (visibleItemCount % 2 == 0) visibleItemCount++
            state.slotHeight =
                max(maxItemHeight, windowPlaceables.maxOfOrNull(Placeable::height) ?: 0)
            val width = max(maxItemWidth, windowPlaceables.maxOfOrNull(Placeable::width) ?: 0)
            val height = visibleItemCount * state.slotHeight
            val centerY = height / 2
            layout(width, height) {
                windowPlaceables.forEach { placeable ->
                    placeable.place(0, (height - placeable.height) / 2)
                }
                visiblePlaceables.forEach { (index, placeable) ->
                    val itemCenterY = centerY + (index - state.value) * state.slotHeight
                    placeable.place(
                        (width - placeable.width) / 2,
                        (itemCenterY - placeable.height / 2).toInt(),
                    )
                }
            }
        }
    }
