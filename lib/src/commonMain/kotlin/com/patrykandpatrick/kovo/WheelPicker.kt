package com.patrykandpatrick.kovo

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import kotlinx.coroutines.launch

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
