package com.swmansion.kmpwheelpicker.sample

import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FireTruck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Scanner
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.WheelPicker
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import kotlin.math.abs

private const val BUFFER_SIZE = 3

private val icons =
    Icons.Rounded.run {
        listOf(Cloud, CameraAlt, Radio, Wifi, Home, MusicNote, Bed, Scanner, FireTruck)
    }

@Composable
fun WheelPicker2() {
    val state = rememberWheelPickerState(itemCount = icons.size, initialIndex = icons.size / 2)
    WheelPicker(
        state = state,
        bufferSize = BUFFER_SIZE,
        animationSpec = spring(),
        window = {
            Box(
                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            )
        },
    ) { index ->
        Icon(
            imageVector = icons[index],
            contentDescription = null,
            modifier =
                Modifier.padding(12.dp).size(24.dp).graphicsLayer {
                    alpha = (BUFFER_SIZE - abs(state.value - index)).coerceIn(0f, 1f)
                },
            tint =
                lerp(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    abs(state.value - index).coerceIn(0f, 1f),
                ),
        )
    }
}
