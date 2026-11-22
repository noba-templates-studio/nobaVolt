package com.noba.volt

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Todos los iconos de la app en un solo sitio (estilo "outlined", minimalista).
 * Si algún día quieres cambiar un icono, solo hay que tocarlo aquí.
 */
object Ic {
    val Menu = Icons.Filled.Menu
    val Atras = Icons.AutoMirrored.Filled.ArrowBack
    val Siguiente = Icons.AutoMirrored.Filled.ArrowForward
    val Flecha = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val Mas = Icons.Filled.Add
    val Menos = Icons.Outlined.Remove
    val Check = Icons.Filled.Check
    val Cerrar = Icons.Filled.Close
    val Ok = Icons.Outlined.CheckCircle

    val Inicio = Icons.Outlined.Home
    val Presupuesto = Icons.Outlined.Description
    val Factura = Icons.Outlined.Receipt
    val Inventario = Icons.Outlined.Inventory2
    val Ajustes = Icons.Outlined.Settings

    val Buscar = Icons.Outlined.Search
    val Borrar = Icons.Outlined.Delete
    val Editar = Icons.Outlined.Edit
    val Compartir = Icons.Outlined.Share
    val Aviso = Icons.Outlined.WarningAmber
    val Info = Icons.Outlined.Info
    val Exportar = Icons.Outlined.Backup
    val Importar = Icons.Outlined.Restore
}

/** Icono de cada categoría del inventario. */
val Categoria.icono: ImageVector
    get() = when (this) {
        Categoria.CABLEADO -> Icons.Outlined.Cable
        Categoria.CANALIZACION -> Icons.Outlined.LinearScale
        Categoria.MECANISMOS -> Icons.Outlined.ToggleOn
        Categoria.ILUMINACION -> Icons.Outlined.Lightbulb
        Categoria.PROTECCIONES -> Icons.Outlined.Shield
        Categoria.OTROS -> Icons.Outlined.Category
        Categoria.MANO_DE_OBRA -> Icons.Outlined.Engineering
        Categoria.DESPLAZAMIENTO -> Icons.Outlined.LocalShipping
        Categoria.TRAMITES -> Icons.Outlined.Verified
    }
