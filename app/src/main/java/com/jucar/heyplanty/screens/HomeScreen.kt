package com.jucar.heyplanty.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { mensaje ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(mensaje, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (!isSearching) Text("HeyPlanty 🌿", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    else TextField(
                        value = searchText, onValueChange = { searchText = it },
                        placeholder = { Text("¿Qué buscas?") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching; if(!isSearching) searchText = "" }) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, null)
                    }
                }
            )
        },
        floatingActionButton = {
            // CORRECCIÓN ERROR containerColor: Usamos containerColor dentro de FloatingActionButtonDefaults
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, "Añadir")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val filtradas = misPlantas.filter { it.nombre.contains(searchText, ignoreCase = true) }

            if (misPlantas.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Spa, null, modifier = Modifier.size(100.dp), tint = Color(0xFF4CAF50).copy(0.2f))
                    Text("¡Jardín solitario!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Presiona + para darle vida a tu app.", textAlign = TextAlign.Center, color = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filtradas, key = { it.id }) { planta ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.eliminarPlanta(planta)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red.copy(0.7f) else Color.Transparent
                            Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(20.dp)).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, null, tint = Color.White)
                            }
                        }
                    ) {
                        PlantItem(planta, tiempoActual, { viewModel.regarPlanta(planta) }, viewModel)
                    }
                }
            }

            if (showDialog) {
                AddPlantDialog(onDismiss = { showDialog = false }, onPlantAdded = { n, e, h, m, u, c ->
                    viewModel.agregarPlanta(n, e, h, m, u, c)
                    showDialog = false
                })
            }
        }
    }
}

@Composable
fun PlantItem(planta: Planta, tiempoActual: Long, onRegar: () -> Unit, viewModel: PlantaViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val historial by viewModel.obtenerHistorial(planta.id).collectAsState(initial = emptyList())
    val progreso = planta.obtenerProgresoRiego()
    val restanteMilis = (planta.fechaUltimoRiego + (planta.diasEntreRiegos * 60 * 1000L)) - tiempoActual

    // CORRECCIÓN ANIMACIÓN (Errores de la captura): Sintaxis simplificada
    val infiniteTransition = rememberInfiniteTransition(label = "latido")
    val escala by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (planta.esCritica()) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    val colorBarra = when {
        progreso > 0.6f -> Color(0xFF4CAF50)
        progreso > 0.3f -> Color(0xFFFF9800)
        else -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if(planta.esCritica()) Color(0xFFFFF8F8) else Color.White),
        elevation = CardDefaults.cardElevation(if(planta.esCritica()) 6.dp else 2.dp)
    ) {
        Column(Modifier.animateContentSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = planta.imagenUri?.let { File(it) },
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (planta.estaEnferma()) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                )

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(planta.nombre, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("Salud: ${planta.salud}%", fontWeight = FontWeight.Bold, color = if(planta.estaEnferma()) Color.Red else Color(0xFF4CAF50))

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = colorBarra,
                        trackColor = colorBarra.copy(0.15f)
                    )

                    val h = (restanteMilis / 3600000).coerceAtLeast(0)
                    val m = ((restanteMilis % 3600000) / 60000).coerceAtLeast(0)
                    val s = ((restanteMilis % 60000) / 1000).coerceAtLeast(0)

                    Text(
                        text = if (restanteMilis <= 0) "¡Necesito agua! 💧" else "Riego en: ${h}h ${m}m ${s}s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (restanteMilis <= 0) Color.Red else Color.DarkGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(onClick = onRegar, modifier = Modifier.background(colorBarra, CircleShape).size(50.dp)) {
                    Icon(Icons.Default.WaterDrop, "Regar", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(0.4f))
                Text("Especie: ${planta.especie}", fontWeight = FontWeight.Bold)
                Text("Nota: ${planta.consejo.ifBlank { "Sin notas." }}", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(12.dp))
                Text("Historial:", fontWeight = FontWeight.Bold)
                historial.take(5).forEach { evento ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        // AQUÍ CORREGIMOS EL ERROR 'esSobrerego' y 'fecha'
                        Icon(if(evento.esSobrerego) Icons.Default.Warning else Icons.Default.CheckCircle, null,
                            tint = if(evento.esSobrerego) Color.Red else Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(evento.fecha)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                        Text(if(evento.esSobrerego) "EXCESO" else "BIEN", color = if(evento.esSobrerego) Color.Red else Color(0xFF4CAF50), fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}