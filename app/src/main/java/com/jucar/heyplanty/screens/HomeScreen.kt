package com.jucar.heyplanty.screens

import androidx.compose.animation.* // Importamos todo para las animaciones
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit
import java.io.File

// --- ICONO GOTA ---
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
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val misPlantas by viewModel.todasLasPlantas.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val plantasFiltradas = misPlantas.filter {
        it.nombre.contains(searchText, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // --- ANIMACIÓN DE LA BARRA SUPERIOR ---
                    AnimatedContent(
                        targetState = isSearching,
                        transitionSpec = {
                            if (targetState) { // Si aparece el buscador
                                (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                    slideOutVertically { height -> height } + fadeOut())
                            } else { // Si vuelve el título
                                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                    slideOutVertically { height -> -height } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "AnimacionBuscador"
                    ) { searching ->
                        if (!searching) {
                            Text("HeyPlanty 🌿", fontWeight = FontWeight.Bold)
                        } else {
                            TextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("Buscar planta...") },
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearching = !isSearching
                        if (!isSearching) searchText = ""
                    }) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Planta")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (plantasFiltradas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchText.isEmpty()) "No tienes plantas aún. 🌱" else "No se encontró nada. 🔍",
                        color = Color.Gray
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = plantasFiltradas,
                    key = { it.id }
                ) { planta ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.eliminarPlanta(planta)
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
                                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 20.dp))
                            }
                        }
                    ) {
                        PlantItem(planta = planta, tiempoActual = tiempoActual, onRegarClick = { viewModel.regarPlanta(planta) })
                    }
                }
            }

            if (showDialog) {
                AddPlantDialog(onDismiss = { showDialog = false }, onPlantAdded = { n, h, u ->
                    viewModel.agregarPlanta(n, h, u)
                    showDialog = false
                })
            }
        }
    }
}

// PlantItem se mantiene igual que antes (ya lo tienes funcionando de maravilla)
@Composable
fun PlantItem(planta: Planta, tiempoActual: Long, onRegarClick: () -> Unit) {
    val tieneSed = planta.tieneSed()
    val progresoRiego = planta.obtenerProgresoRiego()
    val proximoRiego = planta.fechaUltimoRiego + (planta.diasEntreRiegos * 60 * 60 * 1000L)
    val milisRestantes = proximoRiego - tiempoActual
    val minutosRestantes = TimeUnit.MILLISECONDS.toMinutes(milisRestantes)

    val mensajeTiempo = if (milisRestantes < 0) {
        val h = TimeUnit.MILLISECONDS.toHours(Math.abs(milisRestantes))
        if (h == 0L) "¡Toca regar ahora!" else "Retraso de $h h"
    } else {
        val h = TimeUnit.MILLISECONDS.toHours(milisRestantes)
        val m = TimeUnit.MILLISECONDS.toMinutes(milisRestantes) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(milisRestantes) % 60
        if (h > 0) "En $h h $m min" else if (m > 0) "En $m min $s s" else "En $s s"
    }

    val colorAlerta = when {
        tieneSed -> Color(0xFFB71C1C)
        minutosRestantes < 10 -> Color(0xFFF44336)
        minutosRestantes < 30 -> Color(0xFFFF9800)
        minutosRestantes < 45 -> Color(0xFFFFEB3B)
        else -> Color(0xFF4CAF50)
    }

    val colorFondoCard = if (tieneSed || minutosRestantes < 10) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
    val colorTextoPrincipal = if (tieneSed || minutosRestantes < 10) Color(0xFFB71C1C) else Color(0xFF2E7D32)
    val colorAnimado by animateColorAsState(targetValue = colorFondoCard, animationSpec = tween(500), label = "")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorAnimado),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = if (planta.imagenUri != null) File(planta.imagenUri) else "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (tieneSed) "${planta.nombre} 😫" else "${planta.nombre} 😊", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorTextoPrincipal)
                LinearProgressIndicator(
                    progress = { progresoRiego },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(8.dp).clip(CircleShape),
                    color = colorAlerta,
                    trackColor = colorAlerta.copy(alpha = 0.2f)
                )
                Text(text = mensajeTiempo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = colorTextoPrincipal.copy(alpha = 0.7f))
            }
            FilledIconButton(onClick = onRegarClick, colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorAlerta), modifier = Modifier.size(48.dp)) {
                Icon(imageVector = IconoGotaPersonalizado, contentDescription = null, tint = if(minutosRestantes < 45 && minutosRestantes >= 30 && !tieneSed) Color.Black else Color.White)
            }
        }
    }
}
