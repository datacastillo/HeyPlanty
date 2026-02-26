
package com.jucar.heyplanty.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.jucar.heyplanty.PlantaViewModel
import com.jucar.heyplanty.R
import com.jucar.heyplanty.RiegoResult
import com.jucar.heyplanty.components.AddPlantDialog
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.domain.RiegoEvento
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// --- PALETA DE COLORES PREMIUM ---
val PlantDark = Color(0xFF1B5E20)
val PlantPrimary = Color(0xFF4CAF50)
val PlantLight = Color(0xFFE8F5E9)
val PlantAccent = Color(0xFFC8E6C9)
val DangerRed = Color(0xFFD32F2F)
val WarningOrange = Color(0xFFFF9800)
val DeepGray = Color(0xFF455A64)
val SicklyYellow = Color(0xFFFBC02D) // Color para plantas enfermas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: PlantaViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var plantaAEditar by remember { mutableStateOf<Planta?>(null) }
    var plantaParaHistorial by remember { mutableStateOf<Planta?>(null) } // State for the history sheet
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showExcesoDialog by remember { mutableStateOf(false) }
    var ultimoResultadoRiego by remember { mutableStateOf<RiegoResult?>(null) }

    val misPlantas by viewModel.todasLasPlantas.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventoRiego.collectLatest { resultado ->
            ultimoResultadoRiego = resultado
            showExcesoDialog = true
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = resultado.mensaje,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFBFDFA))) {
        PremiumBackgroundEffect()

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    actionColor = PlantAccent,
                    containerColor = PlantDark,
                    snackbarData = data,
                    shape = RoundedCornerShape(24.dp)
                )
            } },
            topBar = { EliteTopAppBar(isSearching = isSearching) { isSearching = !isSearching } },
            floatingActionButton = {
                SpeedDialFAB(
                    onAddManually = { showDialog = true },
                    onScanWithCamera = { navController.navigate("camera") }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                HeaderBrandingSection(
                    misPlantas = misPlantas,
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
                            key(tiempoActual) { // Forzar recomposición por tiempo
                                PlantCardWrapper(onDelete = { viewModel.eliminarPlanta(planta) }) {
                                    PlantItemElite(
                                        planta = planta,
                                        onRegar = { viewModel.regarPlanta(planta) },
                                        onEdit = { plantaAEditar = planta },
                                        onShowHistory = { plantaParaHistorial = planta },
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }

        if (plantaParaHistorial != null) {
            PlantHistorySheet(
                planta = plantaParaHistorial!!,
                viewModel = viewModel,
                onDismiss = { plantaParaHistorial = null },
                onEdit = {
                    plantaAEditar = plantaParaHistorial
                    plantaParaHistorial = null
                },
                onDelete = {
                    viewModel.eliminarPlanta(plantaParaHistorial!!)
                    plantaParaHistorial = null
                }
            )
        }


        if (showExcesoDialog && ultimoResultadoRiego != null) {
            ExcesoRiegoDialog(
                resultado = ultimoResultadoRiego!!,
                onDismiss = { showExcesoDialog = false }
            )
        }

        if (showDialog) {
            AddPlantDialog(
                onDismiss = { showDialog = false },
                onPlantAdded = { nombre, especie, minutos, imagenUri, tipoLuz, tipoSuelo, notas ->
                    viewModel.agregarPlanta(nombre, especie, minutos, imagenUri, tipoLuz, tipoSuelo, notas)
                    showDialog = false
                }
            )
        }

        if (plantaAEditar != null) {
            AddPlantDialog(
                plantaAEditar = plantaAEditar,
                onDismiss = { plantaAEditar = null },
                onPlantAdded = { nombre, especie, minutos, imagenUri, tipoLuz, tipoSuelo, notas ->
                    viewModel.editarPlanta(plantaAEditar!!, nombre, especie, minutos, imagenUri, tipoLuz, tipoSuelo, notas)
                    plantaAEditar = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantHistorySheet(
    planta: Planta,
    viewModel: PlantaViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFFFBFDFA)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                planta.nombre,
                style = TextStyle(fontWeight = FontWeight.Black, fontSize = 28.sp, color = PlantDark),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                planta.especie,
                style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, color = DeepGray)
            )

            EliteDetailsSection(planta, viewModel)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantAccent, contentColor = PlantDark)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, DangerRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = DangerRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = DangerRed)
                }
            }
        }
    }
}

