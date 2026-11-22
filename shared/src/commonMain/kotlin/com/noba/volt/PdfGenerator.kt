package com.noba.volt

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Genera el PDF de presupuestos y facturas en Kotlin puro, igual en Android e iOS.
 * Usa las fuentes estándar Helvetica (las llevan todos los lectores de PDF) y un tamaño A4.
 */
object PdfGenerator {
    private const val ANCHO = 595f // A4 en puntos
    private const val ALTO = 842f
    private const val IZQ = 48f
    private const val DER = 547f

    private val NEGRO = Rgb(0x0A, 0x0A, 0x0A)
    private val GRIS = Rgb(0x71, 0x71, 0x7A)
    private val LINEA = Rgb(0xE4, 0xE4, 0xE7)
    private val VERDE = Rgb(0x16, 0xA3, 0x4A)

    fun nombreArchivo(d: Documento) = "${d.tipo}_${d.numero}.pdf"

    /**
     * Crea el PDF del documento.
     * @param refPresupuesto número del presupuesto de origen (solo en facturas).
     */
    fun crear(d: Documento, ajustes: Ajustes, refPresupuesto: String? = null): ByteArray {
        val pdf = DocumentoPdf()
        var n = 1
        var c = pdf.nuevaPagina()

        val txt = Estilo(Fuente.NORMAL, 9.5f, NEGRO)
        val gris = txt.copy(color = GRIS)
        val grisPeq = gris.copy(tam = 8f, espaciado = 0.08f)
        val neg = txt.copy(fuente = Fuente.NEGRITA)
        val der = txt.copy(alineacion = Alin.DERECHA)
        val grisDer = gris.copy(alineacion = Alin.DERECHA)
        val grisPeqDer = grisPeq.copy(alineacion = Alin.DERECHA)
        val titulo = neg.copy(tam = 22f, alineacion = Alin.DERECHA, espaciado = -0.02f)
        val empresa = neg.copy(tam = 13f)
        val totalTxt = neg.copy(tam = 14f, alineacion = Alin.DERECHA)
        val marcaTxt = grisPeq.copy(alineacion = Alin.CENTRO, espaciado = 0f, opacidad = 0.55f)

        var y = 0f

        /** Pequeña marca de agua de Noba al pie de cada página. */
        fun marcaDeAgua(p: PaginaPdf, pagina: Int) {
            val alto = 11f
            val ancho = alto * LOGO_ANCHO / LOGO_ALTO
            p.logo((ANCHO - ancho) / 2f, ALTO - 34f, ancho, alto, opacidad = 0.235f)
            p.texto("Página $pagina", ANCHO / 2f, ALTO - 14f, marcaTxt)
        }

        fun cabeceraTabla() {
            c.texto("CONCEPTO", IZQ, y, grisPeq)
            c.texto("CANTIDAD", 365f, y, grisPeqDer)
            c.texto("PRECIO", 455f, y, grisPeqDer)
            c.texto("IMPORTE", DER, y, grisPeqDer)
            y += 8f
            c.linea(IZQ, y, DER, y, NEGRO, 1f)
            y += 18f
        }

        fun salto(limite: Float = 760f, conCabecera: Boolean = false) {
            if (y > limite) {
                marcaDeAgua(c, n)
                n++
                c = pdf.nuevaPagina()
                y = 60f
                if (conCabecera) cabeceraTabla()
            }
        }

        // ----- Cabecera: empresa a la izquierda, tipo de documento a la derecha -----
        y = 64f
        c.texto(ajustes.empresa.ifBlank { "Tu empresa" }, IZQ, y, empresa)
        var yEmp = y + 16f
        listOfNotNull(
            ajustes.nif.takeIf { it.isNotBlank() }?.let { "NIF $it" },
            ajustes.direccion.takeIf { it.isNotBlank() },
            listOf(ajustes.telefono, ajustes.email).filter { it.isNotBlank() }.joinToString("  ·  ").ifBlank { null }
        ).forEach { linea ->
            c.texto(linea, IZQ, yEmp, gris); yEmp += 13f
        }

        c.texto(if (d.esFactura) "Factura" else "Presupuesto", DER, y + 4f, titulo)
        c.texto("Nº ${d.numero}", DER, y + 22f, grisDer)
        c.texto(d.fecha, DER, y + 35f, grisDer)

        y = max(yEmp, y + 40f) + 16f
        c.linea(IZQ, y, DER, y, LINEA, 0.7f)
        y += 26f

        // ----- Cliente -----
        c.texto("CLIENTE", IZQ, y, grisPeq); y += 15f
        c.texto(d.cliente, IZQ, y, neg); y += 14f
        if (d.direccion.isNotBlank()) { c.texto(d.direccion, IZQ, y, txt); y += 13f }
        if (d.telefono.isNotBlank()) { c.texto("Tel. ${d.telefono}", IZQ, y, txt); y += 13f }
        if (!refPresupuesto.isNullOrBlank()) { c.texto("Ref. presupuesto $refPresupuesto", IZQ, y, gris); y += 13f }
        y += 14f

        // ----- Instalación -----
        if (d.instalacion.isNotBlank()) {
            c.texto("TRABAJO A REALIZAR", IZQ, y, grisPeq); y += 15f
            partir(d.instalacion, txt, DER - IZQ).forEach { salto(); c.texto(it, IZQ, y, txt); y += 13f }
            y += 16f
        }

        // ----- Tabla -----
        cabeceraTabla()
        d.lineas.forEach { l ->
            salto(conCabecera = true)
            partir(l.concepto, txt, 250f).forEachIndexed { i, s ->
                c.texto(s, IZQ, y, txt)
                if (i == 0) {
                    c.texto("${l.cantidad.fmt()} ${l.unidad}", 365f, y, der)
                    c.texto(l.precio.eur(), 455f, y, der)
                    c.texto(l.total.eur(), DER, y, der)
                }
                y += 13f
            }
            y += 4f
            c.linea(IZQ, y - 8f, DER, y - 8f, LINEA, 0.7f)
            y += 6f
        }

        // ----- Totales -----
        salto(680f)
        y += 10f
        fun fila(a: String, b: String) {
            c.texto(a, 455f, y, grisDer); c.texto(b, DER, y, der); y += 17f
        }
        fila("Base imponible", d.base.eur())
        fila("IVA ${d.ivaPct.fmt()} %", d.iva.eur())
        y += 4f
        c.linea(340f, y - 10f, DER, y - 10f, VERDE, 2f)
        y += 8f
        c.texto("Total", 455f, y, totalTxt)
        c.texto(d.total.eur(), DER, y, totalTxt)

        y += 40f
        salto(780f)
        c.texto(
            if (d.esFactura) "Gracias por su confianza."
            else "Presupuesto válido durante ${ajustes.validezDias} días desde su fecha de emisión.",
            IZQ, y, gris
        )

        marcaDeAgua(c, n)
        return pdf.terminar()
    }

