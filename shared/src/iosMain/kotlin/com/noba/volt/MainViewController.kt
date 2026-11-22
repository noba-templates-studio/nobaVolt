package com.noba.volt

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Pantalla principal para iOS. La crea iosApp (ContentView.swift) al abrir la app. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    NobaVoltApp(PlataformaIos, conIntro = true)
}