@Composable
fun EliteDetailsSection(planta: Planta, viewModel: PlantaViewModel) {
    val historial by viewModel.obtenerHistorial(planta.id).collectAsState(initial = emptyList())

    Column {
        // --- MÉTRICAS VITALES ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem(
                icon = Icons.Rounded.Favorite,
                label = "Vitalidad",
                value = "${planta.salud}%",
                color = when {
                    planta.salud > 70 -> PlantPrimary
                    planta.salud > 40 -> WarningOrange
                    else -> DangerRed
                }
            )
            MetricItem(
                icon = Icons.Rounded.TheaterComedy,
                label = "Drama",
                value = "${planta.nivelDrama}/5",
                color = DeepGray
            )
            MetricItem(
                icon = Icons.Rounded.WaterDrop,
                label = "Riego",
                value = formatMinutes(planta.minutosEntreRiegos),
                color = Color(0xFF3C83D4)
            )
        }

        // --- HISTORIAL DE RIEGO ---
        Text(
            "Historial de Cuidado",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlantDark),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        if (historial.isEmpty()) {
            Text(
                "Aún no hay registros de riego para ${planta.nombre}.",
                style = TextStyle(color = DeepGray.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historial.sortedByDescending { it.fecha }) { evento ->
                    HistoryItem(evento)
                }
            }
        }
    }
}

@Composable
fun MetricItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 16.sp, color = PlantDark))
        Text(label, style = TextStyle(fontSize = 12.sp, color = DeepGray.copy(alpha = 0.8f)))
    }
}

fun formatMinutes(totalMinutes: Int): String {
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
fun HistoryItem(evento: RiegoEvento) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlantAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    evento.esSobrerego -> Icons.Rounded.Waves
                    evento.fuePuntual -> Icons.Rounded.CheckCircle
                    else -> Icons.Rounded.Warning
                },
                contentDescription = null,
                tint = when {
                    evento.esSobrerego -> WarningOrange
                    evento.fuePuntual -> PlantPrimary
                    else -> DangerRed
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = dateFormat.format(Date(evento.fecha)),
                style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = PlantDark)
            )
        }
        Text(
            text = when {
                evento.esSobrerego -> "Sobrerego"
                evento.fuePuntual -> "A tiempo"
                else -> "Con retraso"
            },
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepGray)
        )
    }
}

@Composable
fun ExcesoRiegoDialog(resultado: RiegoResult, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp).shadow(30.dp, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier.background(
                    Brush.verticalGradient(listOf(Color.White, PlantLight.copy(alpha = 0.5f)))
                ).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp).background(
                        if (resultado.esExceso) WarningOrange.copy(0.1f) else PlantPrimary.copy(0.1f),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (resultado.esExceso) Icons.Rounded.Waves else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (resultado.esExceso) WarningOrange else PlantPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text("Aviso de Cuidado", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (resultado.esExceso) WarningOrange else PlantPrimary, letterSpacing = 1.sp))
                Spacer(Modifier.height(8.dp))
                Text(resultado.mensaje, style = TextStyle(color = PlantDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 24.sp))
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (resultado.esExceso) WarningOrange else PlantPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text("ENTENDIDO", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp))
                }
            }
        }
    }
}