    /** Parte un texto en líneas que caben en [ancho] puntos (respeta los saltos de línea). */
    private fun partir(t: String, e: Estilo, ancho: Float): List<String> {
        val out = mutableListOf<String>()
        for (parrafo in t.split("\n")) {
            var cur = ""
            for (w in parrafo.split(" ")) {
                val prueba = if (cur.isEmpty()) w else "$cur $w"
                if (e.medir(prueba) > ancho && cur.isNotEmpty()) {
                    out.add(cur); cur = w
                } else cur = prueba
            }
            out.add(cur)
        }
        return out
    }
}

// =====================================================================
//  Escritor de PDF mínimo (PDF 1.4): texto, líneas y la marca de agua
// =====================================================================

internal data class Rgb(val r: Int, val g: Int, val b: Int)

internal enum class Fuente(val recurso: String, val anchos: IntArray) {
    NORMAL("/F1", ANCHOS_HELVETICA),
    NEGRITA("/F2", ANCHOS_HELVETICA_NEGRITA)
}

internal enum class Alin { IZQUIERDA, DERECHA, CENTRO }

internal data class Estilo(
    val fuente: Fuente,
    val tam: Float,
    val color: Rgb,
    val alineacion: Alin = Alin.IZQUIERDA,
    /** Espaciado entre letras en "em" (igual que Paint.letterSpacing de Android). */
    val espaciado: Float = 0f,
    val opacidad: Float = 1f
) {
    /** Ancho del texto en puntos. */
    fun medir(s: String): Float {
        val codigos = aWinAnsi(s)
        var unidades = 0
        for (b in codigos) {
            val c = b.toInt() and 0xFF
            unidades += if (c >= 32) fuente.anchos[c - 32] else 0
        }
        return unidades * tam / 1000f + codigos.size * espaciado * tam
    }
}

