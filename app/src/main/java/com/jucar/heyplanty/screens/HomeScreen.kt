package com.jucar.heyplanty.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jucar.heyplanty.PlantaViewModel
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.domain.RiegoEvento
import com.jucar.heyplanty.components.AddPlantDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- PALETA DE COLORES PREMIUM ---
val PlantDark = Color(0xFF1B5E20)
val PlantPrimary = Color(0xFF4CAF50)
val PlantLight = Color(0xFFE8F5E9)
val PlantAccent = Color(0xFFC8E6C9)
val DangerRed = Color(0xFFD32F2F)
val WarningOrange = Color(0xFFFF9800)
val DeepGray = Color(0xFF455A64)
val GlassWhite = Color(0xCCFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PlantaViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var plantaAEditar by remember { mutableStateOf<Planta?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // --- ESTADO PARA EL DIÁLOGO DE EXCESO (SOLUCIÓN AL ERROR) ---
    var showExcesoDialog by remember { mutableStateOf(false) }
    var ultimoResultadoRiego by remember { mutableStateOf<Any?>(null) }

    val misPlantas by viewModel.todasLasPlantas.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()

    // --- RELOJ DE ALTA PRECISIÓN ---
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    // --- COLECTOR DE EVENTOS DE RIEGO ---
    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { resultado ->
            // Guardamos el objeto para el diálogo profesional
            ultimoResultadoRiego = resultado
            showExcesoDialog = true

            // También lo mostramos en snackbar por si acaso
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "Actualización de riego detectada",
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFBFDFA))) {
        PremiumBackgroundEffect()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        actionColor = PlantAccent,
                        containerColor = PlantDark,
                        snackbarData = data,
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            },
            topBar = {
                EliteTopAppBar(
                    isSearching = isSearching,
                    onToggleSearch = { isSearching = !isSearching }
                )
            },
            floatingActionButton = {
                EnhancedFAB(onClick = { showDialog = true })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                HeaderBrandingSection(
                    misPlantas = misPlantas,
                    tiempoActual = tiempoActual,
                    isSearching = isSearching
                )

                SearchSection(
                    isVisible = isSearching,
                    searchText = searchText,
                    onValueChange = { searchText = it }
                )

                val plantasFiltradas = misPlantas.filter {
                    it.nombre.contains(searchText, ignoreCase = true)
                }

                if (misPlantas.isEmpty() && !isSearching) {
                    EmptyGardenIllustration()
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        items(plantasFiltradas, key = { it.id }) { planta ->
                            PlantCardWrapper(onDelete = { viewModel.eliminarPlanta(planta) }) {
                                PlantItemElite(
                                    planta = planta,
                                    tiempoActual = tiempoActual,
                                    onRegar = { viewModel.regarPlanta(planta) },
                                    onEdit = { plantaAEditar = planta },
                                    viewModel = viewModel
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }

        // --- DIÁLOGO DE EXCESO / RESULTADO (SOLUCIÓN DEFINITIVA) ---
        if (showExcesoDialog && ultimoResultadoRiego != null) {
            ExcesoRiegoDialog(
                resultado = ultimoResultadoRiego!!,
                onDismiss = { showExcesoDialog = false }
            )
        }

        if (showDialog) {
            AddPlantDialog(
                onDismiss = { showDialog = false },
                onPlantAdded = { n, e, t, s, uri, color ->
                    viewModel.agregarPlanta(n, e, t, uri, "#%08X".format(color.toArgb()))
                    showDialog = false
                }
            )
        }

        if (plantaAEditar != null) {
            AddPlantDialog(
                plantaAEditar = plantaAEditar,
                onDismiss = { plantaAEditar = null },
                onPlantAdded = { n, e, t, s, uri, color ->
                    viewModel.editarPlanta(plantaAEditar!!, n, e, t, uri)
                    plantaAEditar = null
                }
            )
        }
    }
}

// --- COMPONENTE DE DIÁLOGO PROFESIONAL CORREGIDO ---
@Composable
fun ExcesoRiegoDialog(resultado: Any, onDismiss: () -> Unit) {
    // LÓGICA DE EXTRACCIÓN MEJORADA: Evita mostrar el texto técnico RiegoResult(...)
    val mensajeLimpio = remember(resultado) {
        val raw = resultado.toString()
        // Buscamos si el string contiene el patrón de la data class y extraemos el valor de 'mensaje'
        if (raw.contains("mensaje=")) {
            raw.substringAfter("mensaje=").substringBefore(",").trim()
        } else {
            raw
        }
    }

    // Verificamos si es exceso para cambiar el color del icono
    val esExceso = resultado.toString().contains("esExceso=true")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
                .shadow(30.dp, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, PlantLight.copy(alpha = 0.5f))
                        )
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono dinámico con fondo suave
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (esExceso) WarningOrange.copy(0.1f) else PlantPrimary.copy(0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (esExceso) Icons.Rounded.Waves else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (esExceso) WarningOrange else PlantPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Aviso de Cuidado",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esExceso) WarningOrange else PlantPrimary,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = mensajeLimpio,
                    style = TextStyle(
                        color = PlantDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esExceso) WarningOrange else PlantPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text(
                        "ENTENDIDO",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumBackgroundEffect() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PlantAccent.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                radius = 400.dp.toPx()
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE1F5FE).copy(alpha = 0.5f), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.9f),
                radius = 600.dp.toPx()
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteTopAppBar(isSearching: Boolean, onToggleSearch: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        title = {
            Text(
                "HEYPLANTY",
                style = TextStyle(
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 5.sp,
                    brush = Brush.horizontalGradient(listOf(PlantDark, PlantPrimary))
                )
            )
        },
        actions = {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(42.dp)
                    .background(Color.White.copy(0.7f), CircleShape)
                    .border(0.5.dp, PlantAccent, CircleShape)
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = null,
                    tint = PlantDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
fun HeaderBrandingSection(
    misPlantas: List<Planta>,
    tiempoActual: Long,
    isSearching: Boolean
) {
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = !isSearching }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn(animationSpec = tween(600)) + slideInHorizontally(animationSpec = tween(600)),
        exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally()
    ) {
        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val (saludoTop, saludoBottom, subtexto) = when(horaActual) {
            in 5..11 -> Triple("Despierta,", "Buen día", "Tus brotes buscan el sol")
            in 12..18 -> Triple("Luz vital,", "Buenas tardes", "Momento de frescura en el jardín")
            in 19..23 -> Triple("Calma,", "Buenas noches", "Tus raíces descansan hoy")
            else -> Triple("Silencio,", "Hola noctámbulo", "El jardín sueña bajo la luna")
        }

        val necesitanAgua = misPlantas.count {
            (it.fechaUltimoRiego + (it.diasEntreRiegos * 60000L)) - tiempoActual <= 0
        }

        Column(modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = saludoTop, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PlantPrimary, letterSpacing = 1.sp))
                    Text(text = saludoBottom, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B301B), letterSpacing = (-1.5).sp, lineHeight = 36.sp))
                    Text(text = subtexto, style = TextStyle(fontSize = 13.sp, color = DeepGray.copy(alpha = 0.7f), fontWeight = FontWeight.Medium))
                }
                StatusGlassCard(necesitanAgua)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth(0.15f).height(4.dp).clip(CircleShape).background(PlantPrimary.copy(0.3f)))
        }
    }
}

