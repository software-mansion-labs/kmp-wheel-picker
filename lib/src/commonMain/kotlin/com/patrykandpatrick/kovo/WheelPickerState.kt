package com.patrykandpatrick.kovo

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class WheelPickerState internal constructor(initialIndex: Int = 0) {
    var index: Int by mutableIntStateOf(initialIndex)
        private set

    var value: Float by mutableFloatStateOf(initialIndex.toFloat())
        private set

    private var scrollJob: Job? = null

    internal var currentScrollPriority: MutatePriority? = null

    internal var isDragInProgress = false

    internal lateinit var animationSpec: AnimationSpec<Float>

    internal var friction by Delegates.notNull<Float>()

    internal var itemCount by mutableIntStateOf(0)

    var maxItemHeight: Int by mutableIntStateOf(0)
        internal set

    internal val draggableState = DraggableState(::onScrollDelta)

    internal val internalInteractionSource = MutableInteractionSource()

    val interactionSource: InteractionSource
        get() = internalInteractionSource

    private fun onScrollDelta(delta: Float) {
        currentScrollPriority = null
        scrollJob?.cancel()
        scrollJob = null
        value -= delta / maxItemHeight
    }

    private val Int.coercedInIndexRange
        get() = coerceIn(0, itemCount - 1)

    private val Float.coercedInValueRange
        get() = coerceIn(0f, itemCount - 1f)

    private suspend fun stopScrollJob(scrollPriority: MutatePriority): Boolean {
        when {
            isDragInProgress -> return false
            (currentScrollPriority?.compareTo(scrollPriority) ?: 0) > 0 -> return false
        }
        scrollJob?.cancelAndJoin()
        scrollJob = null
        return true
    }

    private suspend fun startScroll(
        scrollPriority: MutatePriority,
        scroll: suspend CoroutineScope.() -> Unit,
    ) {
        if (stopScrollJob(scrollPriority)) {
            currentScrollPriority = scrollPriority
            scrollJob = coroutineScope {
                launch(context = Job()) {
                    scroll()
                    currentScrollPriority = null
                }
            }
        }
    }

    internal suspend fun snap(
        velocity: Float,
        scrollPriority: MutatePriority = MutatePriority.UserInput,
    ) {
        startScroll(scrollPriority) { performSnap(velocity) }
    }

    private suspend fun performSnap(velocity: Float) {
        val targetValue =
            round(
                FloatExponentialDecaySpec(friction)
                    .getTargetValue(value, -velocity / maxItemHeight)
                    .coercedInValueRange
            )
        index = targetValue.toInt()
        animate(
            initialValue = value,
            targetValue = targetValue,
            initialVelocity = -velocity / maxItemHeight,
            animationSpec = animationSpec,
        ) { value, _ ->
            this.value = value
        }
    }

    suspend fun scrollTo(value: Float, scrollPriority: MutatePriority = MutatePriority.Default) {
        startScroll(scrollPriority) { performScrollTo(value.coercedInValueRange) }
    }

    suspend fun scrollTo(index: Int, scrollPriority: MutatePriority = MutatePriority.Default) {
        scrollTo(index.toFloat(), scrollPriority)
    }

    private fun performScrollTo(value: Float) {
        this.index = value.roundToInt()
        this.value = value
    }

    suspend fun animateScrollTo(
        index: Int,
        scrollPriority: MutatePriority = MutatePriority.Default,
    ) {
        startScroll(scrollPriority) { performAnimateScrollTo(index.coercedInIndexRange) }
    }

    private suspend fun performAnimateScrollTo(index: Int) {
        this.index = index
        animate(
            initialValue = value,
            targetValue = index.toFloat(),
            animationSpec = animationSpec,
        ) { value, _ ->
            this.value = value
        }
    }
}

private val WheelPickerStateSaver = Saver({ it.index }, ::WheelPickerState)

@Composable
fun rememberWheelPickerState(itemCount: Int, initialIndex: Int = 0): WheelPickerState =
    rememberSaveable(saver = WheelPickerStateSaver) { WheelPickerState(initialIndex) }
        .also { it.itemCount = itemCount }
