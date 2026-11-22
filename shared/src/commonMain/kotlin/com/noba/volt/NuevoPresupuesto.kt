@file:OptIn(ExperimentalMaterial3Api::class)

package com.noba.volt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// =====================================================================
//  PASO 1 · Datos del cliente
// =====================================================================

@Composable
fun DatosClienteScreen(vm: AppViewModel) {
    val b = vm.borrador
    val titulo = b.editandoId?.let { id -> vm.documento(id)?.let { "Editar ${it.numero}" } } ?: "Nuevo presupuesto"

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BarraSuperior(titulo, onAtras = { vm.atras() }) },
        bottomBar = {
            BarraInferior {
                Button(
                    onClick = { vm.ir(Pantalla.NuevoMateriales) },
                    enabled = b.cliente.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Siguiente")
                    Spacer(Modifier.width(8.dp))
                    Icon(Ic.Siguiente, null, Modifier.size(18.dp))
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IndicadorPaso(1)
            Text("Datos del cliente", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = b.cliente,
                onValueChange = { vm.borrador = vm.borrador.copy(cliente = it) },
                label = { Text("Cliente *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = b.telefono,
                onValueChange = { vm.borrador = vm.borrador.copy(telefono = it) },
                label = { Text("Teléfono") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = b.direccion,
                onValueChange = { vm.borrador = vm.borrador.copy(direccion = it) },
                label = { Text("Dirección") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = b.instalacion,
                onValueChange = { vm.borrador = vm.borrador.copy(instalacion = it) },
                label = { Text("Instalación a realizar") },
                placeholder = { Text("Ej. Cambio de cuadro eléctrico y 6 puntos de luz en salón") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "* Obligatorio. En el siguiente paso elegirás los materiales y servicios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================
//  PASO 2 · Materiales y servicios
// =====================================================================

@Composable
fun SeleccionScreen(vm: AppViewModel) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf<Categoria?>(null) }
    var cantidadDe by remember { mutableStateOf<Articulo?>(null) }
    var creando by remember { mutableStateOf(false) }
    var conceptoLibre by remember { mutableStateOf(false) }
    val editando = vm.borrador.editandoId != null

    val cantidades = vm.borrador.cantidades
    val extras = vm.borrador.extras
    val umbral = vm.ajustes.umbralStock
    val grupos = vm.inventario
        .filter { filtro == null || it.categoria == filtro }
        .filter {
            busqueda.isBlank() || it.nombre.contains(busqueda.trim(), ignoreCase = true) ||
                Similitud.parecido(busqueda, it.nombre) >= 50
        }
        .sortedBy { it.categoria.ordinal }
        .groupBy { it.categoria }
    val lineas = vm.lineasBorrador()
    val totalConIva = lineas.sumOf { it.total } * (1 + vm.ajustes.ivaPct / 100.0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BarraSuperior(
                "Materiales y servicios",
                onAtras = { vm.atras() },
                acciones = {
                    IconButton(onClick = { creando = true }) { Icon(Ic.Mas, "Añadir artículo al inventario") }
                }
            )
        },
        bottomBar = {
            BarraInferior {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (lineas.isEmpty()) "Nada seleccionado"
                            else "${lineas.size} concepto${if (lineas.size == 1) "" else "s"} · IVA incl.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(totalConIva.eur(), style = MaterialTheme.typography.titleLarge)
                    }
                    Button(
                        onClick = {
                            val doc = vm.finalizarPresupuesto()
                            val ajustes = vm.ajustes
                            // El PDF se genera fuera del hilo principal para que la app no se congele
                            // Se genera en el ViewModel: sigue aunque esta pantalla se cierre al ir al detalle
                            vm.abrirPdf(doc, ajustes)
                        },
                        enabled = lineas.isNotEmpty(),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Ic.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (editando) "Guardar" else "Finalizar")
                    }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IndicadorPaso(2)
                CampoBusqueda(busqueda, { busqueda = it }, "Buscar material o servicio…")
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroChip("Todo", filtro == null) { filtro = null }
                Categoria.entries.forEach { c ->
                    FiltroChip(c.etiqueta, filtro == c) { filtro = if (filtro == c) null else c }
                }
            }

            if (grupos.isEmpty()) {
                EstadoVacio(Ic.Buscar, "No hay artículos", "Prueba otra búsqueda o añade uno con el botón + de arriba.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item(key = "extras", span = { GridItemSpan(maxLineSpan) }) {
                        ConceptosLibres(
                            extras = extras,
                            onAnadir = { conceptoLibre = true },
                            onQuitar = { vm.quitarExtra(it) }
                        )
                    }
                    grupos.forEach { (cat, articulos) ->
                        item(key = "cat_${cat.name}", span = { GridItemSpan(maxLineSpan) }) {
                            TituloSeccion(cat.etiqueta, Modifier.padding(top = 6.dp))
                        }
                        items(articulos, key = { it.id }) { a ->
                            TarjetaArticulo(
                                a = a,
                                cantidad = cantidades[a.id] ?: 0.0,
                                umbral = umbral,
                                onCambiar = { vm.cambiarCantidad(a.id, it) },
                                onEditarCantidad = { cantidadDe = a }
                            )
                        }
                    }
                }
            }
        }
    }

    cantidadDe?.let { a ->
        CantidadDialogo(
            a = a,
            actual = cantidades[a.id] ?: 0.0,
            onAceptar = { vm.cambiarCantidad(a.id, it); cantidadDe = null },
            onCancelar = { cantidadDe = null }
        )
    }
    if (conceptoLibre) {
        ConceptoLibreDialogo(
            onAceptar = { vm.anadirExtra(it); conceptoLibre = false },
            onCancelar = { conceptoLibre = false }
        )
    }
    if (creando) {
        ArticuloDialogo(
            inicial = null,
            onGuardar = {
                val nuevo = vm.guardarArticulo(it)
                vm.cambiarCantidad(nuevo.id, 1.0)
                creando = false
            },
            onCancelar = { creando = false }
        )
    }
}

/** Chip de filtro en blanco y negro. */
@Composable
fun FiltroChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
            selectedLabelColor = MaterialTheme.colorScheme.background,
            selectedLeadingIconColor = MaterialTheme.colorScheme.background
        )
    )
}

