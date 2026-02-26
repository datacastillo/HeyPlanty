package com.jucar.heyplanty.screens

import android.content.Context
import android.hardware.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: PlantaViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var plantaAEditar by remember { mutableStateOf<Planta?>(null) }
    var plantaParaHistorial by remember { mutableStateOf<Planta?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var tiempoActual by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showExcesoDialog by remember { mutableStateOf(false) }
    var ultimoResultadoRiego by remember { mutableStateOf<RiegoResult?>(null) }

    // Estados para el Luxómetro
    var showLightMeter by remember { mutableStateOf(false) }
    var targetLightForMeter by remember { mutableStateOf("") }

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
                            key(tiempoActual) {
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
                },
                onMeasureLight = { target ->
                    targetLightForMeter = target
                    showLightMeter = true
                }
            )
        }

        if (showLightMeter) {
            LightMeterDialog(
                targetLight = targetLightForMeter,
                onDismiss = { showLightMeter = false }
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
    onDelete: () -> Unit,
    onMeasureLight: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray.copy(0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // --- SECCIÓN HERO PRO ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = planta.imagenUri ?: R.drawable.ic_launcher_background,
                    contentDescription = null,
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(16.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(PlantLight),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(
                        planta.nombre,
                        style = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, color = PlantDark, letterSpacing = (-1).sp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            planta.especie,
                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PlantPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        val dificultad = when {
                            planta.nivelDrama >= 4 -> "Experto"
                            planta.nivelDrama >= 2 -> "Intermedio"
                            else -> "Principiante"
                        }
                        Surface(color = PlantAccent.copy(0.4f), shape = CircleShape) {
                            Text(dificultad, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PlantDark)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- SEGURIDAD PARA MASCOTAS ---
            val esToxica = listOf("Monstera", "Sansevieria", "Ficus", "Pothos", "Aloe").any { planta.especie.contains(it, true) }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (esToxica) Color(0xFFFFEBEE) else Color(0xFFF1F8E9),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (esToxica) Icons.Rounded.Warning else Icons.Rounded.Pets,
                        null,
                        tint = if (esToxica) DangerRed else PlantPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (esToxica) "Tóxica para mascotas" else "Segura para mascotas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esToxica) DangerRed else PlantDark
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- DASHBOARD DE VITALIDAD ---
            Text("Estado Vital", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PlantDark)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), Icons.Rounded.Favorite, "Salud", "${planta.salud}%", if (planta.salud > 50) PlantPrimary else DangerRed)
                MetricCard(Modifier.weight(1f), Icons.Rounded.TheaterComedy, "Drama", "${planta.nivelDrama}/5", if (planta.nivelDrama > 3) WarningOrange else PlantPrimary)
                MetricCard(Modifier.weight(1f), Icons.Rounded.WaterDrop, "Riego", formatMinutes(planta.minutosEntreRiegos), Color(0xFF3C83D4))
            }

            Spacer(Modifier.height(32.dp))

            // --- GUÍA DE CUIDADOS ---
            Text("Guía de Cuidados", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PlantDark)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RequirementItem(
                    Modifier.weight(1f),
                    Icons.Rounded.WbSunny,
                    "Luz",
                    planta.tipoDeLuz.ifEmpty { "Indirecta" },
                    onClick = { onMeasureLight(planta.tipoDeLuz.ifEmpty { "Indirecta" }) }
                )
                RequirementItem(Modifier.weight(1f), Icons.Rounded.Grass, "Suelo", planta.tipoDeSuelo.ifEmpty { "Universal" })
            }

            // --- CONSEJO DEL EXPERTO ---
            if (planta.consejo.isNotBlank()) {
                Spacer(Modifier.height(32.dp))
                Text("Consejo del Experto", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PlantDark)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = Color(0xFFFFF9C4),
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.TipsAndUpdates, null, tint = WarningOrange, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            planta.consejo,
                            style = TextStyle(fontSize = 14.sp, color = PlantDark, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                        )
                    }
                }
            }

            // --- TUS NOTAS ---
            if (planta.notas.isNotBlank()) {
                Spacer(Modifier.height(32.dp))
                Text("Tus Notas", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PlantDark)
                Text(
                    planta.notas,
                    modifier = Modifier.padding(top = 8.dp).alpha(0.8f),
                    style = TextStyle(fontSize = 15.sp, color = DeepGray, lineHeight = 22.sp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- HISTORIAL ---
            Text("Historial Reciente", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PlantDark)
            Spacer(Modifier.height(12.dp))
            EliteDetailsSection(planta, viewModel)

            // --- ACCIONES ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantAccent, contentColor = PlantDark)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar", fontWeight = FontWeight.Black)
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(56.dp).background(DangerRed.copy(0.1f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Delete, null, tint = DangerRed)
                }
            }
        }
    }
}

