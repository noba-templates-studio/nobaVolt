package com.example.electromanager

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Pantallas de la app. */
sealed interface Pantalla {
    data object Inicio : Pantalla
    data object Presupuestos : Pantalla
    data object Facturas : Pantalla
    data object Inventario : Pantalla
    data object Ajustes : Pantalla
    data object NuevoDatos : Pantalla
    data object NuevoMateriales : Pantalla
    data class Detalle(val docId: Long, val volver: Pantalla) : Pantalla
}

/**
 * Estado de toda la app. Al ser un ViewModel sobrevive a los giros de pantalla,
 * así no se pierde un presupuesto a medio hacer.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application get() = getApplication()

    var documentos by mutableStateOf(Store.cargarDocumentos(app))
        private set
    var inventario by mutableStateOf(Store.cargarInventario(app))
        private set
    var ajustes by mutableStateOf(Store.cargarAjustes(app))
        private set
    var pantalla by mutableStateOf<Pantalla>(Pantalla.Inicio)
        private set
    var borrador by mutableStateOf(Borrador())

    /** Artículos que acaban de quedar por debajo del mínimo (muestra un aviso). */
    var avisoStock by mutableStateOf<List<Articulo>>(emptyList())

    /** Cambia cada vez que se restauran los datos, para que los formularios se recarguen. */
    var versionDatos by mutableStateOf(0)
        private set

    /** Si es true, el inventario se abre filtrado por stock bajo. */
    var verSoloStockBajo by mutableStateOf(false)

    private var ultimoId = 0L

    /** Id único y creciente (evita repetir ids si se crean varios elementos en el mismo milisegundo). */
    fun nuevoId(): Long {
        val ahora = System.currentTimeMillis()
        ultimoId = if (ahora > ultimoId) ahora else ultimoId + 1
        return ultimoId
    }

    // ---------- Navegación ----------

    fun ir(p: Pantalla) {
        pantalla = p
    }

    /** Devuelve false si ya estamos en Inicio (el botón atrás cierra la app). */
    fun atras(): Boolean {
        var destino: Pantalla = when (val p = pantalla) {
            Pantalla.Inicio -> return false
            Pantalla.NuevoMateriales -> Pantalla.NuevoDatos
            Pantalla.NuevoDatos -> borrador.editandoId?.let { id ->
                // Al salir de una edición se vuelve al presupuesto y se descartan los cambios
                borrador = Borrador()
                Pantalla.Detalle(id, Pantalla.Presupuestos)
            } ?: Pantalla.Inicio
            is Pantalla.Detalle -> p.volver
            else -> Pantalla.Inicio
        }
        // Salta detalles de documentos que ya se han borrado
        while (destino is Pantalla.Detalle && documento(destino.docId) == null) {
            destino = destino.volver
        }
        pantalla = destino
        return true
    }

    fun abrirInventarioStockBajo() {
        verSoloStockBajo = true
        avisoStock = emptyList()
        pantalla = Pantalla.Inventario
    }

    // ---------- Ajustes ----------

    fun guardarAjustes(a: Ajustes) {
        ajustes = a
        Store.guardarAjustes(ctx, a)
    }

    fun restaurarInventario() {
        inventario = inventarioInicial()
        Store.guardarInventario(ctx, inventario)
        borrador = borrador.copy(cantidades = emptyMap())
    }

    fun borrarTodosLosDocumentos() {
        documentos = emptyList()
        Store.guardarDocumentos(ctx, documentos)
    }

    // ---------- Documentos ----------

    fun documento(id: Long): Documento? = documentos.firstOrNull { it.id == id }

    fun documentoPorNumero(numero: String): Documento? =
        documentos.firstOrNull { it.numero.equals(numero.trim(), ignoreCase = true) }

    /** Factura generada a partir de un presupuesto, si existe. */
    fun facturaDe(presupuesto: Documento): Documento? {
        val docs = documentos
        var mapa = cacheFacturas
        if (cacheDocs !== docs) {
            // Se recalcula solo cuando cambia la lista: las listas largas no se ralentizan
            mapa = docs.filter { it.esFactura && it.origenId != null }.associateBy { it.origenId ?: 0L }
            cacheFacturas = mapa
            cacheDocs = docs
        }
        return mapa[presupuesto.id]
    }

    @Volatile private var cacheDocs: List<Documento>? = null
    @Volatile private var cacheFacturas: Map<Long, Documento> = emptyMap()

    /** Presupuesto del que procede una factura, si existe. */
    fun origenDe(factura: Documento): Documento? = factura.origenId?.let { documento(it) }

    /** Último documento de cada uno de los 3 clientes más recientes. */
    fun ultimosClientes(): List<Documento> =
        documentos.sortedByDescending { it.id }
            .distinctBy { it.cliente.trim().lowercase() }
            .take(3)

    private fun siguienteNumero(tipo: String): String {
        val prefijo = if (tipo == FACTURA) "F" else "P"
        val max = documentos
            .filter { it.tipo == tipo }
            .mapNotNull { it.numero.substringAfter('-').toIntOrNull() }
            .maxOrNull() ?: 0
        return "$prefijo-%03d".format(max + 1)
    }

    private fun guardarDocumento(d: Documento) {
        documentos = documentos.filter { it.id != d.id } + d
        Store.guardarDocumentos(ctx, documentos)
    }

    /** Guarda los cambios de un documento existente. */
    fun actualizarDocumento(d: Documento) = guardarDocumento(d)

    /** Borra un documento. Si es una factura y se descontó stock al crearla, lo devuelve al inventario. */
    fun borrarDocumento(d: Documento) {
        documentos = documentos.filter { it.id != d.id }
        Store.guardarDocumentos(ctx, documentos)
        if (d.esFactura && ajustes.descontarStock) moverStock(d.lineas, signo = +1)
    }

    // ---------- Asistente de nuevo presupuesto ----------

    fun nuevoPresupuesto() {
        borrador = Borrador()
        pantalla = Pantalla.NuevoDatos
    }

    /** Abre un presupuesto existente en el asistente de 2 pasos para modificarlo. */
    fun editarPresupuesto(d: Documento) {
        if (d.esFactura || facturaDe(d) != null) return
        val cantidades = mutableMapOf<Long, Double>()
        val extras = mutableListOf<Linea>()
        d.lineas.forEach { l ->
            val a = inventario.firstOrNull { it.id == l.articuloId }
                ?: inventario.firstOrNull { l.articuloId == null && it.nombre == l.concepto }
            if (a != null && a.precio == l.precio) cantidades[a.id] = (cantidades[a.id] ?: 0.0) + l.cantidad
            else extras.add(l)
        }
        borrador = Borrador(
            cliente = d.cliente, telefono = d.telefono, direccion = d.direccion, instalacion = d.instalacion,
            cantidades = cantidades, extras = extras, editandoId = d.id
        )
        pantalla = Pantalla.NuevoDatos
    }

    fun anadirExtra(l: Linea) {
        borrador = borrador.copy(extras = borrador.extras + l)
    }

    fun quitarExtra(indice: Int) {
        borrador = borrador.copy(extras = borrador.extras.filterIndexed { i, _ -> i != indice })
    }

    /** Empieza un presupuesto con los datos de un cliente anterior. */
    fun nuevoPresupuestoPara(d: Documento) {
        borrador = Borrador(cliente = d.cliente, telefono = d.telefono, direccion = d.direccion)
        pantalla = Pantalla.NuevoDatos
    }

    fun cambiarCantidad(articuloId: Long, cantidad: Double) {
        val nuevas = borrador.cantidades.toMutableMap()
        if (cantidad <= 0.0) nuevas.remove(articuloId) else nuevas[articuloId] = cantidad
        borrador = borrador.copy(cantidades = nuevas)
    }

    /** Líneas del borrador, ordenadas como el inventario (materiales primero, luego servicios). */
    fun lineasBorrador(): List<Linea> =
        inventario
            .filter { (borrador.cantidades[it.id] ?: 0.0) > 0.0 }
            .sortedBy { it.categoria.ordinal }
            .map { Linea(it.nombre, borrador.cantidades.getValue(it.id), it.precio, it.unidad, it.id) } +
            borrador.extras

    /** Crea y guarda un presupuesto. */
    fun crearPresupuesto(
        cliente: String,
        telefono: String,
        direccion: String,
        instalacion: String,
        lineas: List<Linea>
    ): Documento {
        val doc = Documento(
            id = nuevoId(),
            tipo = PRESUPUESTO,
            numero = siguienteNumero(PRESUPUESTO),
            fecha = hoy(),
            cliente = cliente.trim(),
            direccion = direccion.trim(),
            instalacion = instalacion.trim(),
            lineas = lineas,
            telefono = telefono.trim(),
            ivaPct = ajustes.ivaPct
        )
        guardarDocumento(doc)
        return doc
    }

    /** Finaliza el asistente de 2 pasos. Devuelve el documento creado. */
    fun finalizarPresupuesto(): Documento {
        val b = borrador
        val original = b.editandoId?.let { documento(it) }
        val doc = if (original != null && facturaDe(original) == null) {
            original.copy(
                cliente = b.cliente.trim(),
                telefono = b.telefono.trim(),
                direccion = b.direccion.trim(),
                instalacion = b.instalacion.trim(),
                lineas = lineasBorrador()
            ).also { guardarDocumento(it) }
        } else {
            crearPresupuesto(b.cliente, b.telefono, b.direccion, b.instalacion, lineasBorrador())
        }
        borrador = Borrador()
        pantalla = Pantalla.Detalle(doc.id, volver = Pantalla.Presupuestos)
        return doc
    }

    /**
     * Crea la factura de un presupuesto (o devuelve la existente).
     * Si está activado en Ajustes, descuenta del stock los materiales usados.
     * @param volver pantalla a la que vuelve el detalle; null = no navegar.
     */
    fun generarFactura(presupuesto: Documento, volver: Pantalla?): Documento {
        facturaDe(presupuesto)?.let { return it }
        val f = presupuesto.copy(
            id = nuevoId(),
            tipo = FACTURA,
            numero = siguienteNumero(FACTURA),
            fecha = hoy(),
            origenId = presupuesto.id
        )
        guardarDocumento(f)
        if (ajustes.descontarStock) descontarStock(f.lineas)
        if (volver != null) pantalla = Pantalla.Detalle(f.id, volver)
        return f
    }

    // ---------- Stock ----------

    fun articulosStockBajo(): List<Articulo> = inventario.filter { it.stockBajo(ajustes.umbralStock) }

    private fun descontarStock(lineas: List<Linea>) = moverStock(lineas, signo = -1)

    /** Resta (signo -1) o devuelve (signo +1) al stock las cantidades de las líneas. */
    private fun moverStock(lineas: List<Linea>, signo: Int) {
        val umbral = ajustes.umbralStock
        val antes = inventario.filter { it.stockBajo(umbral) }.map { it.id }.toSet()
        var cambios = false
        inventario = inventario.map { a ->
            val usado = lineas
                .filter { l -> l.articuloId == a.id || (l.articuloId == null && l.concepto == a.nombre) }
                .sumOf { it.cantidad }
            val actual = a.stock
            if (usado > 0.0 && a.controlaStock && actual != null) {
                cambios = true
                a.copy(stock = maxOf(0.0, actual + signo * usado))
            } else a
        }
        if (!cambios) return
        Store.guardarInventario(ctx, inventario)
        if (signo < 0) {
            val nuevos = inventario.filter { it.stockBajo(umbral) && it.id !in antes }
            if (nuevos.isNotEmpty()) avisoStock = nuevos
        }
    }

    // ---------- Inventario ----------

    fun guardarArticulo(a: Articulo): Articulo {
        val conId = if (a.id == 0L) a.copy(id = nuevoId()) else a
        inventario = if (inventario.any { it.id == conId.id })
            inventario.map { if (it.id == conId.id) conId else it }
        else
            inventario + conId
        Store.guardarInventario(ctx, inventario)
        return conId
    }

    fun borrarArticulo(a: Articulo) {
        inventario = inventario.filter { it.id != a.id }
        Store.guardarInventario(ctx, inventario)
        cambiarCantidad(a.id, 0.0)
    }

    // ---------- Copia de seguridad ----------

    /** Crea un archivo con todos los datos (documentos, inventario y ajustes). */
    fun exportarCopia(): File {
        val dir = File(ctx.cacheDir, "copias").apply { mkdirs() }
        val f = File(dir, "noba-volt-copia-${hoy().replace('/', '-')}.json")
        f.writeText(Store.exportar(ctx), Charsets.UTF_8)
        return f
    }

    /** Restaura una copia de seguridad. Devuelve un mensaje para mostrar al usuario. */
    fun importarCopia(uri: Uri): String = try {
        val texto = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw Exception("No se ha podido abrir el archivo.")
        Store.importar(ctx, texto)
        documentos = Store.cargarDocumentos(ctx)
        inventario = Store.cargarInventario(ctx)
        ajustes = Store.cargarAjustes(ctx)
        borrador = Borrador()
        versionDatos++
        "Copia restaurada: ${documentos.size} documentos y ${inventario.size} artículos."
    } catch (e: Exception) {
        "No se ha podido restaurar la copia: ${e.message}"
    }

    // ---------- Limpieza ----------

    /** Borra los restos del antiguo asistente con IA (el modelo para el móvil ocupaba ~2,6 GB). */
    private fun limpiarRestosAsistente() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(ctx.filesDir, "modelos").takeIf { it.exists() }?.deleteRecursively()
                ctx.getExternalFilesDir(null)?.let { File(it, "modelos") }?.takeIf { it.exists() }?.deleteRecursively()
            } catch (_: Exception) {
                // No es crítico: si falla se intentará en el próximo arranque
            }
        }
    }

    init {
        Store.purgarAsistente(ctx)
        limpiarRestosAsistente()
    }
}
