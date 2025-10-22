package com.swmansion.kmpwheelpicker.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KMP Wheel Picker") { SampleApp() }
}
