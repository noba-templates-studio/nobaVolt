package com.noba.volt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lo poco que depende del sistema operativo. Android e iOS aportan cada uno su implementación
 * (PlataformaAndroid y PlataformaIos); todo lo demás de la app es código común.
 */
interface Plataforma {
    // ---- Almacenamiento persistente (clave → texto) ----
    fun leer(clave: String): String?
    fun guardar(valores: Map<String, String>)

    // ---- Archivos para compartir (PDF, copias de seguridad) ----
    /** Guarda los datos en un archivo temporal y devuelve su ruta. */
    fun guardarTemporal(nombre: String, datos: ByteArray): String
    /** Abre un PDF en el visor del sistema. */
    fun abrirPdf(ruta: String)
    /** Abre el menú de compartir del sistema (WhatsApp, correo, Archivos…). */
    fun compartir(ruta: String, tipoMime: String, titulo: String)

    // ---- Intro ----
    /** false si el usuario ha desactivado o reducido las animaciones del sistema. */
    fun animacionesActivas(): Boolean
    /** Si el desenfoque (Modifier.blur) tiene efecto en este dispositivo. */
    val desenfoqueDisponible: Boolean
    /** Prepara un sonido corto; null si no debe sonar (p. ej. móvil en silencio). */
    fun prepararSonido(datos: ByteArray): Sonido?

    /** Tareas de mantenimiento al arrancar (p. ej. borrar restos de versiones antiguas). */
    fun limpiezaInicial() {}
}

/** Un sonido ya cargado y listo para sonar al instante. */
interface Sonido {
    fun reproducir()
    fun liberar()
}

val LocalPlataforma = staticCompositionLocalOf<Plataforma> { error("Plataforma no inicializada") }

// ---- Fecha y hora del sistema ----
expect fun ahoraMillis(): Long
/** Fecha de hoy en formato dd/MM/yyyy. */
expect fun fechaHoy(): String
/** Hora local (0-23), para el saludo. */
expect fun horaActual(): Int

// ---- Integraciones de interfaz ----

/** Botón/gesto "atrás" del sistema (en iOS no existe y no hace nada). */
@Composable
expect fun ManejarAtras(activo: Boolean, alVolver: () -> Unit)

/** Ajusta el color de los iconos de las barras del sistema según el tema. */
@Composable
expect fun BarrasDelSistema(oscuro: Boolean)

/**
 * Selector de archivos del sistema para importar una copia de seguridad.
 * Devuelve una función que lo abre; [alLeer] recibe el texto del archivo o null si se cancela o falla.
 */
@Composable
expect fun rememberSelectorDeArchivo(alLeer: (String?) -> Unit): () -> Unit
