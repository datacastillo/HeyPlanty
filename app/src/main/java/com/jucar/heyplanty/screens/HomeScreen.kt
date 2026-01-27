package com.jucar.heyplanty.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.components.AddPlantDialog
import com.jucar.heyplanty.PlantaViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit
import java.io.File

// Icono personalizado de gota de agua
val IconoGotaPersonalizado: ImageVector
    get() = ImageVector.Builder(
        name = "Gota",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HeyPlanty 🌿", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Planta")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (misPlantas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes plantas aún. 🌱", color = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = misPlantas,
                    key = { it.id + it.fechaUltimoRiego }
                ) { planta ->
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
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFE57373), MaterialTheme.shapes.large),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        }
                    ) {
                        PlantItem(
                            planta = planta,
                            onRegarClick = { viewModel.regarPlanta(planta) }
                        )
                    }
                }
            }

            if (showDialog) {
                AddPlantDialog(
                    onDismiss = { showDialog = false },
                    onPlantAdded = { n, h, u ->
                        viewModel.agregarPlanta(n, h, u)
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlantItem(planta: Planta, onRegarClick: () -> Unit) {
    val tieneSed = remember(planta.fechaUltimoRiego) { planta.tieneSed() }

    // Lógica de tiempo actualizada para horas y minutos
    val mensajeTiempo = remember(planta.fechaUltimoRiego) {
        val proximo = planta.fechaUltimoRiego + (planta.diasEntreRiegos * 60 * 60 * 1000L)
        val diff = proximo - System.currentTimeMillis()

        if (diff < 0) {
            val h = TimeUnit.MILLISECONDS.toHours(Math.abs(diff))
            if (h == 0L) "¡Toca regar ahora!" else "Retraso de $h h"
        } else {
            val h = TimeUnit.MILLISECONDS.toHours(diff)
            val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            if (h > 0) "En $h h $m min" else "En $m min"
        }
    }

    val colorDestino = if (tieneSed) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
    val colorContenido = if (tieneSed) Color(0xFFB71C1C) else Color(0xFF2E7D32)
    val colorBoton = if (tieneSed) Color(0xFFD32F2F) else Color(0xFF4CAF50)

    val colorAnimado by animateColorAsState(
        targetValue = colorDestino,
        animationSpec = tween(500),
        label = "ColorCardAnimation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorAnimado),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // IMAGEN CON SOPORTE PARA RUTA INTERNA
            AsyncImage(
                model = if (planta.imagenUri != null) File(planta.imagenUri) else "https://via.placeholder.com/150",
                contentDescription = "Foto de ${planta.nombre}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (tieneSed) "${planta.nombre} 😫" else "${planta.nombre} 😊",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorContenido
                )
                Text(
                    text = mensajeTiempo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorContenido.copy(alpha = 0.8f)
                )
            }

            FilledIconButton(
                onClick = onRegarClick,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorBoton),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = IconoGotaPersonalizado,
                    contentDescription = "Botón Regar",
                    tint = Color.White
                )
            }
        }
    }
}