@Composable
fun PremiumBackgroundEffect() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(brush = Brush.radialGradient(colors = listOf(PlantAccent.copy(alpha = 0.4f), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.1f), radius = 400.dp.toPx()))
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFE1F5FE).copy(alpha = 0.5f), Color.Transparent), center = Offset(size.width * 0.1f, size.height * 0.9f), radius = 600.dp.toPx()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteTopAppBar(isSearching: Boolean, onToggleSearch: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        title = { Text("HEYPLANTY", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 5.sp, brush = Brush.horizontalGradient(listOf(PlantDark, PlantPrimary)))) },
        actions = {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.padding(end = 12.dp).size(42.dp).background(Color.White.copy(0.7f), CircleShape).border(0.5.dp, PlantAccent, CircleShape)
            ) {
                Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, null, tint = PlantDark, modifier = Modifier.size(20.dp))
            }
        }
    )
}

@Composable
fun HeaderBrandingSection(misPlantas: List<Planta>, isSearching: Boolean) {
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = !isSearching }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn(tween(600)) + slideInHorizontally(tween(600)),
        exit = fadeOut(tween(300)) + slideOutHorizontally()
    ) {
        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (saludoTop, saludoBottom, subtexto) = when(horaActual) {
            in 5..11 -> Triple("Despierta,", "Buen día", "Tus brotes buscan el sol")
            in 12..18 -> Triple("Luz vital,", "Buenas tardes", "Momento de frescura en el jardín")
            in 19..23 -> Triple("Calma,", "Buenas noches", "Tus raíces descansan hoy")
            else -> Triple("Silencio,", "Hola noctámbulo", "El jardín sueña bajo la luna")
        }

        val necesitanAgua = misPlantas.count { it.obtenerProgresoRiego() <= 0f && it.fechaUltimoRiego > 0L }

        Column(modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(saludoTop, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PlantPrimary, letterSpacing = 1.sp))
                    Text(saludoBottom, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B301B), letterSpacing = (-1.5).sp, lineHeight = 36.sp))
                    Text(subtexto, style = TextStyle(fontSize = 13.sp, color = DeepGray.copy(alpha = 0.7f), fontWeight = FontWeight.Medium))
                }
                StatusGlassCard(necesitanAgua)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth(0.15f).height(4.dp).clip(CircleShape).background(PlantPrimary.copy(0.3f)))
        }
    }
}

@Composable
fun StatusGlassCard(necesitanAgua: Int) {
    val infiniteTransition = rememberInfiniteTransition("pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "alpha"
    )

    Surface(
        color = if (necesitanAgua > 0) DangerRed.copy(0.08f) else PlantPrimary.copy(0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, if (necesitanAgua > 0) DangerRed.copy(0.15f) else PlantPrimary.copy(0.15f)),
        modifier = Modifier.shadow(if (necesitanAgua > 0) 8.dp else 0.dp, RoundedCornerShape(24.dp))
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).graphicsLayer(alpha = if (necesitanAgua > 0) alphaAnim else 1f).background(if (necesitanAgua > 0) DangerRed else PlantPrimary, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (necesitanAgua > 0) "$necesitanAgua con sed" else "Todo OK", style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (necesitanAgua > 0) DangerRed else PlantDark))
                Text(if (necesitanAgua > 0) "¡A regar!" else "Jardín feliz", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = DeepGray))
            }
        }
    }
}

@Composable
fun SearchSection(isVisible: Boolean, searchText: String, onValueChange: (String) -> Unit) {
    AnimatedVisibility(visible = isVisible, enter = slideInVertically() + expandVertically() + fadeIn(), exit = slideOutVertically() + shrinkVertically() + fadeOut()) {
        TextField(
            value = searchText, onValueChange = onValueChange,
            placeholder = { Text("Buscar por nombre...", color = DeepGray.copy(0.5f)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PlantAccent.copy(0.5f),
                unfocusedContainerColor = PlantAccent.copy(0.3f),
                disabledContainerColor = PlantLight,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = PlantPrimary) }
        )
    }
}

@Composable
fun EmptyGardenIllustration() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.NaturePeople, contentDescription = null, tint = PlantAccent, modifier = Modifier.size(120.dp))
        Text("Tu jardín está vacío", style = MaterialTheme.typography.headlineSmall, color = PlantDark, fontWeight = FontWeight.Bold)
        Text("Añade tu primera planta para empezar a cuidarla", style = MaterialTheme.typography.bodyMedium, color = DeepGray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}


