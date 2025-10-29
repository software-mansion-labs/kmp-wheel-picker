package com.swmansion.kmpwheelpicker

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import kotlinx.coroutines.launch

/**
 * Displays a wheel picker.
 *
 * @param state manages the wheel picker’s state.
 * @param modifier the modifiers to apply.
 * @param bufferSize the number of items shown above and below the window.
 * @param animationSpec the animation specification for animated scroll and snapping.
 * @param friction the scroll friction, used for snapping.
 * @param window draws the window, which highlights the selected item.
 * @param item draws the item at the given index. To style the item based on its position, compare
 *   the passed-in index to [WheelPickerState.value].
 */
@Composable
public fun WheelPicker(
    state: WheelPickerState,
    modifier: Modifier = Modifier,
    bufferSize: Int = 3,
    animationSpec: AnimationSpec<Float> =
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
    friction: Float = 8f,
    window: @Composable () -> Unit = {},
    item: @Composable (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    state.animationSpec = animationSpec
    state.friction = friction

    Layout(
        contents = listOf(window) + { repeat(state.itemCount) { item(it) } },
        measurePolicy = rememberWheelPickerMeasurePolicy(state, bufferSize),
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
                        val topValue = state.value - (size.height / state.maxItemHeight) / 2
                        val offsetDelta = offset.y / state.maxItemHeight
                        coroutineScope.launch {
                            state.animateScrollTo(
                                (topValue + offsetDelta).toInt(),
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