/** Tarjeta visual de un material o servicio. Tocarla suma 1 unidad. */
@Composable
fun TarjetaArticulo(
    a: Articulo,
    cantidad: Double,
    umbral: Double,
    onCambiar: (Double) -> Unit,
    onEditarCantidad: () -> Unit
) {
    val elegido = cantidad > 0.0
    val stock = a.stock
    val superaStock = a.controlaStock && stock != null && cantidad > stock
    Tarjeta(onClick = { onCambiar(cantidad + 1) }, resaltada = elegido) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconoCirculo(
                a.categoria.icono, 48.dp,
                fondo = if (elegido) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                color = if (elegido) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                a.nombre,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${a.precio.eur()} / ${a.unidad}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Existencias
            if (a.controlaStock && stock != null) {
                val bajo = a.stockBajo(umbral)
                Text(
                    when {
                        superaStock -> "Supera el stock (${stock.fmt()})"
                        bajo -> "Quedan ${stock.fmt()} ${a.unidad}"
                        else -> "Stock: ${stock.fmt()} ${a.unidad}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (superaStock || bajo) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (elegido) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { onCambiar(cantidad - 1) },
                            modifier = Modifier.size(34.dp)
                        ) { Icon(Ic.Menos, "Restar", Modifier.size(18.dp)) }
                        Text(
                            "${cantidad.fmt()} ${a.unidad}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onEditarCantidad)
                                .padding(vertical = 8.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { onCambiar(cantidad + 1) },
                            modifier = Modifier.size(34.dp)
                        ) { Icon(Ic.Mas, "Sumar", Modifier.size(18.dp)) }
                    }
                } else {
                    Text(
                        "Toca para añadir",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/** Para escribir una cantidad exacta (p. ej. 37,5 m de cable). */
@Composable
fun CantidadDialogo(a: Articulo, actual: Double, onAceptar: (Double) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(if (actual > 0.0) actual.fmt() else "") }
    val valor = texto.aNumero()
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(a.nombre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Cantidad (${a.unidad})") },
                    singleLine = true,
                    isError = texto.isNotBlank() && valor == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (valor != null) {
                    Text(
                        "Importe: ${(valor * a.precio).eur()} + IVA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val stock = a.stock
                if (a.controlaStock && stock != null) {
                    Text(
                        "En stock: ${stock.fmt()} ${a.unidad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (valor != null && valor > stock) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Pon 0 para quitarlo del presupuesto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAceptar(valor ?: 0.0) }, enabled = texto.isBlank() || valor != null) {
                Text("Aceptar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Conceptos que no están en el inventario (trabajos especiales, material puntual...). */
@Composable
private fun ConceptosLibres(extras: List<Linea>, onAnadir: () -> Unit, onQuitar: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        extras.forEachIndexed { i, l ->
            Tarjeta(modifier = Modifier.fillMaxWidth(), resaltada = true) {
                Row(
                    Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(l.concepto, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${l.cantidad.fmt()} ${l.unidad} × ${l.precio.eur()} = ${l.total.eur()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onQuitar(i) }) {
                        Icon(Ic.Cerrar, "Quitar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        TextButton(onClick = onAnadir) {
            Icon(Ic.Mas, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Añadir concepto libre")
        }
    }
}

@Composable
private fun ConceptoLibreDialogo(onAceptar: (Linea) -> Unit, onCancelar: () -> Unit) {
    var concepto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var unidad by remember { mutableStateOf("ud") }
    var precio by remember { mutableStateOf("") }
    val c = cantidad.aNumero()
    val p = precio.aNumero()
    val valido = concepto.isNotBlank() && c != null && c > 0.0 && p != null && p >= 0.0
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Concepto libre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto") },
                    placeholder = { Text("Ej. Instalación de punto de recarga") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        isError = cantidad.isNotBlank() && (c == null || c <= 0.0),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unidad,
                        onValueChange = { unidad = it },
                        label = { Text("Unidad") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio unitario sin IVA (€)") },
                    singleLine = true,
                    isError = precio.isNotBlank() && p == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (valido && c != null && p != null) {
                    Text(
                        "Importe: ${(c * p).eur()} + IVA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    onAceptar(
                        Linea(
                            concepto = concepto.trim().replaceFirstChar { it.uppercase() },
                            cantidad = c ?: 1.0,
                            precio = p ?: 0.0,
                            unidad = unidad.trim().ifBlank { "ud" }
                        )
                    )
                }
            ) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
