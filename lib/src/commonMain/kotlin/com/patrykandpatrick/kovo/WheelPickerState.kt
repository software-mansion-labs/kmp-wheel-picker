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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class WheelPickerState(initialSelectedIndex: Int = 0) {
    var currentItem by mutableIntStateOf(initialSelectedIndex)
        internal set

    var currentItemOffset by mutableFloatStateOf(0f)
        internal set

    private var scrollJob: Job? = null

    internal var currentScrollPriority: MutatePriority? = null

    internal var isDragInProgress = false

    internal var scrollAnimationSpec: AnimationSpec<Float> = androidx.compose.animation.core.snap()

    internal var itemCount: Int = 0

    var maxItemHeight: Int by mutableIntStateOf(0)
        internal set

    internal var minScroll = 0f

    internal var maxScroll = 0f

    internal val draggableState = DraggableState(::onScrollDelta)

    internal val internalInteractionSource = MutableInteractionSource()

    val interactionSource: InteractionSource
        get() = internalInteractionSource

    val scroll = mutableFloatStateOf(0f)

    var currentScroll: Float = 0f
        private set

    var initialScrollCalculated = false
        private set

    fun setScroll(value: Float, minScroll: Float, maxScroll: Float) {
        scrollJob?.cancel()
        scrollJob = null
        this.minScroll = minScroll
        this.maxScroll = maxScroll
        if (currentScroll == 0f) setScroll(value)
        initialScrollCalculated = true
    }

    private fun setScroll(value: Float): Float {
        val currentScroll = currentScroll
        val delta = value - currentScroll
        onScrollDeltaInternal(delta)
        return delta
    }

    fun onScrollDelta(delta: Float) {
        currentScrollPriority = null
        scrollJob?.cancel()
        scrollJob = null
        onScrollDeltaInternal(delta)
    }

    private fun onScrollDeltaInternal(delta: Float) {
        val newScroll = (currentScroll + delta)
        scroll.floatValue = newScroll
        currentScroll = newScroll
    }

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
            FloatExponentialDecaySpec()
                .getTargetValue(currentScroll, velocity)
                .coerceIn(minScroll, maxScroll)
        val remainder = targetValue % maxItemHeight
        val snapScrollValue =
            targetValue - remainder - if (abs(remainder) > maxItemHeight / 2) maxItemHeight else 0
        animate(
            initialValue = currentScroll,
            targetValue = snapScrollValue,
            initialVelocity = velocity,
            animationSpec = scrollAnimationSpec,
        ) { value, _ ->
            setScroll(value)
        }
    }

    private fun getTargetScrollValue(index: Int, positionOffset: Float): Float =
        maxScroll - maxItemHeight * (index + positionOffset).coerceIn(0f, itemCount - 1f)

    suspend fun scrollTo(
        index: Int,
        positionOffset: Float = 0f,
        scrollPriority: MutatePriority = MutatePriority.Default,
    ) {
        startScroll(scrollPriority) { performScrollTo(index, positionOffset) }
    }

    private fun performScrollTo(index: Int, positionOffset: Float) {
        setScroll(getTargetScrollValue(index, positionOffset))
    }

    suspend fun animateScrollTo(
        index: Int,
        scrollPriority: MutatePriority = MutatePriority.Default,
    ) {
        startScroll(scrollPriority) { performAnimateScrollTo(index) }
    }

    private suspend fun performAnimateScrollTo(index: Int) {
        animate(
            initialValue = currentScroll,
            targetValue = getTargetScrollValue(index, 0f),
            animationSpec = scrollAnimationSpec,
        ) { value, _ ->
            setScroll(value)
        }
    }
}

@Composable
fun rememberWheelPickerState(initialSelectedIndex: Int = 0): WheelPickerState =
    rememberSaveable(
        saver =
            Saver<WheelPickerState, Int>(
                save = { it.currentItem },
                restore = { WheelPickerState(it) },
            )
    ) {
        WheelPickerState(initialSelectedIndex)
    }
