package com.noba.volt

import kotlin.math.abs
import kotlin.math.roundToLong

const val PRESUPUESTO = "PRESUPUESTO"
const val FACTURA = "FACTURA"

/** Una línea de un presupuesto o factura (copia del artículo en el momento de crearlo). */
data class Linea(
    val concepto: String,
    val cantidad: Double,
    val precio: Double,
    val unidad: String = "ud",
    /** Artículo del inventario del que procede (para descontar stock). */
    val articuloId: Long? = null
) {
    val total get() = cantidad * precio
}

data class Documento(
    val id: Long,
    val tipo: String, // PRESUPUESTO o FACTURA
    val numero: String,
    val fecha: String,
    val cliente: String,
    val direccion: String,
    val instalacion: String,
    val lineas: List<Linea>,
    val telefono: String = "",
    /** En una factura: id del presupuesto del que procede. */
    val origenId: Long? = null,
    /** IVA aplicado (%). Se guarda en cada documento para que no cambie si cambias el IVA en Ajustes. */
    val ivaPct: Double = 21.0
) {
    val base get() = lineas.sumOf { it.total }
    val iva get() = base * ivaPct / 100.0
    val total get() = base + iva
    val esFactura get() = tipo == FACTURA
}

/** Categorías del inventario. Las marcadas como servicio no llevan control de stock. */
enum class Categoria(val etiqueta: String, val servicio: Boolean = false) {
    CABLEADO("Cableado"),
    CANALIZACION("Canalización"),
    MECANISMOS("Mecanismos"),
    ILUMINACION("Iluminación"),
    PROTECCIONES("Protecciones"),
    OTROS("Otros materiales"),
    MANO_DE_OBRA("Mano de obra", servicio = true),
    DESPLAZAMIENTO("Desplazamiento", servicio = true),
    TRAMITES("Trámites y revisiones", servicio = true);

    companion object {
        fun de(nombre: String?): Categoria = entries.firstOrNull { it.name == nombre } ?: OTROS
    }
}

/** Un material o servicio del inventario. */
data class Articulo(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val unidad: String,
    val categoria: Categoria,
    /** Existencias. null = no se controla el stock de este artículo. */
    val stock: Double? = null
) {
    val controlaStock get() = !categoria.servicio && stock != null
    fun stockBajo(umbral: Double): Boolean = controlaStock && (stock ?: 0.0) < umbral
}

/** Configuración de la app (pantalla Ajustes). */
data class Ajustes(
    val empresa: String = "Tu empresa",
    val nif: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val ivaPct: Double = 21.0,
    val validezDias: Int = 30,
    val umbralStock: Double = 5.0,
    val descontarStock: Boolean = true,
    /** SISTEMA, CLARO u OSCURO */
    val tema: String = "SISTEMA",
    /** Sonido breve con la animación de inicio (nunca suena con el móvil en silencio). */
    val sonidoInicio: Boolean = true
)

/** Datos del presupuesto que se está creando en el asistente de 2 pasos. */
data class Borrador(
    val cliente: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val instalacion: String = "",
    /** id del artículo -> cantidad elegida */
    val cantidades: Map<Long, Double> = emptyMap(),
    /** Conceptos libres que no están en el inventario. */
    val extras: List<Linea> = emptyList(),
    /** Si se está editando un presupuesto existente, su id. */
    val editandoId: Long? = null
) {
    val vacio get() = cliente.isBlank() && cantidades.isEmpty() && extras.isEmpty()
}

