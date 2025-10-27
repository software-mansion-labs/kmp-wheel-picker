package com.swmansion.kmpwheelpicker.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun SampleApp() {
    val layoutDirection = LocalLayoutDirection.current
    MaterialTheme(if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold { padding ->
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                        top = padding.calculateTopPadding() + 16.dp,
                        end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { WheelPickerCard { WheelPicker1() } }
                item { WheelPickerCard { WheelPicker2() } }
            }
        }
    }
}