@Composable
fun EliteDetailsSection(planta: Planta, viewModel: PlantaViewModel) {
    val historial by viewModel.obtenerHistorial(planta.id).collectAsState(initial = emptyList())
    Column {
        if (historial.isEmpty()) {
            Text("Aún no hay registros de riego.", style = TextStyle(color = Color.Gray), modifier = Modifier.padding(vertical = 16.dp))
        } else {
            historial.sortedByDescending { it.fecha }.take(5).forEach { evento ->
                HistoryItem(evento)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier.shadow(6.dp, RoundedCornerShape(20.dp)),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = PlantDark)
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RequirementItem(modifier: Modifier, icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .background(PlantLight.copy(0.5f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = PlantPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = PlantDark)
        }
    }
}

fun formatMinutes(totalMinutes: Int): String {
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h"
        else -> "${totalMinutes}m"
    }
}

@Composable
fun HistoryItem(evento: RiegoEvento) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().background(PlantLight.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    evento.esSobrerego -> Icons.Rounded.Waves
                    evento.fuePuntual -> Icons.Rounded.CheckCircle
                    else -> Icons.Rounded.History
                },
                contentDescription = null,
                tint = when {
                    evento.esSobrerego -> WarningOrange
                    evento.fuePuntual -> PlantPrimary
                    else -> DangerRed
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(dateFormat.format(Date(evento.fecha)), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PlantDark)
        }
        Text(
            if (evento.esSobrerego) "Sobrerego" else if (evento.fuePuntual) "Perfecto" else "Tarde",
            fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray
        )
    }
}

@Composable
fun ExcesoRiegoDialog(resultado: RiegoResult, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (resultado.esExceso) Icons.Rounded.Waves else Icons.Rounded.CheckCircle,
                    null,
                    tint = if (resultado.esExceso) WarningOrange else PlantPrimary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(resultado.mensaje, style = TextStyle(color = PlantDark, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center))
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = if (resultado.esExceso) WarningOrange else PlantPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun PremiumBackgroundEffect() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(brush = Brush.radialGradient(colors = listOf(PlantAccent.copy(alpha = 0.4f), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.1f), radius = 400.dp.toPx()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteTopAppBar(isSearching: Boolean, onToggleSearch: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        title = { Text("HEYPLANTY", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 5.sp, brush = Brush.horizontalGradient(listOf(PlantDark, PlantPrimary)))) },
        actions = {
            IconButton(onClick = onToggleSearch, modifier = Modifier.padding(end = 12.dp).size(42.dp).background(Color.White.copy(0.7f), CircleShape).border(0.5.dp, PlantAccent, CircleShape)) {
                Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, null, tint = PlantDark, modifier = Modifier.size(20.dp))
            }
        }
    )
}

@Composable
fun HeaderBrandingSection(misPlantas: List<Planta>, isSearching: Boolean) {
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = !isSearching }
    AnimatedVisibility(visibleState = transitionState, enter = fadeIn() + slideInHorizontally(), exit = fadeOut() + slideOutHorizontally()) {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when(hora) {
            in 5..11 -> "Buen día"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        val necesitanAgua = misPlantas.count { it.obtenerProgresoRiego() <= 0f && it.fechaUltimoRiego > 0L }
        Column(modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hey,", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = PlantPrimary))
                    Text(saludo, style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B301B), letterSpacing = (-1.5).sp))
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
    Surface(
        color = if (necesitanAgua > 0) DangerRed.copy(0.08f) else PlantPrimary.copy(0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, if (necesitanAgua > 0) DangerRed.copy(0.15f) else PlantPrimary.copy(0.15f))
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (necesitanAgua > 0) DangerRed else PlantPrimary, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (necesitanAgua > 0) "$necesitanAgua con sed" else "Todo OK", style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (necesitanAgua > 0) DangerRed else PlantDark))
                Text(if (necesitanAgua > 0) "¡Riega ya!" else "Jardín feliz", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = DeepGray))
            }
        }
    }
}