/** Inventario de ejemplo para el primer arranque. Precios y existencias orientativos y editables. */
fun inventarioInicial(): List<Articulo> {
    var id = 1L
    fun a(nombre: String, precio: Double, unidad: String, cat: Categoria, stock: Double? = null) =
        Articulo(id++, nombre, precio, unidad, cat, stock)
    return listOf(
        a("Cable 1,5 mm²", 0.45, "m", Categoria.CABLEADO, 200.0),
        a("Cable 2,5 mm²", 0.65, "m", Categoria.CABLEADO, 150.0),
        a("Cable 6 mm²", 1.60, "m", Categoria.CABLEADO, 40.0),
        a("Cable de red Cat6", 0.55, "m", Categoria.CABLEADO, 80.0),
        a("Tubo corrugado 20 mm", 0.35, "m", Categoria.CANALIZACION, 100.0),
        a("Tubo corrugado 25 mm", 0.45, "m", Categoria.CANALIZACION, 50.0),
        a("Canaleta 40x20", 2.80, "m", Categoria.CANALIZACION, 12.0),
        a("Caja de registro", 3.50, "ud", Categoria.CANALIZACION, 15.0),
        a("Caja de mecanismo", 0.60, "ud", Categoria.CANALIZACION, 40.0),
        a("Enchufe Schuko", 4.50, "ud", Categoria.MECANISMOS, 25.0),
        a("Interruptor", 3.90, "ud", Categoria.MECANISMOS, 20.0),
        a("Conmutador", 4.80, "ud", Categoria.MECANISMOS, 10.0),
        a("Toma TV / datos", 7.50, "ud", Categoria.MECANISMOS, 6.0),
        a("Downlight LED", 12.00, "ud", Categoria.ILUMINACION, 18.0),
        a("Pantalla LED 120 cm", 24.00, "ud", Categoria.ILUMINACION, 4.0),
        a("Bombilla LED E27", 3.50, "ud", Categoria.ILUMINACION, 30.0),
        a("Magnetotérmico 2P 16 A", 9.50, "ud", Categoria.PROTECCIONES, 10.0),
        a("Magnetotérmico 2P 25 A", 10.50, "ud", Categoria.PROTECCIONES, 6.0),
        a("Diferencial 40 A 30 mA", 38.00, "ud", Categoria.PROTECCIONES, 3.0),
        a("Cuadro eléctrico 12 módulos", 25.00, "ud", Categoria.PROTECCIONES, 5.0),
        a("Mano de obra oficial", 35.00, "h", Categoria.MANO_DE_OBRA),
        a("Mano de obra ayudante", 25.00, "h", Categoria.MANO_DE_OBRA),
        a("Desplazamiento", 20.00, "ud", Categoria.DESPLAZAMIENTO),
        a("Revisión de instalación", 60.00, "ud", Categoria.TRAMITES),
        a("Boletín / certificado (CIE)", 120.00, "ud", Categoria.TRAMITES)
    )
}

/** Agrupa los miles con punto: 1234567 -> "1.234.567". */
private fun Long.conMiles(): String {
    val t = abs(this).toString()
    val sb = StringBuilder()
    t.forEachIndexed { i, c ->
        if (i > 0 && (t.length - i) % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return if (this < 0) "-$sb" else sb.toString()
}

/** Formato español con dos decimales: 1234.5 -> "1.234,50" (sin depender de Locale de Java). */
fun Double.dosDecimales(miles: Boolean = true): String {
    val centimos = (abs(this) * 100.0).roundToLong()
    val entero = centimos / 100
    val dec = (centimos % 100).toString().padStart(2, '0')
    val signo = if (this < 0 && centimos != 0L) "-" else ""
    return signo + (if (miles) entero.conMiles() else entero.toString()) + "," + dec
}

fun Double.eur(): String = "${dosDecimales()} €"
fun Double.fmt(): String =
    if (this % 1.0 == 0.0) toLong().toString() else dosDecimales(miles = false).trimEnd('0').trimEnd(',')

/** Convierte "12,5" o "12.5" en número. */
fun String.aNumero(): Double? = trim().replace(',', '.').toDoubleOrNull()

fun hoy(): String = fechaHoy()

/** Iniciales para el avatar del cliente ("Juan Pérez" -> "JP"). */
fun String.iniciales(): String =
    trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
