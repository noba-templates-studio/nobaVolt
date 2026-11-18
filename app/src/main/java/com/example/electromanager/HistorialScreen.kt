@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.electromanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Historial de presupuestos o de facturas (según [tipo]). */
@Composable
fun HistorialScreen(vm: AppViewModel, tipo: String, onMenu: () -> Unit) {
    val esFactura = tipo == FACTURA
    var busqueda by rememberSaveable(tipo) { mutableStateOf("") }
    val estaPantalla = if (esFactura) Pantalla.Facturas else Pantalla.Presupuestos

    val todos = vm.documentos.filter { it.tipo == tipo }.sortedByDescending { it.id }
    val q = busqueda.trim()
    val lista = todos.filter {
        q.isEmpty() || it.cliente.contains(q, true) || it.numero.contains(q, true) ||
            it.direccion.contains(q, true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(if (esFactura) "Facturas" else "Presupuestos", onMenu = onMenu)
        },
        floatingActionButton = {
            if (!esFactura) {
                FloatingActionButton(
                    onClick = { vm.nuevoPresupuesto() },
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ) { Icon(Ic.Mas, "Nuevo presupuesto") }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (esFactura) {
                    Resumen(
                        "Total facturado", todos.sumOf { it.total }.eur(), "${todos.size} facturas",
                        MaterialTheme.colorScheme.secondary
                    )
                } else {
                    val pendientes = todos.filter { vm.facturaDe(it) == null }
                    Resumen(
                        "Pendiente de facturar",
                        pendientes.sumOf { it.total }.eur(),
                        "${pendientes.size} de ${todos.size} presupuestos",
                        MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            item { CampoBusqueda(busqueda, { busqueda = it }, "Buscar por cliente, número o dirección…") }

            if (lista.isEmpty()) {
                item {
                    when {
                        todos.isEmpty() && esFactura -> EstadoVacio(
                            Ic.Factura, "Aún no hay facturas",
                            "Abre un presupuesto y pulsa «Generar factura»."
                        )
                        todos.isEmpty() -> EstadoVacio(
                            Ic.Presupuesto, "Aún no hay presupuestos",
                            "Pulsa + para crear el primero."
                        )
                        else -> EstadoVacio(Ic.Buscar, "Sin resultados", "No hay nada que coincida con «$q».")
                    }
                }
            }
            items(lista, key = { it.id }) { d ->
                TarjetaDocumento(
                    d,
                    facturado = !esFactura && vm.facturaDe(d) != null,
                    onClick = { vm.ir(Pantalla.Detalle(d.id, estaPantalla)) }
                )
            }
        }
    }
}

@Composable
private fun Resumen(titulo: String, valor: String, detalle: String, acento: Color) {
    Tarjeta(modifier = Modifier.fillMaxWidth(), colorBorde = acento.copy(alpha = 0.4f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.labelLarge, color = acento)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(valor, style = MaterialTheme.typography.titleLarge)
        }
    }
}