@Composable
fun SearchSection(isVisible: Boolean, searchText: String, onValueChange: (String) -> Unit) {
    AnimatedVisibility(visible = isVisible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        TextField(
            value = searchText, onValueChange = onValueChange,
            placeholder = { Text("Buscar planta...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = PlantAccent.copy(0.3f), unfocusedContainerColor = PlantAccent.copy(0.1f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = PlantPrimary) }
        )
    }
}

@Composable
fun EmptyGardenIllustration() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.NaturePeople, null, tint = PlantAccent, modifier = Modifier.size(120.dp))
        Text("Tu jardín está vacío", fontSize = 20.sp, color = PlantDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlantCardWrapper(onDelete: () -> Unit, content: @Composable () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.pointerInput(Unit) { detectHorizontalDragGestures(onHorizontalDrag = { c, d -> offsetX += d; c.consume() }, onDragEnd = { if (offsetX < -250f) onDelete(); scope.launch { animate(0f, offsetX) { v, _ -> offsetX = v } } }) }.offset { IntOffset(offsetX.roundToInt(), 0) }) { content() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlantItemElite(planta: Planta, onRegar: () -> Unit, onEdit: () -> Unit, onShowHistory: () -> Unit) {
    val progreso = planta.obtenerProgresoRiego()
    val colorBarra = if (progreso < 0.2f) DangerRed else if (progreso < 0.5f) WarningOrange else PlantPrimary
    Surface(modifier = Modifier.shadow(18.dp, RoundedCornerShape(32.dp)), shape = RoundedCornerShape(32.dp), color = Color.White) {
        Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onShowHistory, onLongClick = onEdit).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = planta.imagenUri ?: R.drawable.ic_launcher_background, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(PlantLight), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(planta.nombre, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, color = PlantDark))
                Text(planta.especie, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = colorBarra, trackColor = colorBarra.copy(0.2f))
            }
            IconButton(onClick = onRegar, modifier = Modifier.size(52.dp).clip(CircleShape).background(PlantPrimary.copy(0.1f))) {
                Icon(Icons.Rounded.WaterDrop, null, tint = PlantPrimary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun SpeedDialFAB(onAddManually: () -> Unit, onScanWithCamera: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 45f else 0f)
    Column(horizontalAlignment = Alignment.End) {
        if (expanded) {
            SpeedDialActionButton(SpeedDialItem("Escanear", Icons.Rounded.Camera, onScanWithCamera))
            Spacer(Modifier.height(12.dp))
            SpeedDialActionButton(SpeedDialItem("Añadir", Icons.Rounded.Edit, onAddManually))
            Spacer(Modifier.height(12.dp))
        }
        FloatingActionButton(onClick = { expanded = !expanded }, shape = CircleShape, containerColor = PlantDark, contentColor = Color.White) {
            Icon(Icons.Default.Add, null, modifier = Modifier.rotate(rotation))
        }
    }
}

@Composable
fun SpeedDialActionButton(item: SpeedDialItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(0.6f)) { Text(item.label, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp) }
        FloatingActionButton(onClick = item.onClick, shape = CircleShape, containerColor = PlantAccent, modifier = Modifier.size(48.dp)) { Icon(item.icon, null, tint = PlantDark) }
    }
}

data class SpeedDialItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun LightMeterDialog(targetLight: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val lightSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var luxValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.get(0)?.let { luxValue = it }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.LightMode, null, tint = WarningOrange, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Luxómetro Real", fontWeight = FontWeight.Black, fontSize = 20.sp, color = PlantDark)
                Text("Mide la luz en la ubicación de tu maceta", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                
                Spacer(Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier.size(130.dp).clip(CircleShape).background(PlantLight.copy(0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(luxValue.toInt().toString(), fontSize = 36.sp, fontWeight = FontWeight.Black, color = PlantDark)
                        Text("LUX", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlantPrimary)
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                val (recomendacion, color) = evaluarLuz(luxValue, targetLight)
                Surface(
                    color = color.copy(0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        recomendacion,
                        modifier = Modifier.padding(12.dp),
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color, textAlign = TextAlign.Center)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantDark)
                ) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

fun evaluarLuz(lux: Float, target: String): Pair<String, Color> {
    return when {
        target.contains("Directa", true) -> {
            if (lux > 10000) "¡Perfecto! Luz solar directa ideal para tu planta." to PlantPrimary
            else "Poca luz. Necesita sol directo (>10,000 lux)." to DangerRed
        }
        target.contains("Indirecta", true) -> {
            if (lux in 2500f..10000f) "¡Ideal! Luz brillante indirecta detectada." to PlantPrimary
            else if (lux > 10000) "¡Cuidado! Demasiado sol, las hojas podrían quemarse." to WarningOrange
            else "Luz insuficiente para esta especie." to DangerRed
        }
        else -> { // Sombra / Baja
            if (lux in 500f..2500f) "¡Perfecto! Sombra luminosa detectada." to PlantPrimary
            else if (lux > 2500) "Demasiada luz para esta planta." to WarningOrange
            else "Demasiada oscuridad para su crecimiento." to DangerRed
        }
    }
}
