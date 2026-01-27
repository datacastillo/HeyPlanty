package com.jucar.heyplanty.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
                items(misPlantas) { planta ->
                    PlantItem(
                        planta = planta,
                        // AQUÍ ESTABA EL ERROR: Ahora planta.id (String) coincide con regarPlanta(String)
                        onRegarClick = { viewModel.regarPlanta(planta.id) }
                    )
                }
            }

            if (showDialog) {
                AddPlantDialog(
                    onDismiss = { showDialog = false },
                    onPlantAdded = { nombre, dias ->
                        viewModel.agregarPlanta(nombre, dias)
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlantItem(planta: Planta, onRegarClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = planta.nombre, style = MaterialTheme.typography.titleLarge)
                Text(text = "Riego: cada ${planta.diasEntreRiegos} días")
            }
            IconButton(onClick = onRegarClick) {
                Icon(imageVector = IconoGotaAgua, contentDescription = null, tint = Color.Unspecified)
            }
        }
    }
}