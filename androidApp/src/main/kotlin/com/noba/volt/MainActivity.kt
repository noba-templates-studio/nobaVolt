package com.noba.volt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // La ventana de arranque es negra (Theme.NobaVolt.Arranque) para enlazar con la intro;
        // a partir de aquí se usa el tema normal de la app.
        setTheme(R.style.Theme_NobaVolt)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val plataforma = PlataformaAndroid(applicationContext)
        // La intro solo al abrir la app en frío: no al girar la pantalla ni al volver de segundo plano
        val conIntro = savedInstanceState == null
        setContent {
            NobaVoltApp(plataforma, conIntro)
        }
    }
}
