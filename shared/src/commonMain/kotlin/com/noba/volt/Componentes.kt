@file:OptIn(ExperimentalMaterial3Api::class)

package com.noba.volt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.painterResource
import com.noba.volt.recursos.Res
import com.noba.volt.recursos.logo_noba
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Barra superior blanca y limpia. Muestra el botón de menú o el de volver. */
@Composable
fun BarraSuperior(
    titulo: String,
    onMenu: (() -> Unit)? = null,
    onAtras: (() -> Unit)? = null,
    tituloContenido: (@Composable () -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                if (tituloContenido != null) tituloContenido()
                else Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                when {
                    onMenu != null -> IconButton(onClick = onMenu) { Icon(Ic.Menu, "Menú") }
                    onAtras != null -> IconButton(onClick = onAtras) { Icon(Ic.Atras, "Volver") }
                }
            },
            actions = acciones,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Logo de Noba. Se tiñe con el color del texto para verse bien en modo claro y oscuro. */
@Composable
fun LogoNoba(alto: Dp, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onBackground) {
    Image(
        painter = painterResource(Res.drawable.logo_noba),
        contentDescription = "Noba",
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.height(alto)
    )
}

/** Barra fija inferior para los botones principales de cada paso. */
@Composable
fun BarraInferior(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) { content() }
        }
    }
}

/** Tarjeta minimalista: fondo liso y borde fino, sin sombra. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    resaltada: Boolean = false,
    colorBorde: Color? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val borde = BorderStroke(
        if (resaltada) 1.5.dp else 1.dp,
        colorBorde ?: if (resaltada) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant
    )
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val sombra = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colores, elevation = sombra, border = borde, content = contenido)
    } else {
        Card(modifier = modifier, colors = colores, elevation = sombra, border = borde, content = contenido)
    }
}

/** Icono dentro de un círculo gris claro. */
@Composable
fun IconoCirculo(
    icono: ImageVector,
    tam: Dp = 44.dp,
    fondo: Color = MaterialTheme.colorScheme.surfaceVariant,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(shape = CircleShape, color = fondo, modifier = Modifier.size(tam)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = color, modifier = Modifier.size(tam * 0.5f))
        }
    }
}

/** Avatar con las iniciales del cliente. */
@Composable
fun Avatar(nombre: String, tam: Dp = 44.dp) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(tam)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                nombre.iniciales(),
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Título de sección en mayúsculas pequeñas. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Indicador "Paso X de 2" del asistente de presupuestos. */
@Composable
fun IndicadorPaso(paso: Int, total: Int = 2) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i < paso) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
        Text(
            "Paso $paso de $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CampoBusqueda(valor: String, onCambio: (String) -> Unit, pista: String = "Buscar…") {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text(pista) },
        leadingIcon = { Icon(Ic.Buscar, null) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconoCirculo(icono, 56.dp)
        Spacer(Modifier.height(14.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Etiqueta pequeña redondeada, p. ej. "Facturado". */
@Composable
fun Etiqueta(
    texto: String,
    fondo: Color = MaterialTheme.colorScheme.secondaryContainer,
    color: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(shape = RoundedCornerShape(50), color = fondo) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Fila de un presupuesto o factura en las listas. */
@Composable
fun TarjetaDocumento(d: Documento, facturado: Boolean, onClick: () -> Unit) {
    Tarjeta(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCirculo(if (d.esFactura) Ic.Factura else Ic.Presupuesto, 40.dp)
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
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(d.total.eur(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (facturado) {
                    Spacer(Modifier.height(4.dp))
                    Etiqueta("Facturado")
                }
            }
        }
    }
}

@Composable
fun ConfirmarDialogo(
    titulo: String,
    texto: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(confirmar, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/** Aviso con un solo botón (p. ej. un error al crear el PDF). */
@Composable
fun AvisoDialogo(titulo: String, texto: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Aceptar") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
