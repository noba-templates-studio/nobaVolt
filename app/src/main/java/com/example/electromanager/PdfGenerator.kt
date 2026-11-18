package com.example.electromanager

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    private const val ANCHO = 595 // A4 en puntos
    private const val ALTO = 842
    private const val IZQ = 48f
    private const val DER = 547f

    private val NEGRO = Color.rgb(0x0A, 0x0A, 0x0A)
    private val GRIS = Color.rgb(0x71, 0x71, 0x7A)
    private val LINEA = Color.rgb(0xE4, 0xE4, 0xE7)
    private val VERDE = Color.rgb(0x16, 0xA3, 0x4A)

    private fun fuente(ctx: Context, id: Int, alternativa: Typeface): Typeface =
        try {
            ResourcesCompat.getFont(ctx, id) ?: alternativa
        } catch (e: Exception) {
            alternativa
        }

    private fun partir(t: String, p: Paint, ancho: Float): List<String> {
        val out = mutableListOf<String>()
        for (parrafo in t.split("\n")) {
            var cur = ""
            for (w in parrafo.split(" ")) {
                val test = if (cur.isEmpty()) w else "$cur $w"
                if (p.measureText(test) > ancho && cur.isNotEmpty()) {
                    out.add(cur); cur = w
                } else cur = test
            }
            out.add(cur)
        }
        return out
    }

    /**
     * Genera el PDF del documento en la caché de la app.
     * @param refPresupuesto número del presupuesto de origen (solo en facturas).
     */
    fun crear(ctx: Context, d: Documento, ajustes: Ajustes, refPresupuesto: String? = null): File {
        val regular = fuente(ctx, R.font.inter_regular, Typeface.DEFAULT)
        val semibold = fuente(ctx, R.font.inter_semibold, Typeface.DEFAULT_BOLD)
        val logo: Bitmap? = try {
            BitmapFactory.decodeResource(ctx.resources, R.drawable.logo_noba)
        } catch (e: Exception) {
            null
        }

        val pdf = PdfDocument()
        var n = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, n).create())
        var c = page.canvas

        val txt = Paint().apply { typeface = regular; textSize = 9.5f; isAntiAlias = true; color = NEGRO }
        val gris = Paint(txt).apply { color = GRIS }
        val grisPeq = Paint(gris).apply { textSize = 8f; letterSpacing = 0.08f }
        val neg = Paint(txt).apply { typeface = semibold }
        val der = Paint(txt).apply { textAlign = Paint.Align.RIGHT }
        val grisDer = Paint(gris).apply { textAlign = Paint.Align.RIGHT }
        val grisPeqDer = Paint(grisPeq).apply { textAlign = Paint.Align.RIGHT }
        val titulo = Paint(neg).apply { textSize = 22f; textAlign = Paint.Align.RIGHT; letterSpacing = -0.02f }
        val empresa = Paint(neg).apply { textSize = 13f }
        val totalP = Paint(neg).apply { textSize = 14f; textAlign = Paint.Align.RIGHT }
        val totalTxt = Paint(neg).apply { textSize = 14f; textAlign = Paint.Align.RIGHT }
        val raya = Paint().apply { color = LINEA; strokeWidth = 0.7f }
        val rayaNegra = Paint().apply { color = NEGRO; strokeWidth = 1f }
        val rayaVerde = Paint().apply { color = VERDE; strokeWidth = 2f }
        val marca = Paint().apply { alpha = 60; isFilterBitmap = true }
        val marcaTxt = Paint(grisPeq).apply { textAlign = Paint.Align.CENTER; letterSpacing = 0f; alpha = 140 }

        var y = 0f

        /** Pequeña marca de agua de Noba al pie de cada página. */
        fun marcaDeAgua(canvas: Canvas, pagina: Int) {
            val alto = 11f
            if (logo != null && logo.height > 0) {
                val ancho = alto * logo.width / logo.height
                val x = (ANCHO - ancho) / 2f
                canvas.drawBitmap(logo, null, RectF(x, ALTO - 34f, x + ancho, ALTO - 34f + alto), marca)
            }
            canvas.drawText("Página $pagina", ANCHO / 2f, ALTO - 14f, marcaTxt)
        }

        fun cabeceraTabla() {
            c.drawText("CONCEPTO", IZQ, y, grisPeq)
            c.drawText("CANTIDAD", 365f, y, grisPeqDer)
            c.drawText("PRECIO", 455f, y, grisPeqDer)
            c.drawText("IMPORTE", DER, y, grisPeqDer)
            y += 8f
            c.drawLine(IZQ, y, DER, y, rayaNegra)
            y += 18f
        }

        fun salto(limite: Float = 760f, conCabecera: Boolean = false) {
            if (y > limite) {
                marcaDeAgua(c, n)
                pdf.finishPage(page)
                n++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, n).create())
                c = page.canvas
                y = 60f
                if (conCabecera) cabeceraTabla()
            }
        }

        // ----- Cabecera: empresa a la izquierda, tipo de documento a la derecha -----
        y = 64f
        c.drawText(ajustes.empresa.ifBlank { "Tu empresa" }, IZQ, y, empresa)
        var yEmp = y + 16f
        listOf(
            ajustes.nif.takeIf { it.isNotBlank() }?.let { "NIF $it" },
            ajustes.direccion.takeIf { it.isNotBlank() },
            listOf(ajustes.telefono, ajustes.email).filter { it.isNotBlank() }.joinToString("  ·  ").ifBlank { null }
        ).filterNotNull().forEach { linea ->
            c.drawText(linea, IZQ, yEmp, gris); yEmp += 13f
        }

        c.drawText(if (d.esFactura) "Factura" else "Presupuesto", DER, y + 4f, titulo)
        c.drawText("Nº ${d.numero}", DER, y + 22f, grisDer)
        c.drawText(d.fecha, DER, y + 35f, grisDer)

        y = maxOf(yEmp, y + 40f) + 16f
        c.drawLine(IZQ, y, DER, y, raya)
        y += 26f

        // ----- Cliente -----
        c.drawText("CLIENTE", IZQ, y, grisPeq); y += 15f
        c.drawText(d.cliente, IZQ, y, neg); y += 14f
        if (d.direccion.isNotBlank()) { c.drawText(d.direccion, IZQ, y, txt); y += 13f }
        if (d.telefono.isNotBlank()) { c.drawText("Tel. ${d.telefono}", IZQ, y, txt); y += 13f }
        if (!refPresupuesto.isNullOrBlank()) { c.drawText("Ref. presupuesto $refPresupuesto", IZQ, y, gris); y += 13f }
        y += 14f

        // ----- Instalación -----
        if (d.instalacion.isNotBlank()) {
            c.drawText("TRABAJO A REALIZAR", IZQ, y, grisPeq); y += 15f
            partir(d.instalacion, txt, DER - IZQ).forEach { salto(); c.drawText(it, IZQ, y, txt); y += 13f }
            y += 16f
        }

        // ----- Tabla -----
        cabeceraTabla()
        d.lineas.forEach { l ->
            salto(conCabecera = true)
            partir(l.concepto, txt, 250f).forEachIndexed { i, s ->
                c.drawText(s, IZQ, y, txt)
                if (i == 0) {
                    c.drawText("${l.cantidad.fmt()} ${l.unidad}", 365f, y, der)
                    c.drawText(l.precio.eur(), 455f, y, der)
                    c.drawText(l.total.eur(), DER, y, der)
                }
                y += 13f
            }
            y += 4f
            c.drawLine(IZQ, y - 8f, DER, y - 8f, raya)
            y += 6f
        }

        // ----- Totales -----
        salto(680f)
        y += 10f
        fun fila(a: String, b: String) {
            c.drawText(a, 455f, y, grisDer); c.drawText(b, DER, y, der); y += 17f
        }
        fila("Base imponible", d.base.eur())
        fila("IVA ${d.ivaPct.fmt()} %", d.iva.eur())
        y += 4f
        c.drawLine(340f, y - 10f, DER, y - 10f, rayaVerde)
        y += 8f
        c.drawText("Total", 455f, y, totalTxt)
        c.drawText(d.total.eur(), DER, y, totalP)

        y += 40f
        salto(780f)
        c.drawText(
            if (d.esFactura) "Gracias por su confianza."
            else "Presupuesto válido durante ${ajustes.validezDias} días desde su fecha de emisión.",
            IZQ, y, gris
        )

        marcaDeAgua(c, n)
        pdf.finishPage(page)
        val dir = File(ctx.cacheDir, "pdfs").apply { mkdirs() }
        val f = File(dir, "${d.tipo}_${d.numero}.pdf")
        FileOutputStream(f).use { pdf.writeTo(it) }
        pdf.close()
        return f
    }

    private fun uri(ctx: Context, f: File) =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    /** Abre el PDF en el visor; si no hay visor instalado, abre el menú de compartir. */
    fun abrir(ctx: Context, f: File) {
        val ver = Intent(Intent.ACTION_VIEW).setDataAndType(uri(ctx, f), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(ver)
        } catch (e: ActivityNotFoundException) {
            compartir(ctx, f)
        }
    }

    /** Comparte cualquier archivo de la caché (p. ej. la copia de seguridad). */
    fun compartirArchivo(ctx: Context, f: File, tipo: String, titulo: String) {
        val enviar = Intent(Intent.ACTION_SEND).setType(tipo)
            .putExtra(Intent.EXTRA_STREAM, uri(ctx, f))
            .putExtra(Intent.EXTRA_SUBJECT, f.name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(enviar, titulo))
    }

    /** Enviar por WhatsApp, correo, etc. */
    fun compartir(ctx: Context, f: File) {
        val enviar = Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri(ctx, f))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(enviar, "Enviar PDF"))
    }
}
