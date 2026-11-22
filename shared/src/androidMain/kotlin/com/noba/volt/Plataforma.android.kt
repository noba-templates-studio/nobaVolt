package com.noba.volt

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Implementación Android. Usa el contexto de la aplicación (no el de la Activity) para no
 * retener pantallas en memoria; por eso las Activity se abren con FLAG_ACTIVITY_NEW_TASK.
 */
class PlataformaAndroid(contexto: Context) : Plataforma {
    private val ctx = contexto.applicationContext

    // Mismo archivo de preferencias que la versión anterior: quien actualice conserva sus datos
    private val prefs = ctx.getSharedPreferences("electromanager", Context.MODE_PRIVATE)

    override fun leer(clave: String): String? = prefs.getString(clave, null)

    override fun guardar(valores: Map<String, String>) {
        prefs.edit().apply { valores.forEach { (k, v) -> putString(k, v) } }.apply()
    }

    override fun guardarTemporal(nombre: String, datos: ByteArray): String {
        val dir = File(ctx.cacheDir, "compartidos").apply { mkdirs() }
        val f = File(dir, nombre)
        f.writeBytes(datos)
        return f.absolutePath
    }

    private fun uri(ruta: String) =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(ruta))

    override fun abrirPdf(ruta: String) {
        val ver = Intent(Intent.ACTION_VIEW).setDataAndType(uri(ruta), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(ver)
        } catch (e: ActivityNotFoundException) {
            // Sin visor de PDF instalado: se ofrece compartirlo
            compartir(ruta, "application/pdf", "Abrir PDF con…")
        }
    }

    override fun compartir(ruta: String, tipoMime: String, titulo: String) {
        val enviar = Intent(Intent.ACTION_SEND).setType(tipoMime)
            .putExtra(Intent.EXTRA_STREAM, uri(ruta))
            .putExtra(Intent.EXTRA_SUBJECT, File(ruta).name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(
            Intent.createChooser(enviar, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun animacionesActivas(): Boolean =
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    override val desenfoqueDisponible: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun prepararSonido(datos: ByteArray): Sonido? {
        val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return null
        return try {
            val f = File(ctx.cacheDir, "intro_noba.m4a")
            if (!f.exists() || f.length() != datos.size.toLong()) f.writeBytes(datos)
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                // Canal de sonidos del sistema (no el de música): respeta su volumen
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(f.absolutePath)
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { p, _, _ -> p.release(); true }
            mp.prepare()
            object : Sonido {
                override fun reproducir() = mp.start()
                override fun liberar() = mp.release()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Borra los restos del antiguo asistente con IA (el modelo para el móvil ocupaba ~2,6 GB). */
    override fun limpiezaInicial() {
        File(ctx.filesDir, "modelos").takeIf { it.exists() }?.deleteRecursively()
        ctx.getExternalFilesDir(null)?.let { File(it, "modelos") }?.takeIf { it.exists() }?.deleteRecursively()
    }
}

actual fun ahoraMillis(): Long = System.currentTimeMillis()

actual fun fechaHoy(): String = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date())

actual fun horaActual(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

@Composable
actual fun ManejarAtras(activo: Boolean, alVolver: () -> Unit) {
    BackHandler(enabled = activo, onBack = alVolver)
}

@Composable
actual fun BarrasDelSistema(oscuro: Boolean) {
    // Iconos de la barra de estado oscuros sobre fondo blanco y claros sobre fondo negro
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !oscuro
                isAppearanceLightNavigationBars = !oscuro
            }
        }
    }
}

@Composable
actual fun rememberSelectorDeArchivo(alLeer: (String?) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            alLeer(null)
        } else {
            val texto = try {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            alLeer(texto)
        }
    }
    return { lanzador.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
}
