package com.swmansion.kmpwheelpicker.sample

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.swmansion.kmpwheelpicker.WheelPicker
import com.swmansion.kmpwheelpicker.rememberWheelPickerState
import kotlin.math.abs
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val BUFFER_SIZE = 3

@Composable
@Preview
fun App() {
    val state = rememberWheelPickerState(itemCount = 10)
    MaterialTheme(if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                WheelPicker(
                    state = state,
                    bufferSize = BUFFER_SIZE,
                    window = {
                        Box(
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.small,
                            )
                        )
                    },
                ) { index ->
                    Text(
                        text = "Item $index",
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
        }
    }
}