@Composable
fun StatusGlassCard(necesitanAgua: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    Surface(
        color = if (necesitanAgua > 0) DangerRed.copy(0.08f) else PlantPrimary.copy(0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, if (necesitanAgua > 0) DangerRed.copy(0.15f) else PlantPrimary.copy(0.15f)),
        modifier = Modifier.shadow(if (necesitanAgua > 0) 8.dp else 0.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer(alpha = if (necesitanAgua > 0) alphaAnim else 1f)
                    .background(if (necesitanAgua > 0) DangerRed else PlantPrimary, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text = if (necesitanAgua > 0) "$necesitanAgua con sed" else "Todo OK", style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (necesitanAgua > 0) DangerRed else PlantDark))
                Text(text = if (necesitanAgua > 0) "¡A regar!" else "Jardín feliz", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = DeepGray))
            }
        }
    }
}

@Composable
fun SearchSection(isVisible: Boolean, searchText: String, onValueChange: (String) -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        TextField(
            value = searchText,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).shadow(12.dp, RoundedCornerShape(24.dp)),
            placeholder = { Text("Busca una planta...", color = Color.Gray) },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = PlantPrimary) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantCardWrapper(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(DangerRed.copy(0.12f), RoundedCornerShape(32.dp)).padding(end = 28.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.DeleteForever, null, tint = DangerRed, modifier = Modifier.size(30.dp))
            }
        }
    ) { content() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlantItemElite(
    planta: Planta,
    tiempoActual: Long,
    onRegar: () -> Unit,
    onEdit: () -> Unit,
    viewModel: PlantaViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val progreso = planta.obtenerProgresoRiego()
    val restanteMilis = (planta.fechaUltimoRiego + (planta.diasEntreRiegos * 60000L)) - tiempoActual
    val esUrgente = restanteMilis <= 0 && planta.fechaUltimoRiego > 0L
    val colorSalud by animateColorAsState(targetValue = when { progreso > 0.65f -> PlantPrimary; progreso > 0.3f -> WarningOrange; else -> DangerRed })

    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { expanded = !expanded }, onLongClick = onEdit),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.animateContentSize(tween(400)).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxSize(), color = colorSalud, strokeWidth = 5.dp, trackColor = colorSalud.copy(0.1f), strokeCap = StrokeCap.Round)
                    Surface(modifier = Modifier.size(62.dp), shape = CircleShape, color = PlantLight) {
                        if (!planta.imagenUri.isNullOrEmpty()) {
                            AsyncImage(model = File(planta.imagenUri!!), contentDescription = null, contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Rounded.Eco, null, Modifier.padding(16.dp), tint = colorSalud)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = planta.nombre, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF1B301B)))
                    val sTotal = (restanteMilis / 1000).coerceAtLeast(0)
                    val h = sTotal / 3600
                    val m = (sTotal % 3600) / 60
                    val s = sTotal % 60
                    Text(
                        text = if (esUrgente) "NECESITA AGUA YA" else String.format("%02dh %02dm %02ds", h, m, s),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (esUrgente) DangerRed else Color.Gray, fontFamily = FontFamily.Monospace)
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth(0.85f).height(6.dp).clip(CircleShape), color = colorSalud, trackColor = colorSalud.copy(0.12f))
                }
                FloatingActionButton(onClick = onRegar, containerColor = colorSalud, modifier = Modifier.size(46.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            if (expanded) EliteDetailsSection(planta, viewModel)
        }
    }
}

