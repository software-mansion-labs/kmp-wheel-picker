package com.patrykandpatrick.kovo.sample

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.kovo.WheelPicker
import com.patrykandpatrick.kovo.WheelPickerItem
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            WheelPicker(
                highlight = {
                    Box(modifier = Modifier.border(2.dp, Color.DarkGray, RoundedCornerShape(8.dp)))
                }
            ) {
                repeat(10) { WheelPickerItem("Item $it") }
            }
        }
    }
}
