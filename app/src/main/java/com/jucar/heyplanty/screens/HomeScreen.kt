package com.jucar.heyplanty.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.components.AddPlantDialog
import com.jucar.heyplanty.PlantaViewModel

val IconoGotaAgua: ImageVector
    get() = ImageVector.Builder(
        name = "Gota",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = SolidColor(Color(0xFF2196F3))) {
        moveTo(12f, 2.15f)
        curveTo(12f, 2.15f, 4f, 10f, 4f, 15.5f)
        curveTo(4f, 19.92f, 7.58f, 23.5f, 12f, 23.5f)
        curveTo(16.42f, 23.5f, 20f, 19.92f, 20f, 15.5f)
        curveTo(20f, 10f, 12f, 2.15f, 12f, 2.15f)
        close()
    }.build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PlantaViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    val misPlantas by viewModel.todasLasPlantas.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("HeyPlanty 🌿") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = misPlantas, key = { it.id }) { planta ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.eliminarPlanta(planta.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                Color.Red.copy(alpha = 0.7f) else Color.Transparent
                            Box(
                                modifier = Modifier.fillMaxSize().padding(8.dp).background(color, MaterialTheme.shapes.medium),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                            }
                        }
                    ) {
                        PlantItem(
                            planta = planta,
                            onRegarClick = { viewModel.regarPlanta(planta.id) }
                        )
                    }
                }
            }

            if (showDialog) {
                AddPlantDialog(onDismiss = { showDialog = false }, onPlantAdded = { n, d -> viewModel.agregarPlanta(n, d); showDialog = false })
            }
        }
    }
}

@Composable
fun PlantItem(planta: Planta, onRegarClick: () -> Unit) {
    // DETERMINAR COLORES SEGÚN LA SED
    val colorFondo = if (planta.tieneSed()) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
    val colorBorde = if (planta.tieneSed()) Color(0xFFFFCDD2) else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        border = if (planta.tieneSed()) androidx.compose.foundation.BorderStroke(1.dp, colorBorde) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (planta.tieneSed()) "${planta.nombre} 😫" else planta.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (planta.tieneSed()) Color(0xFFB71C1C) else Color.Unspecified
                )
                Text(
                    text = if (planta.tieneSed()) "¡NECESITA AGUA!" else "Riego: cada ${planta.diasEntreRiegos} días",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (planta.tieneSed()) Color(0xFFD32F2F) else Color.Unspecified
                )
            }
            IconButton(onClick = onRegarClick) {
                Icon(imageVector = IconoGotaAgua, contentDescription = null, tint = Color.Unspecified)
            }
        }
    }
}