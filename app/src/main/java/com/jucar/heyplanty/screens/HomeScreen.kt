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
    var nombreUsuario by remember { mutableStateOf("Cultivador") }

    val misPlantas by viewModel.todasLasPlantas.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    // --- LÓGICA DE TIEMPO REAL ---
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    // --- GESTIÓN DE SNACKBAR PREMIUM ---
    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { mensaje ->
            val mensajeLimpio = if (mensaje.contains("exceso", ignoreCase = true)) {
                "¡Tu planta está perfectamente hidratada! 🌿"
            } else {
                mensaje
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(mensajeLimpio, duration = SnackbarDuration.Short)
        }
    }

    // --- DISEÑO DE FONDO (MESH GRADIENT) ---
    val meshGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0F7F0), Color(0xFFFFFFFF), Color(0xFFF9FBF9))
    )

    Box(modifier = Modifier.fillMaxSize().background(meshGradient)) {
        // Círculos difusos decorativos para estética orgánica
        Canvas(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopEnd)
                .offset(x = 180.dp, y = (-120).dp)
                .blur(100.dp)
        ) {
            drawCircle(color = Color(0xFFC8E6C9).copy(alpha = 0.5f))
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Text(
                            text = "HeyPlanty",
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            color = Color(0xFF1B5E20),
                            letterSpacing = (-1).sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* Perfil */ }) {
                            Icon(Icons.Default.AccountCircle, "Perfil", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if(!isSearching) searchText = ""
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color(0xFF1B5E20)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Add, "Añadir", modifier = Modifier.size(32.dp))
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                // --- SECCIÓN DE BIENVENIDA Y STATUS ---
                if (!isSearching) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .animateContentSize()
                    ) {
                        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val saludoTexto = when(horaActual) {
                            in 5..11 -> "¡Buenos días! ☀️"
                            in 12..18 -> "¡Buenas tardes! 🌤️"
                            else -> "¡Buenas noches! 🌙"
                        }

                        Text(
                            text = if (misPlantas.isEmpty()) "¡Bienvenido! 👋" else "Jardín de $nombreUsuario",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B301B),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = saludoTexto,
                            fontSize = 17.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // --- LA TARJETA "PRO" DE STATUS ---
                        val totalPlantas = misPlantas.size
                        val necesitanAgua = misPlantas.count {
                            (it.fechaUltimoRiego + (it.diasEntreRiegos * 60 * 1000L)) - tiempoActual <= 0
                        }
                        val statusColor = if (necesitanAgua > 0) Color(0xFFFF5252) else Color(0xFF4CAF50)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badge Circular con Gradiente
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))),
                                            CircleShape
                                        )
                                ) {
                                    Text("$totalPlantas", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Text(
                                        text = if(totalPlantas == 1) "PLANTA EN COLECCIÓN" else "PLANTAS TOTALES",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        letterSpacing = 1.sp
                                    )

                                    // Píldora de estado con animación de respiración
                                    val infiniteTransition = rememberInfiniteTransition(label = "")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.4f, targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = ""
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(statusColor.copy(alpha = 0.1f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).background(statusColor.copy(alpha = alpha), CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when {
                                                totalPlantas == 0 -> "Comienza tu jardín"
                                                necesitanAgua > 0 -> "Tienes $necesitanAgua sedientas"
                                                else -> "Todo perfecto ✅"
                                            },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- BUSCADOR ---
                AnimatedVisibility(
                    visible = isSearching,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = { Text("¿Qué planta buscas?") },
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF4CAF50)) }
                    )
                }

                // --- LISTADO DE PLANTAS ---
                val filtradas = misPlantas.filter { it.nombre.contains(searchText, ignoreCase = true) }

                if (misPlantas.isEmpty() && !isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Spa, null, modifier = Modifier.size(80.dp), tint = Color(0xFFE8F5E9))
                            Spacer(Modifier.height(16.dp))
                            Text("Tu jardín está vacío", fontWeight = FontWeight.Bold, color = Color.LightGray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color(0xFFFFEBEE) else Color.Transparent
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(32.dp)).padding(end = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, "Eliminar", tint = Color.Red)
                                    }
                                }
                            ) {
                                PlantItem(planta, tiempoActual, { viewModel.regarPlanta(planta) }, viewModel)
                            }
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

    // Color dinámico según la urgencia
    val cardColor = when {
        restanteMilis <= 0 -> Color(0xFFFFEBEE)
        restanteMilis < 3600000L -> Color(0xFFFFF8E1)
        else -> Color.White
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(32.dp),
        color = cardColor,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F8E9))
    ) {
        Column(modifier = Modifier.animateContentSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Imagen con contenedor moderno
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFE8F5E9))
                ) {
                    if (!planta.imagenUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = File(planta.imagenUri!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.LocalFlorist, null, modifier = Modifier.align(Alignment.Center), tint = Color(0xFF4CAF50))
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(planta.nombre, fontWeight = FontWeight.Black, fontSize = 21.sp, color = Color(0xFF1B301B))
                    Text(planta.especie, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barra de Progreso estilizada
                    LinearProgressIndicator(
                        progress = { progreso.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (restanteMilis <= 0) Color(0xFFFF5252) else Color(0xFF4CAF50),
                        trackColor = Color(0xFFF1F8E9)
                    )

                    val totalS = (restanteMilis / 1000).coerceAtLeast(0)
                    val h = totalS / 3600; val m = (totalS % 3600) / 60; val s = totalS % 60
                    Text(
                        text = if (restanteMilis <= 0) "¡Sedienta ahora! 💧" else "Riego en: ${h}h ${m}m ${s}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (restanteMilis <= 0) Color.Red else Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Botón de Riego Circular
                IconButton(
                    onClick = onRegar,
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF43A047))),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.WaterDrop, "Regar", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // --- DETALLE EXPANDIDO: HISTORIAL ---
            if (expanded) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFE8F5E9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "ÚLTIMOS CUIDADOS 📝",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF2E7D32)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (historial.isEmpty()) {
                    Text("No hay registros aún", fontSize = 12.sp, color = Color.LightGray)
                } else {
                    historial.take(3).forEach { evento ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colorIcono = if(evento.esSobrerego) Color(0xFF1976D2) else Color(0xFF4CAF50)
                            Icon(
                                imageVector = if(evento.esSobrerego) Icons.Default.Waves else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colorIcono,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if(evento.esSobrerego) "Hidratación extra" else "Riego completado",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B301B)
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMMM, HH:mm", Locale.getDefault()).format(Date(evento.fecha)),
                                    fontSize = 12.sp, color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}