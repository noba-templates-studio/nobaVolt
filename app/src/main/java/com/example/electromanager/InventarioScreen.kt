@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.electromanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InventarioScreen(vm: AppViewModel, onMenu: () -> Unit) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var creando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Articulo?>(null) }
    var borrando by remember { mutableStateOf<Articulo?>(null) }

    val umbral = vm.ajustes.umbralStock
    val soloBajo = vm.verSoloStockBajo
    val nBajo = vm.articulosStockBajo().size
    val grupos = vm.inventario
        .filter { !soloBajo || it.stockBajo(umbral) }
        .filter { busqueda.isBlank() || it.nombre.contains(busqueda.trim(), ignoreCase = true) }
        .sortedWith(compareBy<Articulo>({ it.categoria.ordinal }, { it.nombre.lowercase() }))
        .groupBy { it.categoria }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BarraSuperior("Inventario", onMenu = onMenu) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creando = true },
                icon = { Icon(Ic.Mas, null) },
                text = { Text("Añadir") },
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { CampoBusqueda(busqueda, { busqueda = it }, "Buscar en el inventario…") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroChip("Todo (${vm.inventario.size})", !soloBajo) { vm.verSoloStockBajo = false }
                    FiltroChip("Stock bajo ($nBajo)", soloBajo) { vm.verSoloStockBajo = true }
                }
            }
            item {
                Text(
                    "Aviso cuando un material baja de ${umbral.fmt()} unidades. Cámbialo en Ajustes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (grupos.isEmpty()) {
                item {
                    if (soloBajo) EstadoVacio(Ic.Ok, "Todo en orden", "Ningún material está por debajo del mínimo.")
                    else EstadoVacio(Ic.Inventario, "Sin artículos", "Añade materiales y servicios con el botón Añadir.")
                }
            }
            grupos.forEach { (cat, articulos) ->
                item(key = "cat_${cat.name}") {
                    TituloSeccion(cat.etiqueta, Modifier.padding(top = 14.dp, bottom = 2.dp))
                }
                items(articulos, key = { it.id }) { a ->
                    FilaArticulo(a, umbral, onClick = { editando = a }, onBorrar = { borrando = a })
                }
            }
        }
    }

    if (creando) {
        ArticuloDialogo(
            inicial = null,
            onGuardar = { vm.guardarArticulo(it); creando = false },
            onCancelar = { creando = false }
        )
    }
    editando?.let { a ->
        key(a.id) {
            ArticuloDialogo(
                inicial = a,
                onGuardar = { vm.guardarArticulo(it); editando = null },
                onCancelar = { editando = null }
            )
        }
    }
    borrando?.let { a ->
        ConfirmarDialogo(
            titulo = "Eliminar artículo",
            texto = "¿Quitar «${a.nombre}» del inventario? Los presupuestos ya hechos no cambian.",
            confirmar = "Eliminar",
            onConfirmar = { vm.borrarArticulo(a); borrando = null },
            onCancelar = { borrando = null }
        )
    }
}

@Composable
private fun FilaArticulo(a: Articulo, umbral: Double, onClick: () -> Unit, onBorrar: () -> Unit) {
    val bajo = a.stockBajo(umbral)
    Tarjeta(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colorBorde = if (bajo) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else null
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconoCirculo(a.categoria.icono, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${a.precio.eur()} / ${a.unidad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val stock = a.stock
            when {
                a.categoria.servicio -> Etiqueta(
                    "Servicio",
                    fondo = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                stock == null -> Etiqueta(
                    "Sin control",
                    fondo = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                bajo -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.Aviso, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${(stock ?: 0.0).fmt()} ${a.unidad}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> Text("${(stock ?: 0.0).fmt()} ${a.unidad}", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = onBorrar) {
                Icon(Ic.Borrar, "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Crear o editar un material/servicio del inventario. */
@Composable
fun ArticuloDialogo(inicial: Articulo?, onGuardar: (Articulo) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var precio by remember { mutableStateOf(inicial?.precio?.fmt() ?: "") }
    var unidad by remember { mutableStateOf(inicial?.unidad ?: "ud") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: Categoria.OTROS) }
    var stock by remember { mutableStateOf(inicial?.stock?.fmt() ?: "") }
    val precioNum = precio.aNumero()
    val stockNum = stock.aNumero()
    val stockValido = stock.isBlank() || (stockNum != null && stockNum >= 0.0)
    val valido = nombre.isNotBlank() && precioNum != null && precioNum >= 0.0 && stockValido

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (inicial == null) "Nuevo artículo" else "Editar artículo") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = precio,
                        onValueChange = { precio = it },
                        label = { Text("Precio (€)") },
                        singleLine = true,
                        isError = precio.isNotBlank() && precioNum == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unidad,
                        onValueChange = { unidad = it },
                        label = { Text("Unidad") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ud", "m", "m²", "h", "kg", "rollo").forEach { u ->
                        FiltroChip(u, unidad == u) { unidad = u }
                    }
                }
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Categoria.entries.forEach { c ->
                        FiltroChip(c.etiqueta, categoria == c) { categoria = c }
                    }
                }
                if (categoria.servicio) {
                    Text(
                        "Los servicios no llevan control de stock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Existencias ($unidad)") },
                        supportingText = { Text("Déjalo vacío si no quieres controlar el stock") },
                        singleLine = true,
                        isError = !stockValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    onGuardar(
                        Articulo(
                            id = inicial?.id ?: 0L,
                            nombre = nombre.trim(),
                            precio = precioNum ?: 0.0,
                            unidad = unidad.trim().ifBlank { "ud" },
                            categoria = categoria,
                            stock = if (categoria.servicio) null else stockNum
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
