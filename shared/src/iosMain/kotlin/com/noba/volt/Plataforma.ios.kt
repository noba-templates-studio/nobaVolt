@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.noba.volt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject
import platform.posix.memcpy

/** Implementación iOS. */
object PlataformaIos : Plataforma {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun leer(clave: String): String? = defaults.stringForKey(clave)

    override fun guardar(valores: Map<String, String>) {
        valores.forEach { (k, v) -> defaults.setObject(v, forKey = k) }
    }

    override fun guardarTemporal(nombre: String, datos: ByteArray): String {
        val dir = NSTemporaryDirectory().trimEnd('/') + "/compartidos"
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir, withIntermediateDirectories = true, attributes = null, error = null
        )
        val ruta = "$dir/$nombre"
        if (!datos.aNSData().writeToFile(ruta, atomically = true)) {
            throw Exception("no se ha podido guardar el archivo")
        }
        return ruta
    }

    // El visor de documentos y su delegado deben seguir vivos mientras se muestran
    private var interaccion: UIDocumentInteractionController? = null
    private val delegadoVisor = object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
        override fun documentInteractionControllerViewControllerForPreview(
            controller: UIDocumentInteractionController
        ): UIViewController = controladorVisible() ?: UIViewController()
    }

    override fun abrirPdf(ruta: String) {
        val c = UIDocumentInteractionController.interactionControllerWithURL(NSURL.fileURLWithPath(ruta))
        c.delegate = delegadoVisor
        interaccion = c
        if (!c.presentPreviewAnimated(true)) compartir(ruta, "application/pdf", "PDF")
    }

    override fun compartir(ruta: String, tipoMime: String, titulo: String) {
        val raiz = controladorVisible() ?: return
        val hoja = UIActivityViewController(
            activityItems = listOf(NSURL.fileURLWithPath(ruta)),
            applicationActivities = null
        )
        // En iPad la hoja se muestra como globo y necesita un punto de anclaje
        hoja.popoverPresentationController?.let { p ->
            p.sourceView = raiz.view
            p.sourceRect = raiz.view.bounds
        }
        raiz.presentViewController(hoja, animated = true, completion = null)
    }

    override fun animacionesActivas(): Boolean = !UIAccessibilityIsReduceMotionEnabled()

    override val desenfoqueDisponible: Boolean = true

    // Se mantiene una referencia mientras suena (si no, iOS lo liberaría y se cortaría)
    private var reproductor: AVAudioPlayer? = null

    override fun prepararSonido(datos: ByteArray): Sonido? = try {
        // "Ambient": se mezcla con la música que suene y respeta el interruptor de silencio
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, error = null)
        val p = AVAudioPlayer(data = datos.aNSData(), error = null)
        p.prepareToPlay()
        reproductor = p
        object : Sonido {
            override fun reproducir() {
                p.play()
            }

            override fun liberar() {
                p.stop()
                if (reproductor === p) reproductor = null
            }
        }
    } catch (e: Exception) {
        null
    }
}

/** La pantalla que está visible ahora mismo (para presentar encima la hoja de compartir, el visor…). */
@Suppress("DEPRECATION")
internal fun controladorVisible(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

internal fun ByteArray.aNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

internal fun NSData.aByteArray(): ByteArray {
    val n = length.toInt()
    if (n == 0) return ByteArray(0)
    val out = ByteArray(n)
    out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

actual fun ahoraMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun fechaHoy(): String = NSDateFormatter().apply { dateFormat = "dd/MM/yyyy" }.stringFromDate(NSDate())

actual fun horaActual(): Int = NSCalendar.currentCalendar.component(NSCalendarUnitHour, fromDate = NSDate()).toInt()

/** En iOS no hay botón "atrás": se vuelve con las flechas de la propia app. */
@Composable
actual fun ManejarAtras(activo: Boolean, alVolver: () -> Unit) {
}

/** En iOS la barra de estado sigue automáticamente el modo claro/oscuro del sistema. */
@Composable
actual fun BarrasDelSistema(oscuro: Boolean) {
}

@Composable
actual fun rememberSelectorDeArchivo(alLeer: (String?) -> Unit): () -> Unit {
    val alLeerActual by rememberUpdatedState(alLeer)
    // El selector guarda una referencia débil a su delegado: lo retenemos aquí
    val delegado = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val texto = url?.let { NSData.dataWithContentsOfURL(it)?.aByteArray()?.decodeToString() }
                alLeerActual(texto)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                alLeerActual(null)
            }
        }
    }
    return remember {
        {
            // asCopy = true: iOS entrega una copia dentro de la app, sin permisos extra
            val selector = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText, UTTypeData),
                asCopy = true
            )
            selector.delegate = delegado
            controladorVisible()?.presentViewController(selector, animated = true, completion = null)
        }
    }
}
