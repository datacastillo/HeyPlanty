package com.jucar.heyplanty.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jucar.heyplanty.PlantaViewModel
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.components.AddPlantDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PlantaViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val misPlantas by viewModel.todasLasPlantas.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    // SOLUCIÓN PARA LA FRANJA INFERIOR (SNACKBAR)
    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { mensaje ->
            // Interceptamos el mensaje de exceso para que siempre sea positivo
            val mensajeFinal = if (mensaje.contains("exceso", ignoreCase = true)) {
                "¡Riego registrado con éxito! 🌿"
            } else {
                mensaje
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(mensajeFinal, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    val meshGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0F7F0), Color(0xFFFFFFFF), Color(0xFFF9FBF9))
    )

    Box(modifier = Modifier.fillMaxSize().background(meshGradient)) {
        Canvas(modifier = Modifier.size(400.dp).align(Alignment.TopEnd).offset(x = 150.dp, y = (-100).dp).blur(80.dp)) {
            drawCircle(color = Color(0xFFC8E6C9).copy(alpha = 0.5f))
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    title = {
                        if (!isSearching) {
                            Text("HeyPlanty 🌿", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color(0xFF1B5E20))
                        } else {
                            TextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("Busca tu planta...") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.7f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.4f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = !isSearching; if(!isSearching) searchText = "" }) {
                            Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, null, tint = Color(0xFF1B5E20))
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape) {
                    Icon(Icons.Default.Add, "Añadir", modifier = Modifier.size(30.dp))
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                val filtradas = misPlantas.filter { it.nombre.contains(searchText, ignoreCase = true) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(filtradas, key = { it.id }) { planta ->
                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.eliminarPlanta(planta)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(Modifier.fillMaxSize().background(Color.Red.copy(0.6f), RoundedCornerShape(32.dp)).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White)
                                }
                            }
                        ) {
                            PlantItem(planta, tiempoActual, { viewModel.regarPlanta(planta) }, viewModel)
                        }
                    }
                }

                if (showDialog) {
                    AddPlantDialog(
                        onDismiss = { showDialog = false },
                        onPlantAdded = { n, e, tMin, salud, uri, colorObjeto ->
                            val colorHex = "#%08X".format(colorObjeto.toArgb())
                            viewModel.agregarPlanta(n, e, tMin, uri, colorHex)
                            showDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlantItem(planta: Planta, tiempoActual: Long, onRegar: () -> Unit, viewModel: PlantaViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val historial by viewModel.obtenerHistorial(planta.id).collectAsState(initial = emptyList())
    val progreso = planta.obtenerProgresoRiego()
    val frecuenciaMilis = planta.diasEntreRiegos * 60 * 1000L
    val restanteMilis = (planta.fechaUltimoRiego + frecuenciaMilis) - tiempoActual

    // --- CAMBIO DE COLOR DINÁMICO DE LA TARJETA ---
    val cardColor = when {
        restanteMilis <= 0 -> Color(0xFFFFEBEE) // Rojo suave (Vencido)
        restanteMilis < 2 * 3600 * 1000L -> Color(0xFFFFF8E1) // Ámbar suave (Próximo < 2 horas)
        else -> Color(0xFFF1F8E9) // Verde estándar
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(32.dp),
        color = cardColor,
        shadowElevation = 8.dp,
        border = null
    ) {
        Column(Modifier.animateContentSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(85.dp).clip(CircleShape).background(Color.White)) {
                    if (!planta.imagenUri.isNullOrEmpty()) {
                        AsyncImage(model = File(planta.imagenUri!!), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Spa, null, modifier = Modifier.align(Alignment.Center), tint = Color(0xFF4CAF50))
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(planta.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF1B301B))
                    Text(planta.especie, fontSize = 14.sp, color = Color.Gray)

                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progreso.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (restanteMilis < 0) Color.Red else if (restanteMilis < 2 * 3600 * 1000L) Color(0xFFFFA000) else Color(0xFF4CAF50),
                        trackColor = Color(0xFFE8F5E9)
                    )

                    val totalS = (restanteMilis / 1000).coerceAtLeast(0)
                    val d = totalS / 86400
                    val h = (totalS % 86400) / 3600
                    val m = (totalS % 3600) / 60
                    val s = totalS % 60

                    Text(
                        text = if (restanteMilis <= 0) "¡Necesita agua! 💧" else "Riego en: ${if(d>0)"${d}d " else ""}${h}h ${m}m ${s}s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (restanteMilis <= 0) Color.Red else Color.Gray
                    )
                }

                IconButton(onClick = onRegar, modifier = Modifier.size(50.dp).background(Color(0xFF4CAF50), CircleShape)) {
                    Icon(Icons.Default.WaterDrop, null, tint = Color.White)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF4CAF50).copy(0.1f), thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
                Text("Bitácora de cuidados 📝", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                Spacer(Modifier.height(8.dp))

                val listaHistorial = historial.take(5)
                listaHistorial.forEachIndexed { index, evento ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {

                        // Lógica para que el primer riego de la historia siempre sea OK
                        val esElPrimerRiegoHistorico = index == listaHistorial.size - 1

                        val (texto, color, icono) = when {
                            esElPrimerRiegoHistorico -> Triple("¡OK! ✅", Color(0xFF388E3C), Icons.Default.CheckCircle)
                            evento.esSobrerego -> Triple("EXCESO 💧", Color(0xFF1976D2), Icons.Default.Waves)
                            else -> Triple("¡OK! ✅", Color(0xFF388E3C), Icons.Default.CheckCircle)
                        }

                        Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(text = texto, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
                            Text(text = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(evento.fecha)), fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}