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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs

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