/** Convierte un texto a la codificación WinAnsi de las fuentes estándar del PDF (incluye á, ñ, €, ², ·…). */
internal fun aWinAnsi(s: String): ByteArray {
    val out = ByteArray(s.length)
    s.forEachIndexed { i, ch ->
        val c = ch.code
        out[i] = when {
            c in 32..126 -> c
            c in 0xA0..0xFF -> c
            ch == '€' -> 0x80
            ch == '‚' -> 0x82
            ch == '…' -> 0x85
            ch == '‘' -> 0x91
            ch == '’' -> 0x92
            ch == '“' -> 0x93
            ch == '”' -> 0x94
            ch == '•' -> 0x95
            ch == '–' -> 0x96
            ch == '—' -> 0x97
            ch == '\t' -> 32
            else -> '?'.code
        }.toByte()
    }
    return out
}

/** Número con hasta 3 decimales y punto decimal, como exige la sintaxis de PDF. */
private fun Float.pdf(): String {
    val v = (this * 1000f).roundToLong()
    val signo = if (v < 0) "-" else ""
    val a = kotlin.math.abs(v)
    val entero = a / 1000
    val dec = (a % 1000).toString().padStart(3, '0').trimEnd('0')
    return if (dec.isEmpty()) "$signo$entero" else "$signo$entero.$dec"
}

private fun Rgb.pdf() = "${(r / 255f).pdf()} ${(g / 255f).pdf()} ${(b / 255f).pdf()}"

/** Bytes que crecen según se escriben (no hay ByteArrayOutputStream en código común). */
private class Bytes {
    private var datos = ByteArray(64 * 1024)
    var tam = 0
        private set

    fun add(b: ByteArray) {
        if (tam + b.size > datos.size) datos = datos.copyOf(max(datos.size * 2, tam + b.size))
        b.copyInto(datos, tam)
        tam += b.size
    }

    fun add(s: String) = add(s.encodeToByteArray())
    fun toByteArray() = datos.copyOf(tam)
}

internal class PaginaPdf {
    private val sb = StringBuilder()
    val contenido: String get() = sb.toString()

    /** Dibuja texto con la línea base en (x, y), con y medida desde arriba como en Android. */
    fun texto(s: String, x: Float, y: Float, e: Estilo) {
        if (s.isEmpty()) return
        val xReal = when (e.alineacion) {
            Alin.IZQUIERDA -> x
            Alin.DERECHA -> x - e.medir(s)
            Alin.CENTRO -> x - e.medir(s) / 2f
        }
        if (e.opacidad < 1f) sb.append("q /").append(gs(e.opacidad)).append(" gs\n")
        sb.append("BT ").append(e.fuente.recurso).append(' ').append(e.tam.pdf()).append(" Tf ")
            .append((e.espaciado * e.tam).pdf()).append(" Tc ")
            .append(e.color.pdf()).append(" rg ")
            .append(xReal.pdf()).append(' ').append((842f - y).pdf()).append(" Td (")
        for (b in aWinAnsi(s)) {
            val c = b.toInt() and 0xFF
            when {
                c == '('.code || c == ')'.code || c == '\\'.code -> sb.append('\\').append(c.toChar())
                c >= 128 -> sb.append('\\').append(c.toString(8).padStart(3, '0'))
                else -> sb.append(c.toChar())
            }
        }
        sb.append(") Tj ET\n")
        if (e.opacidad < 1f) sb.append("Q\n")
    }

