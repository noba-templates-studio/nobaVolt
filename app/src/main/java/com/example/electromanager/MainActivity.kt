@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.electromanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.electromanager.ui.theme.ElectroManagerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // La ventana de arranque es negra (Theme.ElectroManager.Arranque) para enlazar con la intro;
        // a partir de aquí se usa el tema normal de la app.
        setTheme(R.style.Theme_ElectroManager)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Solo al abrir la app en frío: no al girar la pantalla ni al volver de segundo plano
        val conIntro = savedInstanceState == null && animacionesActivas(this)
        setContent {
            val vm: AppViewModel = viewModel()
            val oscuro = when (vm.ajustes.tema) {
                "CLARO" -> false
                "OSCURO" -> true
                else -> isSystemInDarkTheme()
            }
            var intro by rememberSaveable { mutableStateOf(conIntro) }
            ElectroManagerTheme(darkTheme = oscuro) {
                Box(Modifier.fillMaxSize()) {
                    App(vm) // se carga debajo mientras dura la intro
                    if (intro) IntroNoba(sonido = vm.ajustes.sonidoInicio) { intro = false }
                }
            }
        }
    }
}

@Composable
fun App(vm: AppViewModel) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pantalla = vm.pantalla
    val esPrincipal = pantalla == Pantalla.Inicio ||
        pantalla == Pantalla.Presupuestos || pantalla == Pantalla.Facturas ||
        pantalla == Pantalla.Inventario || pantalla == Pantalla.Ajustes
    val abrirMenu: () -> Unit = { scope.launch { drawer.open() } }

    // Botón atrás del móvil: cierra el menú o vuelve a la pantalla anterior
    BackHandler(enabled = !drawer.isOpen && pantalla != Pantalla.Inicio) { vm.atras() }
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = esPrincipal || drawer.isOpen,
        drawerContent = {
            MenuLateral(vm, pantalla) { destino ->
                if (destino == Pantalla.Inventario) vm.verSoloStockBajo = false
                vm.ir(destino)
                scope.launch { drawer.close() }
            }
        }
    ) {
        Crossfade(targetState = pantalla, label = "pantalla") { p ->
            when (p) {
                Pantalla.Inicio -> InicioScreen(vm, abrirMenu)
                Pantalla.Presupuestos -> HistorialScreen(vm, PRESUPUESTO, abrirMenu)
                Pantalla.Facturas -> HistorialScreen(vm, FACTURA, abrirMenu)
                Pantalla.Inventario -> InventarioScreen(vm, abrirMenu)
                Pantalla.Ajustes -> AjustesScreen(vm, abrirMenu)
                Pantalla.NuevoDatos -> DatosClienteScreen(vm)
                Pantalla.NuevoMateriales -> SeleccionScreen(vm)
                is Pantalla.Detalle -> DetalleScreen(vm, p)
            }
        }
    }

    // Aviso automático cuando un material baja del mínimo de stock
    if (vm.avisoStock.isNotEmpty()) {
        AvisoStockDialogo(vm)
    }
}

@Composable
private fun AvisoStockDialogo(vm: AppViewModel) {
    val umbral = vm.ajustes.umbralStock
    AlertDialog(
        onDismissRequest = { vm.avisoStock = emptyList() },
        icon = { Icon(Ic.Aviso, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Stock bajo") },
        text = {
            Column {
                Text("Estos materiales tienen menos de ${umbral.fmt()} unidades:")
                Spacer(Modifier.height(8.dp))
                vm.avisoStock.forEach { a ->
                    Text("• ${a.nombre}: quedan ${(a.stock ?: 0.0).fmt()} ${a.unidad}")
                }
            }
        },
        confirmButton = { Button(onClick = { vm.abrirInventarioStockBajo() }) { Text("Ver inventario") } },
        dismissButton = { TextButton(onClick = { vm.avisoStock = emptyList() }) { Text("Cerrar") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/** Barra lateral deslizante. */
@Composable
fun MenuLateral(vm: AppViewModel, actual: Pantalla, onIr: (Pantalla) -> Unit) {
    val nPresupuestos = vm.documentos.count { !it.esFactura }
    val nFacturas = vm.documentos.count { it.esFactura }
    val nStockBajo = vm.articulosStockBajo().size

    ModalDrawerSheet(
        windowInsets = WindowInsets(0, 0, 0, 0),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 20.dp)
        ) {
            LogoNoba(alto = 26.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Noba Volt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        ItemMenu(Ic.Inicio, "Inicio", null, actual == Pantalla.Inicio) { onIr(Pantalla.Inicio) }

        TituloSeccion("Gestión", Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp))
        ItemMenu(Ic.Factura, "Historial de facturas", "$nFacturas", actual == Pantalla.Facturas) {
            onIr(Pantalla.Facturas)
        }
        ItemMenu(Ic.Presupuesto, "Historial de presupuestos", "$nPresupuestos", actual == Pantalla.Presupuestos) {
            onIr(Pantalla.Presupuestos)
        }
        ItemMenu(
            Ic.Inventario, "Inventario",
            if (nStockBajo > 0) "$nStockBajo bajo" else "${vm.inventario.size}",
            actual == Pantalla.Inventario,
            aviso = nStockBajo > 0
        ) { onIr(Pantalla.Inventario) }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        ItemMenu(Ic.Ajustes, "Ajustes", null, actual == Pantalla.Ajustes) { onIr(Pantalla.Ajustes) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ItemMenu(
    icono: ImageVector,
    texto: String,
    contador: String?,
    seleccionado: Boolean,
    aviso: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(texto) },
        icon = { Icon(icono, null) },
        badge = {
            if (contador != null) {
                if (aviso) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Aviso, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(contador, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Text(contador, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        selected = seleccionado,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedIconColor = MaterialTheme.colorScheme.onSurface,
            selectedBadgeColor = MaterialTheme.colorScheme.onSurface,
            unselectedContainerColor = MaterialTheme.colorScheme.background,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedBadgeColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