@Composable
fun PlantCardWrapper(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val dismissThreshold = -250f

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        offsetX += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (offsetX < dismissThreshold) {
                            onDelete()
                        }
                        coroutineScope.launch {
                            animate(0f, offsetX) { value, _ -> offsetX = value }
                        }
                    }
                )
            }
            .offset { IntOffset(offsetX.roundToInt(), 0) }
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlantItemElite(
    planta: Planta,
    onRegar: () -> Unit,
    onEdit: () -> Unit,
    onShowHistory: () -> Unit
) {
    val progresoRiego = planta.obtenerProgresoRiego()
    val tiempoRestante = planta.obtenerTiempoRestanteFormateado()

    val colorBarra = when {
        progresoRiego < 0.20f -> DangerRed
        progresoRiego < 0.50f -> WarningOrange
        else -> PlantPrimary
    }

    val cardColor = if (planta.estaEnferma()) SicklyYellow.copy(alpha = 0.1f) else Color.White
    val borderColor = when {
        planta.esCritica() -> DangerRed.copy(alpha = 0.8f)
        planta.estaEnferma() -> SicklyYellow.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.8f)
    }

    Surface(
        modifier = Modifier.shadow(18.dp, RoundedCornerShape(32.dp), spotColor = PlantDark),
        shape = RoundedCornerShape(32.dp),
        color = cardColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onShowHistory,
                    onLongClick = onEdit
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = planta.imagenUri ?: R.drawable.ic_launcher_background,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PlantLight),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (planta.estaEnferma()) {
                        Icon(
                            Icons.Rounded.SentimentVeryDissatisfied,
                            contentDescription = "Planta Enferma",
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                    }
                    Text(planta.nombre, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PlantDark))
                }
                Text(planta.especie, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = DeepGray))
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progresoRiego },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = colorBarra,
                    trackColor = colorBarra.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(tiempoRestante, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorBarra.copy(alpha = 0.8f)))
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onRegar,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PlantPrimary.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = "Regar", tint = PlantPrimary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// -- START: SPEED DIAL FAB IMPLEMENTATION --

enum class SpeedDialState {
    EXPANDED,
    COLLAPSED
}

@Composable
fun SpeedDialFAB(
    onAddManually: () -> Unit,
    onScanWithCamera: () -> Unit
) {
    var currentState by remember { mutableStateOf(SpeedDialState.COLLAPSED) }
    val transition = updateTransition(targetState = currentState, label = "fab_transition")

    val rotation by transition.animateFloat(label = "fab_rotation") { state ->
        if (state == SpeedDialState.EXPANDED) 45f else 0f
    }

    val items = listOf(
        SpeedDialItem("Escanear", Icons.Rounded.Camera, onScanWithCamera),
        SpeedDialItem("Añadir", Icons.Rounded.Edit, onAddManually)
    )

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = currentState == SpeedDialState.EXPANDED,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    SpeedDialActionButton(item)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        FloatingActionButton(
            onClick = {
                currentState = if (currentState == SpeedDialState.EXPANDED) {
                    SpeedDialState.COLLAPSED
                } else {
                    SpeedDialState.EXPANDED
                }
            },
            shape = CircleShape,
            containerColor = PlantDark,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Añadir Planta",
                modifier = Modifier.rotate(rotation).size(32.dp)
            )
        }
    }
}

@Composable
fun SpeedDialActionButton(item: SpeedDialItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.6f),
            tonalElevation = 4.dp
        ) {
            Text(
                item.label,
                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        FloatingActionButton(
            onClick = item.onClick,
            shape = CircleShape,
            containerColor = PlantAccent,
            contentColor = PlantDark,
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(item.icon, contentDescription = item.label)
        }
    }
}

data class SpeedDialItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// -- END: SPEED DIAL FAB IMPLEMENTATION --