    fun linea(x1: Float, y1: Float, x2: Float, y2: Float, color: Rgb, grosor: Float) {
        sb.append(color.pdf()).append(" RG ").append(grosor.pdf()).append(" w ")
            .append(x1.pdf()).append(' ').append((842f - y1).pdf()).append(" m ")
            .append(x2.pdf()).append(' ').append((842f - y2).pdf()).append(" l S\n")
    }

    /** Dibuja el logo de Noba con su esquina superior izquierda en (x, y). */
    fun logo(x: Float, y: Float, ancho: Float, alto: Float, opacidad: Float) {
        sb.append("q /").append(gs(opacidad)).append(" gs ")
            .append(ancho.pdf()).append(" 0 0 ").append(alto.pdf()).append(' ')
            .append(x.pdf()).append(' ').append((842f - y - alto).pdf()).append(" cm /Logo Do Q\n")
    }

    private fun gs(opacidad: Float) = if (opacidad < 0.4f) "GSm" else "GSt"
}

internal class DocumentoPdf {
    private val paginas = mutableListOf<PaginaPdf>()

    fun nuevaPagina(): PaginaPdf = PaginaPdf().also { paginas.add(it) }

    @OptIn(ExperimentalEncodingApi::class)
    fun terminar(): ByteArray {
        val out = Bytes()
        val offsets = mutableListOf<Int>()
        fun objeto(cuerpo: String) {
            offsets.add(out.tam)
            out.add("${offsets.size} 0 obj\n$cuerpo\nendobj\n")
        }
        fun objetoConStream(dic: String, datos: ByteArray) {
            offsets.add(out.tam)
            out.add("${offsets.size} 0 obj\n<< $dic /Length ${datos.size} >>\nstream\n")
            out.add(datos)
            out.add("\nendstream\nendobj\n")
        }

        out.add("%PDF-1.4\n")
        out.add(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        // Objetos fijos: 1 catálogo, 2 páginas, 3-4 fuentes, 5-6 transparencias, 7-8 logo
        val primeraPagina = 9
        val kids = paginas.indices.joinToString(" ") { "${primeraPagina + it * 2} 0 R" }
        objeto("<< /Type /Catalog /Pages 2 0 R >>")
        objeto("<< /Type /Pages /Kids [$kids] /Count ${paginas.size} >>")
        objeto("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        objeto("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>")
        objeto("<< /Type /ExtGState /ca 0.235 /CA 0.235 >>")
        objeto("<< /Type /ExtGState /ca 0.55 /CA 0.55 >>")
        // Logo: imagen de 1 píxel casi negro recortada por la máscara de transparencia del logo
        objetoConStream(
            "/Type /XObject /Subtype /Image /Width 1 /Height 1 /ColorSpace /DeviceGray /BitsPerComponent 8 /SMask 8 0 R",
            byteArrayOf(0x0A)
        )
        objetoConStream(
            "/Type /XObject /Subtype /Image /Width $LOGO_ANCHO /Height $LOGO_ALTO /ColorSpace /DeviceGray " +
                "/BitsPerComponent 8 /Filter /FlateDecode",
            Base64.decode(LOGO_ALFA_B64)
        )

        val recursos = "<< /Font << /F1 3 0 R /F2 4 0 R >> /ExtGState << /GSm 5 0 R /GSt 6 0 R >> " +
            "/XObject << /Logo 7 0 R >> >>"
        paginas.forEachIndexed { i, p ->
            val contenidoId = primeraPagina + i * 2 + 1
            objeto(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources $recursos " +
                    "/Contents $contenidoId 0 R >>"
            )
            objetoConStream("", p.contenido.encodeToByteArray())
        }

        val xref = out.tam
        val sb = StringBuilder()
        sb.append("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { sb.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        sb.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        out.add(sb.toString())
        return out.toByteArray()
    }
}

/** Genera el PDF en segundo plano, lo guarda en un archivo temporal y devuelve su ruta. */
suspend fun guardarPdf(plataforma: Plataforma, d: Documento, ajustes: Ajustes, refPresupuesto: String? = null): String =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        plataforma.guardarTemporal(PdfGenerator.nombreArchivo(d), PdfGenerator.crear(d, ajustes, refPresupuesto))
    }
