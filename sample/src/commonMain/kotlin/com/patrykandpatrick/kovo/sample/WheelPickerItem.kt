package com.patrykandpatrick.kovo.sample

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.kovo.WheelPickerScope
import kotlin.math.abs

@Composable
fun WheelPickerScope.WheelPickerItem(value: String, modifier: Modifier = Modifier) {
    val deselectedColor = Color.Gray
    val selectedColor = Color.Red

    val positionOffset = remember { mutableFloatStateOf(1f) }

    val textColor = remember { mutableStateOf(deselectedColor) }
    BasicText(
        text = value,
        style = TextStyle(fontSize = 16.sp),
        color = { textColor.value },
        modifier =
            modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .onPositionChange { offset, viewPortOffset ->
                    positionOffset.floatValue = abs(viewPortOffset)
                    textColor.value = lerp(selectedColor, deselectedColor, abs(offset))
                }
                .graphicsLayer {
                    alpha = 0f + (1 - positionOffset.floatValue)
                    scaleY = .5f + (1 - positionOffset.floatValue) * .5f
                },
    )
}
