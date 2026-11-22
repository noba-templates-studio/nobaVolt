@file:OptIn(ExperimentalMaterial3Api::class)

package com.noba.volt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AjustesScreen(vm: AppViewModel, onMenu: () -> Unit) {
    val a = vm.ajustes

    // Campos de texto: se guardan con el botón "Guardar cambios"
    var empresa by remember(vm.versionDatos) { mutableStateOf(a.empresa) }
    var nif by remember(vm.versionDatos) { mutableStateOf(a.nif) }
    var telefono by remember(vm.versionDatos) { mutableStateOf(a.telefono) }
    var email by remember(vm.versionDatos) { mutableStateOf(a.email) }
    var direccion by remember(vm.versionDatos) { mutableStateOf(a.direccion) }
    var iva by remember(vm.versionDatos) { mutableStateOf(a.ivaPct.fmt()) }
    var validez by remember(vm.versionDatos) { mutableStateOf(a.validezDias.toString()) }
    var umbral by remember(vm.versionDatos) { mutableStateOf(a.umbralStock.fmt()) }
    val plataforma = LocalPlataforma.current
    var mensajeCopia by remember { mutableStateOf<String?>(null) }
    /** Texto de la copia elegida, pendiente de confirmar. */
    var copiaPendiente by remember { mutableStateOf<String?>(null) }
    val importarCopia = rememberSelectorDeArchivo { texto ->
        if (texto != null) copiaPendiente = texto
    }
    var confirmar by remember { mutableStateOf<String?>(null) }

    val ivaNum = iva.aNumero()
    val validezNum = validez.trim().toIntOrNull()
    val umbralNum = umbral.aNumero()
    val valido = ivaNum != null && ivaNum in 0.0..100.0 && validezNum != null && validezNum > 0 &&
        umbralNum != null && umbralNum >= 0.0
    val nuevos = if (valido) a.copy(
        empresa = empresa.trim(),
        nif = nif.trim(),
        telefono = telefono.trim(),
        email = email.trim(),
        direccion = direccion.trim(),
        ivaPct = ivaNum ?: a.ivaPct,
        validezDias = validezNum ?: a.validezDias,
        umbralStock = umbralNum ?: a.umbralStock
    ) else a
    val hayCambios = valido && nuevos != a

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BarraSuperior("Ajustes", onMenu = onMenu) },
        bottomBar = {
            if (hayCambios || !valido) {
                BarraInferior {
                    Button(
                        onClick = { vm.guardarAjustes(nuevos) },
                        enabled = hayCambios,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text(if (valido) "Guardar cambios" else "Revisa los campos marcados") }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Empresa ----
            Bloque("Tu empresa", "Aparece en la cabecera de los PDF.")
            Campo(empresa, { empresa = it }, "Nombre o razón social", capitalizar = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(nif, { nif = it }, "NIF / CIF", Modifier.weight(1f))
                Campo(telefono, { telefono = it }, "Teléfono", Modifier.weight(1f), teclado = KeyboardType.Phone)
            }
            Campo(email, { email = it }, "Email", teclado = KeyboardType.Email)
            Campo(direccion, { direccion = it }, "Dirección", capitalizar = true)

            // ---- Facturación ----
            Bloque("Presupuestos y facturas", "El IVA nuevo solo se aplica a los documentos que crees a partir de ahora.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo(
                    iva, { iva = it }, "IVA (%)", Modifier.weight(1f),
                    teclado = KeyboardType.Decimal, error = ivaNum == null || ivaNum !in 0.0..100.0
                )
                Campo(
                    validez, { validez = it }, "Validez (días)", Modifier.weight(1f),
                    teclado = KeyboardType.Number, error = validezNum == null || validezNum <= 0
                )
            }

            // ---- Inventario ----
            Bloque("Inventario", "Avisos automáticos cuando un material se está acabando.")
            Campo(
                umbral, { umbral = it }, "Avisar cuando queden menos de…",
                teclado = KeyboardType.Decimal, error = umbralNum == null || umbralNum < 0.0
            )
            Interruptor(
                titulo = "Descontar stock al facturar",
                texto = "Al generar una factura se restan del inventario los materiales usados.",
                activo = a.descontarStock,
                onCambio = { vm.guardarAjustes(vm.ajustes.copy(descontarStock = it)) }
            )

            // ---- Apariencia ----
            Bloque("Apariencia", null)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SISTEMA" to "Automático", "CLARO" to "Claro", "OSCURO" to "Oscuro").forEach { (clave, texto) ->
                    FiltroChip(texto, a.tema == clave) { vm.guardarAjustes(vm.ajustes.copy(tema = clave)) }
                }
            }
            Interruptor(
                titulo = "Sonido al abrir",
                texto = "Suena brevemente con la animación de inicio. Nunca suena con el móvil en silencio o vibración.",
                activo = a.sonidoInicio,
                onCambio = { vm.guardarAjustes(vm.ajustes.copy(sonidoInicio = it)) }
            )

            // ---- Datos ----
            Bloque("Copia de seguridad", "Guarda todos tus presupuestos, facturas, inventario y ajustes en un archivo (por ejemplo en Drive o por correo) y recupéralos en otro móvil.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        try {
                            plataforma.compartir(vm.exportarCopia(), "application/json", "Guardar copia de seguridad")
                        } catch (e: Exception) {
                            mensajeCopia = "No se ha podido crear la copia: ${e.message}"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Ic.Exportar, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar")
                }
                OutlinedButton(
                    onClick = { importarCopia() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Ic.Importar, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Importar")
                }
            }
            mensajeCopia?.let { Ayuda(it) }

            Bloque("Datos", null)
            OutlinedButton(onClick = { confirmar = "inventario" }, modifier = Modifier.fillMaxWidth()) {
                Text("Restaurar inventario de ejemplo")
            }
            OutlinedButton(onClick = { confirmar = "documentos" }, modifier = Modifier.fillMaxWidth()) {
                Text("Borrar todos los presupuestos y facturas", color = MaterialTheme.colorScheme.error)
            }

            // ---- Acerca de ----
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LogoNoba(alto = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Noba Volt · versión 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    copiaPendiente?.let { texto ->
        ConfirmarDialogo(
            titulo = "Restaurar copia",
            texto = "Se sustituirán tus presupuestos, facturas, inventario y ajustes actuales por los de la copia.",
            confirmar = "Restaurar",
            onConfirmar = { mensajeCopia = vm.importarCopia(texto); copiaPendiente = null },
            onCancelar = { copiaPendiente = null }
        )
    }

    when (confirmar) {
        "inventario" -> ConfirmarDialogo(
            titulo = "Restaurar inventario",
            texto = "Se sustituirá tu inventario actual (precios y existencias) por el de ejemplo.",
            confirmar = "Restaurar",
            onConfirmar = { vm.restaurarInventario(); confirmar = null },
            onCancelar = { confirmar = null }
        )
        "documentos" -> ConfirmarDialogo(
            titulo = "Borrar documentos",
            texto = "Se eliminarán todos los presupuestos y facturas. No se puede deshacer.",
            confirmar = "Borrar todo",
            onConfirmar = { vm.borrarTodosLosDocumentos(); confirmar = null },
            onCancelar = { confirmar = null }
        )
    }
}

@Composable
private fun Bloque(titulo: String, texto: String?) {
    Column(Modifier.padding(top = 14.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        if (texto != null) {
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    teclado: KeyboardType = KeyboardType.Text,
    capitalizar: Boolean = false,
    error: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = if (capitalizar) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
        ),
        modifier = modifier
    )
}

@Composable
private fun Interruptor(titulo: String, texto: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
        )
    }
}

@Composable
private fun Ayuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
