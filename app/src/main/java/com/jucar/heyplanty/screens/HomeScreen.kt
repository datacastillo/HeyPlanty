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

    // --- DISEÑO DE FONDO (MESH GRADIENT MEJORADO) ---
    val meshGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFEBF5EB), Color(0xFFFFFFFF), Color(0xFFF4F9F4))
    )

    Box(modifier = Modifier.fillMaxSize().background(meshGradient)) {
        // Orbes decorativos con desenfoque profundo
        Canvas(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-150).dp)
                .blur(120.dp)
        ) {
            drawCircle(color = Color(0xFF81C784).copy(alpha = 0.25f))
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
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1B5E20),
                                letterSpacing = (-1.5).sp
                            )
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* Perfil */ }) {
                            Surface(shape = CircleShape, color = Color.White.copy(0.6f), modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.AccountCircle, "Perfil", tint = Color(0xFF4CAF50), modifier = Modifier.padding(4.dp))
                            }
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
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    elevation = FloatingActionButtonDefaults.elevation(12.dp)
                ) {
                    Icon(Icons.Default.Add, "Añadir", modifier = Modifier.size(32.dp))
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                // --- SECCIÓN DE BIENVENIDA ---
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B301B),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = saludoTexto,
                            fontSize = 18.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // --- TARJETA DE STATUS "WOW" ---
                        val totalPlantas = misPlantas.size
                        val necesitanAgua = misPlantas.count {
                            (it.fechaUltimoRiego + (it.diasEntreRiegos * 60 * 1000L)) - tiempoActual <= 0
                        }
                        val statusColor = if (necesitanAgua > 0) Color(0xFFFF5252) else Color(0xFF4CAF50)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = Color.White,
                            shadowElevation = 15.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8F5E9))
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(
                                            Brush.sweepGradient(listOf(Color(0xFF4CAF50), Color(0xFF1B5E20), Color(0xFF4CAF50))),
                                            CircleShape
                                        )
                                        .padding(3.dp)
                                ) {
                                    Surface(shape = CircleShape, color = Color(0xFF1B5E20), modifier = Modifier.fillMaxSize()) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$totalPlantas", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Text(
                                        text = "ESTADO DEL JARDÍN",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray,
                                            letterSpacing = 1.5.sp
                                        )
                                    )

                                    val infiniteTransition = rememberInfiniteTransition(label = "")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.4f, targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = ""
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(statusColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Box(modifier = Modifier.size(10.dp).background(statusColor.copy(alpha = alpha), CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when {
                                                totalPlantas == 0 -> "Comienza tu jardín"
                                                necesitanAgua > 0 -> "Tienes $necesitanAgua sedientas"
                                                else -> "Todo perfecto ✅"
                                            },
                                            fontSize = 16.sp,
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
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(0.7f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF4CAF50)) }
                    )
                }

                // --- LISTADO DE PLANTAS ---
                val filtradas = misPlantas.filter { it.nombre.contains(searchText, ignoreCase = true) }

                if (misPlantas.isEmpty() && !isSearching) {
                    EmptyJardinIllustration()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
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
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(0xFFFFEBEE), RoundedCornerShape(32.dp)).padding(end = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFD32F2F), modifier = Modifier.size(28.dp))
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
    val esUrgente = restanteMilis <= 0

    val cardColor = when {
        esUrgente -> Color(0xFFFFF5F5)
        restanteMilis < 3600000L -> Color(0xFFFFFBF0)
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(32.dp),
        color = cardColor,
        shadowElevation = if (esUrgente) 8.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F8E9))
    ) {
        Column(modifier = Modifier.animateContentSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(85.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE8F5E9))) {
                    if (!planta.imagenUri.isNullOrEmpty()) {
                        AsyncImage(model = File(planta.imagenUri!!), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Spa, null, modifier = Modifier.align(Alignment.Center).size(35.dp), tint = Color(0xFF4CAF50))
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(planta.nombre, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF1B301B))
                    Text(planta.especie, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progreso.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (esUrgente) Color(0xFFFF5252) else Color(0xFF4CAF50),
                        trackColor = Color(0xFFE8F5E9)
                    )

                    val totalS = (restanteMilis / 1000).coerceAtLeast(0)
                    val h = totalS / 3600; val m = (totalS % 3600) / 60; val s = totalS % 60
                    Text(
                        text = if (esUrgente) "¡Necesita agua! 💧" else "Próximo riego: ${h}h ${m}m ${s}s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esUrgente) Color(0xFFD32F2F) else Color(0xFF4CAF50),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                IconButton(
                    onClick = onRegar,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.WaterDrop, "Regar", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFE8F5E9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "BITÁCORA DE CUIDADOS 📝",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF2E7D32)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (historial.isEmpty()) {
                    Text("No hay registros de riego aún", fontSize = 13.sp, color = Color.LightGray)
                } else {
                    historial.take(3).forEach { evento ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            val colorIcono = if(evento.esSobrerego) Color(0xFF1976D2) else Color(0xFF4CAF50)
                            Icon(
                                imageVector = if(evento.esSobrerego) Icons.Default.Waves else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colorIcono,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if(evento.esSobrerego) "Hidratación extra" else "Riego exitoso",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B301B)
                                )
                                Text(
                                    text = SimpleDateFormat("EEEE, dd MMM • HH:mm", Locale.getDefault()).format(Date(evento.fecha)),
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

@Composable
fun EmptyJardinIllustration() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = Color(0xFFF1F8E9), modifier = Modifier.size(120.dp)) {
                Icon(Icons.Default.Spa, null, modifier = Modifier.padding(30.dp), tint = Color(0xFFC8E6C9))
            }
            Spacer(Modifier.height(20.dp))
            Text("Tu jardín está en silencio", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20), fontSize = 18.sp)
            Text("¡Añade una planta para comenzar!", color = Color.Gray, fontSize = 14.sp)
        }
    }
}