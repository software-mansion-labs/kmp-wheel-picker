package com.patrykandpatrick.kovo

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    state: WheelPickerState = rememberWheelPickerState(),
    itemExtent: Int = 3,
    onItemSelected: (Int) -> Unit = {},
    scrollAnimationSpec: AnimationSpec<Float> =
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
    highlight: @Composable () -> Unit = {},
    items: @Composable WheelPickerScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scope = remember { WheelPickerScopeImpl() }
    val onItemSelectedState = rememberUpdatedState(onItemSelected)
    state.scrollAnimationSpec = scrollAnimationSpec

    Layout(
        contents = listOf(highlight) + { items(scope) },
        measurePolicy =
            remember(state, itemExtent) {
                WheelPickerMeasurePolicy(
                    state = state,
                    itemExtent = itemExtent,
                    onItemSelected = onItemSelectedState,
                )
            },
        modifier =
            modifier
                .clip(RectangleShape)
                .draggable(
                    state = state.draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> state.snap(velocity) },
                    interactionSource = state.internalInteractionSource,
                )
                .pointerInput(state) {
                    detectTapGestures { offset ->
                        val firstVisibleIndex =
                            state.currentItem - (size.height / state.maxItemHeight) / 2
                        val clickedItemIndex = offset.y.toInt() / state.maxItemHeight
                        coroutineScope.launch {
                            state.animateScrollTo(
                                firstVisibleIndex + clickedItemIndex,
                                MutatePriority.UserInput,
                            )
                        }
                    }
                }
                .pointerInput(state) {
                    awaitEachGesture {
                        var pressInteraction: PressInteraction.Press? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press ->
                                    PressInteraction.Press(event.changes.first().position).also {
                                        pressInteraction = it
                                    }

                                PointerEventType.Release ->
                                    pressInteraction?.let(PressInteraction::Release)

                                else -> null
                            }?.also { interaction ->
                                coroutineScope.launch {
                                    state.internalInteractionSource.emit(interaction)
                                }
                            }
                            state.isDragInProgress = event.type == PointerEventType.Move
                        }
                    }
                },
    )
}

private class WheelPickerMeasurePolicy(
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

@Preview
@Composable
private fun WheelPickerPreview() {
    rememberScrollState()
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp),
    ) {
        WheelPicker(
            items = {
                val deselectedColor = Color.Gray
                val selectedColor = Color.Red

                List(10) { it.toString() }
                    .forEach {
                        val positionOffset = remember { mutableFloatStateOf(1f) }
                        val textColor = remember { mutableStateOf(deselectedColor) }
                        BasicText(
                            text = it,
                            style = TextStyle(fontSize = 24.sp),
                            color = { textColor.value },
                            modifier =
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    .onPositionChange { offset, viewPortOffset ->
                                        positionOffset.floatValue = abs(viewPortOffset)
                                        textColor.value =
                                            lerp(selectedColor, deselectedColor, abs(offset))
                                    }
                                    .graphicsLayer {
                                        alpha = 0f + (1 - positionOffset.floatValue) // * .75f
                                        scaleY = .5f + (1 - positionOffset.floatValue) * .5f
                                    },
                        )
                    }
            },
            highlight = {
                Box(modifier = Modifier.border(2.dp, Color.DarkGray, RoundedCornerShape(8.dp)))
            },
        )
    }
}