@Composable
fun EliteDetailsSection(planta: Planta, viewModel: PlantaViewModel) {
    val historial by viewModel.obtenerHistorial(planta.id).collectAsState(initial = emptyList())
    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(0.3f))
    Spacer(modifier = Modifier.height(20.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        DetailMetric("Vitalidad", "${planta.salud}%", Icons.Rounded.Favorite, Color(0xFFE91E63))
        DetailMetric("Drama", "${planta.nivelDrama}/5", Icons.Rounded.AutoAwesome, WarningOrange)
        DetailMetric("Intervalo", "${planta.diasEntreRiegos}m", Icons.Rounded.Sync, Color.Blue)
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text("BITÁCORA DE CUIDADOS", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp, color = Color.Gray))
    Spacer(modifier = Modifier.height(12.dp))
    if (historial.isEmpty()) {
        Text("No hay riegos registrados aún.", fontSize = 12.sp, color = Color.LightGray)
    } else {
        historial.take(3).forEach { evento -> LogEntry(evento) }
    }
}

@Composable
fun DetailMetric(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.background(color.copy(0.1f), CircleShape).padding(8.dp)) { Icon(icon, null, modifier = Modifier.size(16.dp), tint = color) }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PlantDark)
    }
}

@Composable
fun LogEntry(evento: RiegoEvento) {
    Surface(modifier = Modifier.padding(vertical = 5.dp).fillMaxWidth(), color = Color(0xFFF1F4F1), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (evento.esSobrerego) Icons.Rounded.PriorityHigh else Icons.Rounded.Verified, null, modifier = Modifier.size(16.dp), tint = if (evento.esSobrerego) Color.Blue else PlantPrimary)
            Spacer(Modifier.width(12.dp))
            Text(text = SimpleDateFormat("EEEE, dd MMM • HH:mm", Locale.getDefault()).format(Date(evento.fecha)), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeepGray)
        }
    }
}

@Composable
fun EnhancedFAB(onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick, containerColor = PlantDark, contentColor = Color.White, elevation = FloatingActionButtonDefaults.elevation(10.dp), shape = RoundedCornerShape(22.dp)) {
        Icon(Icons.Default.AddCircle, null)
        Spacer(Modifier.width(8.dp))
        Text("NUEVA PLANTA", fontWeight = FontWeight.Black)
    }
}

@Composable
fun EmptyGardenIllustration() {
    Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Spa, null, modifier = Modifier.size(120.dp), tint = PlantAccent)
        Text("Tu oasis está listo", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 24.sp, color = PlantDark))
        Text("Empieza añadiendo tu primera planta.", color = Color.Gray)
    }
}

fun Planta.obtenerProgresoRiego(): Float {
    if (this.fechaUltimoRiego <= 0L) return 1f
    val frecuenciaMilis = this.diasEntreRiegos * 60 * 1000L
    val transcurrido = System.currentTimeMillis() - this.fechaUltimoRiego
    return (1f - (transcurrido.toFloat() / frecuenciaMilis)).coerceIn(0f, 1f)
}