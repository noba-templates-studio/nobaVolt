@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.electromanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DetalleScreen(vm: AppViewModel, p: Pantalla.Detalle) {
    val d = vm.documento(p.docId)
    if (d == null) {
        // El documento se acaba de borrar (se ve un instante durante la transición)
        Box(Modifier.fillMaxSize())
    } else {
        DetalleContenido(vm, d, p)
    }
}

@Composable
private fun DetalleContenido(vm: AppViewModel, d: Documento, p: Pantalla.Detalle) {
    val ctx = LocalContext.current
    val factura = if (d.esFactura) null else vm.facturaDe(d)
    val origen = if (d.esFactura) vm.origenDe(d) else null
    var confirmarBorrado by remember { mutableStateOf(false) }
    var generando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /** Genera el PDF en segundo plano (no congela la pantalla) y luego lo abre o lo comparte. */
    fun conPdf(doc: Documento, ref: String?, accion: (java.io.File) -> Unit) {
        if (generando) return
        generando = true
        val ajustes = vm.ajustes
        scope.launch {
            try {
                val f = withContext(Dispatchers.IO) { PdfGenerator.crear(ctx, doc, ajustes, ref) }
                accion(f)
            } catch (e: Exception) {
                android.widget.Toast.makeText(ctx, "No se ha podido crear el PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                generando = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                (if (d.esFactura) "Factura " else "Presupuesto ") + d.numero,
                onAtras = { vm.atras() },
                acciones = {
                    IconButton(onClick = { confirmarBorrado = true }) { Icon(Ic.Borrar, "Eliminar") }
                }
            )
        },
        bottomBar = {
            BarraInferior {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!d.esFactura) {
                        if (factura == null) {
                            Button(
                                onClick = {
                                    val f = vm.generarFactura(d, volver = p)
                                    conPdf(f, d.numero) { PdfGenerator.abrir(ctx, it) }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Ic.Factura, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generar factura")
                            }
                            OutlinedButton(
                                onClick = { vm.editarPresupuesto(d) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Ic.Editar, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Editar presupuesto")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { vm.ir(Pantalla.Detalle(factura.id, volver = p)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Ver factura ${factura.numero}") }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { conPdf(d, origen?.numero) { PdfGenerator.abrir(ctx, it) } },
                            enabled = !generando,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (generando) "Creando…" else "Ver PDF") }
                        OutlinedButton(
                            onClick = { conPdf(d, origen?.numero) { PdfGenerator.compartir(ctx, it) } },
                            enabled = !generando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Ic.Compartir, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Estado
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(d.cliente, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.cliente, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${if (d.esFactura) "Factura" else "Presupuesto"} · ${d.fecha}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    when {
                        factura != null -> Etiqueta("Facturado")
                        d.esFactura -> Etiqueta("Emitida")
                        else -> Etiqueta(
                            "Pendiente",
                            fondo = MaterialTheme.colorScheme.tertiaryContainer,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Cliente
            if (d.direccion.isNotBlank() || d.telefono.isNotBlank()) {
                item {
                    Seccion("Cliente") {
                        if (d.direccion.isNotBlank()) Text(d.direccion)
                        if (d.telefono.isNotBlank()) Text("Tel. ${d.telefono}")
                    }
                }
            }

            if (d.instalacion.isNotBlank()) {
                item { Seccion("Instalación a realizar") { Text(d.instalacion) } }
            }

            // Conceptos + totales
            item {
                Seccion("Materiales y servicios") {
                    d.lineas.forEachIndexed { i, l ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.concepto, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${l.cantidad.fmt()} ${l.unidad} × ${l.precio.eur()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(l.total.eur(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface)
                    FilaTotal("Base imponible", d.base.eur())
                    FilaTotal("IVA ${d.ivaPct.fmt()} %", d.iva.eur())
                    Spacer(Modifier.height(4.dp))
                    FilaTotal("Total", d.total.eur(), destacado = true)
                }
            }

            if (origen != null) {
                item {
                    TextButton(onClick = { vm.ir(Pantalla.Detalle(origen.id, volver = p)) }) {
                        Text("Ver presupuesto de origen ${origen.numero}", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }

    if (confirmarBorrado) {
        ConfirmarDialogo(
            titulo = "Eliminar ${if (d.esFactura) "factura" else "presupuesto"}",
            texto = "¿Seguro que quieres eliminar ${d.numero} de ${d.cliente}? No se puede deshacer.",
            confirmar = "Eliminar",
            onConfirmar = {
                confirmarBorrado = false
                vm.borrarDocumento(d)
                vm.atras()
            },
            onCancelar = { confirmarBorrado = false }
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Tarjeta(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            TituloSeccion(titulo)
            Spacer(Modifier.height(8.dp))
            contenido()
        }
    }
}

@Composable
private fun FilaTotal(texto: String, valor: String, destacado: Boolean = false) {
    val estilo = if (destacado) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            texto,
            modifier = Modifier.weight(1f),
            style = estilo,
            color = if (destacado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(valor, style = estilo, color = if (destacado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
    }
}
