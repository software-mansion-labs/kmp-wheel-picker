package com.swmansion.kmpwheelpicker

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.interaction.Interaction
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
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Manages a wheel picker’s state. */
public class WheelPickerState internal constructor(initialIndex: Int = 0) {
    /** The index of the currently selected item. */
    public var index: Int by mutableIntStateOf(initialIndex)
        private set

    /** The scroll value, based on the range of item indexes. */
    public var value: Float by mutableFloatStateOf(initialIndex.toFloat())
        private set

    private var scrollJob: Job? = null

    internal var currentScrollPriority: MutatePriority? = null

    internal var isDragInProgress = false

    internal lateinit var animationSpec: AnimationSpec<Float>

    internal var friction by Delegates.notNull<Float>()

    internal var itemCount by mutableIntStateOf(0)

    /**
     * The slot height: the height of the tallest item or that of the window, whichever is greater.
     */
    public var slotHeight: Int by mutableIntStateOf(0)
        internal set

    internal val draggableState = DraggableState(::onScrollDelta)

    internal val internalInteractionSource = MutableInteractionSource()

    /** Represents the [Interaction] stream. */
    public val interactionSource: InteractionSource
        get() = internalInteractionSource

    private var widestItemIndex = -1

    private var maxItemWidth = 0

    private var tallestItemIndex = -1

    private var maxItemHeight = 0

    private fun onScrollDelta(delta: Float) {
        currentScrollPriority = null
        scrollJob?.cancel()
        scrollJob = null
        value -= delta / slotHeight
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
                    .getTargetValue(value, -velocity / slotHeight)
                    .coercedInValueRange
            )
        index = targetValue.toInt()
        animate(
            initialValue = value,
            targetValue = targetValue,
            initialVelocity = -velocity / slotHeight,
            animationSpec = animationSpec,
        ) { value, _ ->
            this.value = value
        }
    }

    public suspend fun scrollTo(
        value: Float,
        scrollPriority: MutatePriority = MutatePriority.Default,
    ) {
        startScroll(scrollPriority) { performScrollTo(value.coercedInValueRange) }
    }

    public suspend fun scrollTo(
        index: Int,
        scrollPriority: MutatePriority = MutatePriority.Default,
    ) {
        scrollTo(index.toFloat(), scrollPriority)
    }

    private fun performScrollTo(value: Float) {
        this.index = value.roundToInt()
        this.value = value
    }

    public suspend fun animateScrollTo(
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

    private fun updateMaxItemDimension(
        dimensions: List<Pair<Int, Int>>,
        currentLargestIndex: Int,
        currentMaxDimension: Int,
    ): Pair<Int, Int> {
        var largestIndex = currentLargestIndex.takeIf { it in 0..<itemCount }
        var invalidate = largestIndex == null
        var maxDimension = if (invalidate) 0 else currentMaxDimension
        for ((index, dimension) in dimensions) {
            if (dimension > maxDimension) {
                largestIndex = index
                maxDimension = dimension
                continue
            }
            if (index == largestIndex && dimension < maxDimension) {
                invalidate = true
            }
        }
        if (invalidate) {
            val (index, dimension) = dimensions.maxBy(Pair<Int, Int>::second)
            largestIndex = index
            maxDimension = dimension
        }
        return (largestIndex ?: -1) to maxDimension
    }

    internal fun updateMaxItemWidth(widths: List<Pair<Int, Int>>): Int {
        val (widestIndex, maxWidth) = updateMaxItemDimension(widths, widestItemIndex, maxItemWidth)
        widestItemIndex = widestIndex
        maxItemWidth = maxWidth
        return maxWidth
    }

    internal fun updateMaxItemHeight(heights: List<Pair<Int, Int>>): Int {
        val (tallestIndex, maxHeight) =
            updateMaxItemDimension(heights, tallestItemIndex, maxItemHeight)
        tallestItemIndex = tallestIndex
        maxItemHeight = maxHeight
        return maxHeight
    }
}

private val WheelPickerStateSaver = Saver({ it.index }, ::WheelPickerState)

/**
 * Creates and remembers a [WheelPickerState] instance.
 *
 * @param itemCount the item count.
 * @param initialIndex the index of the initially selected item.
 */
@Composable
public fun rememberWheelPickerState(itemCount: Int, initialIndex: Int = 0): WheelPickerState =
    rememberSaveable(saver = WheelPickerStateSaver) { WheelPickerState(initialIndex) }
        .also { it.itemCount = itemCount }
