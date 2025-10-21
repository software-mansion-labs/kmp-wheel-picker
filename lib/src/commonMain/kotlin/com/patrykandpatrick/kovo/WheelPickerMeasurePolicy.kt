package com.patrykandpatrick.kovo

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.max

internal class WheelPickerMeasurePolicy(
    private val state: WheelPickerState,
    private val bufferSize: Int,
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val itemPlaceables = measurables[1].map { it.measure(constraints) }
        var width = itemPlaceables.maxOf { it.width }
        var maxItemHeight = itemPlaceables.maxOf { it.height }
        var visibleItemCount = (2 * bufferSize + 1).coerceAtMost(itemPlaceables.size)
        if (visibleItemCount % 2 == 0) visibleItemCount++
        val windowConstraints = constraints.copy(minWidth = width, minHeight = maxItemHeight)
        val windowPlaceables = measurables[0].map { it.measure(windowConstraints) }
        width = max(width, windowPlaceables.maxOfOrNull { it.width } ?: 0)
        maxItemHeight = max(maxItemHeight, windowPlaceables.maxOfOrNull { it.height } ?: 0)
        val height = visibleItemCount * maxItemHeight
        val centerLine = height / 2
        state.maxItemHeight = maxItemHeight

        return layout(width, height) {
            windowPlaceables.forEach { it.place(0, (height - it.height) / 2) }
            val firstItemIndex = (state.value - bufferSize).toInt().coerceAtLeast(0)
            val lastItemIndex =
                ceil(state.value + bufferSize).toInt().coerceAtMost(state.itemCount - 1)
            for (index in firstItemIndex..lastItemIndex) {
                val placeable = itemPlaceables[index]
                val itemY = centerLine + (index - state.value) * maxItemHeight
                val placementY = itemY - placeable.height / 2
                placeable.place((width - placeable.width) / 2, placementY.toInt())
            }
        }
    }
}
