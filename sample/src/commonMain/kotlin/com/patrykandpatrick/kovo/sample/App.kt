package com.patrykandpatrick.kovo.sample

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.kovo.WheelPicker
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme(if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold { padding ->
            Box(Modifier.fillMaxWidth().padding(padding), Alignment.TopCenter) {
                WheelPicker(
                    highlight = { Box(Modifier.border(2.dp, Color.Gray, RoundedCornerShape(8.dp))) }
                ) {
                    repeat(10) { index -> WheelPickerItem("Item $index") }
                }
            }
        }
    }
}
