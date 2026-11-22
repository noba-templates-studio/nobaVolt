package com.noba.volt

import kotlin.test.Test
import kotlin.test.assertTrue

/** Genera un PDF de ejemplo (varias páginas, tildes, €) para revisarlo visualmente en la compilación automática. */
class PdfPruebaTest {
    @Test
    fun generaPdfDeEjemplo() {
        val lineas = (1..34).map { i ->
            Linea(
                concepto = if (i % 7 == 0) "Instalación de punto de luz con mecanismo, caja de empotrar y cableado de 1,5 mm² hasta el cuadro"
                else "Material eléctrico nº $i",
                cantidad = (i % 5 + 1).toDouble(),
                precio = 3.5 * i,
                unidad = if (i % 3 == 0) "m" else "ud"
            )
        }
        val d = Documento(
            id = 1L, tipo = FACTURA, numero = "F-007", fecha = "22/11/2026",
            cliente = "Comunidad de Propietarios Peñalara, 12",
            direccion = "C/ Peñalara 12, 28040 Madrid", telefono = "600 123 456",
            instalacion = "Renovación del cuadro eléctrico general y sustitución del cableado de zonas comunes.\nIncluye boletín.",
            lineas = lineas, ivaPct = 21.0
        )
        val ajustes = Ajustes(
            empresa = "Santa Olalla Montajes Eléctricos", nif = "B12345678",
            telefono = "910 000 000", email = "info@ejemplo.es", direccion = "Polígono Sur, nave 4, Toledo"
        )
        val pdf = PdfGenerator.crear(d, ajustes, refPresupuesto = "P-012")
        assertTrue(pdf.decodeToString(0, 8) == "%PDF-1.4", "cabecera PDF")
        val ruta = PlataformaIos.guardarTemporal("prueba_noba.pdf", pdf)
        println("PDF_PRUEBA=$ruta (${pdf.size} bytes)")
    }
}
