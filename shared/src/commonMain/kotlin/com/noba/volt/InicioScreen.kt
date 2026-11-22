@file:OptIn(ExperimentalMaterial3Api::class)

package com.noba.volt

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InicioScreen(vm: AppViewModel, onMenu: () -> Unit) {
    val docs = vm.documentos
    val pendientes = docs.filter { !it.esFactura && vm.facturaDe(it) == null }
    val facturas = docs.filter { it.esFactura }
    val clientes = vm.ultimosClientes()
    val stockBajo = vm.articulosStockBajo()
    val borrador = vm.borrador
    val saludo = when (horaActual()) {
        in 6..13 -> "Buenos días"
        in 14..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                titulo = "Inicio",
                onMenu = onMenu,
                tituloContenido = { LogoNoba(alto = 22.dp) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.nuevoPresupuesto() },
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Ic.Mas, "Nuevo presupuesto", Modifier.size(30.dp))
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Saludo
            item {
                Column {
                    Text(saludo, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        vm.ajustes.empresa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Resumen
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Indicador(
                        titulo = "Pendiente",
                        valor = pendientes.sumOf { it.total }.eur(),
                        detalle = "${pendientes.size} presupuestos",
                        acento = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.ir(Pantalla.Presupuestos) }
                    )
                    Indicador(
                        titulo = "Facturado",
                        valor = facturas.sumOf { it.total }.eur(),
                        detalle = "${facturas.size} facturas",
                        acento = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.ir(Pantalla.Facturas) }
                    )
                }
            }

            // Aviso de stock bajo
            if (stockBajo.isNotEmpty()) {
                item {
                    Tarjeta(
                        onClick = { vm.abrirInventarioStockBajo() },
                        colorBorde = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Ic.Aviso, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (stockBajo.size == 1) "1 material con poco stock"
                                    else "${stockBajo.size} materiales con poco stock",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    stockBajo.joinToString { it.nombre },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Ic.Flecha, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Presupuesto a medias
            if (!borrador.vacio) {
                item {
                    Tarjeta(onClick = { vm.ir(Pantalla.NuevoDatos) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconoCirculo(Ic.Editar, 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (borrador.editandoId != null) "Continuar edición" else "Continuar presupuesto",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    borrador.cliente.ifBlank { "Sin cliente" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Ic.Flecha, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Últimos proyectos
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Últimos proyectos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (clientes.isNotEmpty()) {
                        TextButton(onClick = { vm.ir(Pantalla.Presupuestos) }) { Text("Ver todo") }
                    }
                }
            }
            if (clientes.isEmpty()) {
                item {
                    EstadoVacio(Ic.Presupuesto, "Aún no hay proyectos", "Pulsa + para crear tu primer presupuesto.")
                }
            } else {
                items(clientes, key = { it.id }) { d ->
                    ProyectoReciente(
                        d = d,
                        facturado = d.esFactura || vm.facturaDe(d) != null,
                        onAbrir = { vm.ir(Pantalla.Detalle(d.id, Pantalla.Inicio)) },
                        onNuevo = { vm.nuevoPresupuestoPara(d) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Indicador(
    titulo: String,
    valor: String,
    detalle: String,
    acento: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Tarjeta(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(Modifier.size(8.dp)) { drawCircle(acento) }
                Spacer(Modifier.width(6.dp))
                Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProyectoReciente(d: Documento, facturado: Boolean, onAbrir: () -> Unit, onNuevo: () -> Unit) {
    Tarjeta(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(d.cliente)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    d.cliente.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${d.numero} · ${d.fecha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.total.eur(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    if (facturado) Etiqueta("Facturado")
                    else Etiqueta(
                        "Pendiente",
                        fondo = MaterialTheme.colorScheme.tertiaryContainer,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            IconButton(onClick = onNuevo) {
                Icon(Ic.Mas, "Nuevo presupuesto para ${d.cliente}")
            }
        }
    }
}
