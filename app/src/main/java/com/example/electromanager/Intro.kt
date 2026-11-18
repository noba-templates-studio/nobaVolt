package com.example.electromanager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Intro al abrir la app (≈1,5 s):
 *   0 ms     la S entra desde la izquierda y la O desde la derecha
 *   360 ms   el rayo cae en diagonal entre las dos letras
 *   500 ms   impacto: destello suave, ligero "pulso" y vibración mínima (coincide con el sonido)
 *   1200 ms  el monograma se desvanece y aparece la app, que ya está cargada debajo
 * Un toque en la pantalla la salta. No se muestra si el sistema tiene las animaciones desactivadas.
 */

/** Proporción del lienzo común de las tres capas (intro_s, intro_rayo, intro_o). */
private const val PROPORCION = 1003f / 648f
private const val IMPACTO_MS = 500L
private const val INICIO_RAYO_MS = 360L
/** Momento del golpe principal dentro del archivo de sonido (res/raw/intro_noba.ogg). */
private const val GOLPE_SONIDO_MS = 75L

private val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
/** Acelera hasta el final: da sensación de golpe. */
private val EaseInRayo = CubicBezierEasing(0.55f, 0f, 0.9f, 0.55f)

/** false si el usuario ha desactivado las animaciones en Opciones de accesibilidad/desarrollador. */
fun animacionesActivas(ctx: Context): Boolean =
    Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

@Composable
fun IntroNoba(sonido: Boolean, onFin: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val lados = remember { Animatable(0f) }    // 0 = fuera de pantalla, 1 = en su sitio
    val rayo = remember { Animatable(0f) }     // caída del rayo
    val destello = remember { Animatable(0f) } // 1 en el impacto y se apaga
    val salida = remember { Animatable(0f) }   // desvanecido final

    LaunchedEffect(Unit) {
        if (sonido) launch {
            // Se prepara ya y arranca con retraso para que su golpe coincida con la caída del rayo
            val reproductor = prepararSonidoIntro(ctx) ?: return@launch
            try {
                delay(IMPACTO_MS - GOLPE_SONIDO_MS)
                reproductor.start()
            } catch (e: Exception) {
                reproductor.release() // la intro se saltó antes de que sonara
            }
        }
        launch { lados.animateTo(1f, tween(620, easing = EaseOutQuint)) }
        launch {
            delay(INICIO_RAYO_MS)
            rayo.animateTo(1f, tween((IMPACTO_MS - INICIO_RAYO_MS).toInt(), easing = EaseInRayo))
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            destello.snapTo(1f)
            destello.animateTo(0f, tween(560, easing = LinearOutSlowInEasing))
        }
        delay(1200)
        salida.animateTo(1f, tween(320, easing = FastOutLinearInEasing))
        onFin()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - salida.value }
            .background(Color.Black)
            .pointerInput(Unit) {
                // Tocar salta la intro (y evita que el toque llegue a la app de debajo)
                detectTapGestures {
                    scope.launch {
                        salida.animateTo(1f, tween(180))
                        onFin()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val ancho = minOf(maxWidth * 0.58f, 320.dp)
        val densidad = LocalDensity.current
        // Cada letra parte justo desde fuera de su lateral de la pantalla
        val recorrido = with(densidad) { (maxWidth / 2 + ancho / 2).toPx() }
        val altoPx = with(densidad) { ancho.toPx() } / PROPORCION

        Box(
            Modifier
                .width(ancho)
                .aspectRatio(PROPORCION)
                .graphicsLayer {
                    val s = 1f + 0.03f * destello.value + 0.05f * salida.value
                    scaleX = s; scaleY = s
                }
        ) {
            // S: desde la izquierda
            Capa(R.drawable.intro_s, Modifier.graphicsLayer {
                translationX = -recorrido * (1f - lados.value)
                alpha = (lados.value * 1.8f).coerceAtMost(1f)
            })
            // O: desde la derecha
            Capa(R.drawable.intro_o, Modifier.graphicsLayer {
                translationX = recorrido * (1f - lados.value)
                alpha = (lados.value * 1.8f).coerceAtMost(1f)
            })
            // Halo del rayo en el impacto (el desenfoque solo existe en Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Capa(R.drawable.intro_rayo, Modifier
                    .graphicsLayer {
                        val s = 1f + 0.10f * (1f - destello.value)
                        scaleX = s; scaleY = s
                        alpha = 0.55f * destello.value
                    }
                    .blur(14.dp))
            }
            // Rayo: cae en diagonal, siguiendo su propia inclinación
            Capa(R.drawable.intro_rayo, Modifier.graphicsLayer {
                val p = rayo.value
                translationX = altoPx * 0.46f * (1f - p)   // 0,46/0,75 ≈ inclinación del rayo
                translationY = -altoPx * 0.75f * (1f - p)
                alpha = (p * 2.5f).coerceAtMost(1f)
            })
        }
    }
}

@Composable
private fun Capa(id: Int, modifier: Modifier) {
    Image(
        painter = painterResource(id),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Prepara el sonido de la intro (sin reproducirlo). Usa el canal de sonidos del sistema
 * (no el de música), así que respeta su volumen, y devuelve null si el móvil está en
 * silencio o en vibración. Sin sonido no pasa nada: la animación sigue igual.
 */
private fun prepararSonidoIntro(ctx: Context): MediaPlayer? = try {
    val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) null
    else {
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        MediaPlayer.create(ctx, R.raw.intro_noba, atributos, audio.generateAudioSessionId())?.apply {
            setOnCompletionListener { it.release() }
            setOnErrorListener { mp, _, _ -> mp.release(); true }
        }
    }
} catch (_: Exception) {
    null
}
