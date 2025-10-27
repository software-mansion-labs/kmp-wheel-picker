package com.swmansion.kmpwheelpicker.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.WheelPicker
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import kotlin.math.abs

private const val BUFFER_SIZE = 3

@Composable
fun WheelPicker1() {
    val state = rememberWheelPickerState(itemCount = 15, initialIndex = 7)
    WheelPicker(
        state = state,
        bufferSize = BUFFER_SIZE,
        window = {
            Box(
                Modifier.background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    MaterialTheme.shapes.small,
                )
            )
        },
    ) { index ->
        Text(
            text = "Item ${index + 1}",
            modifier =
                Modifier.padding(16.dp, 8.dp).graphicsLayer {
                    alpha = (BUFFER_SIZE - abs(state.value - index)).coerceIn(0f, 1f)
                },
            color =
                lerp(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    abs(state.value - index).coerceIn(0f, 1f),
                ),
        )
    }
}
