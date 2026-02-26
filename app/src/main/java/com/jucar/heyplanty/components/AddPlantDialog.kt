package com.jucar.heyplanty.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.jucar.heyplanty.domain.Planta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- PALETA ELITE ---
private val PlantDark = Color(0xFF1B5E20)
private val PlantPrimary = Color(0xFF4CAF50)
private val PlantLight = Color(0xFFF1F8E9)
private val SoftWhite = Color(0xFFFAFAFA)

// --- CATÁLOGO MAESTRO ---
data class EspecieSugerida(val nombre: String, val icon: String, val d: Int, val h: Int, val m: Int, val luz: String, val suelo: String)
val CATALOGO_MASTER = listOf(
    EspecieSugerida("Monstera Deliciosa", "🌿", 7, 0, 0, "Luz Indirecta", "Universal Premium"),
    EspecieSugerida("Sansevieria", "🐍", 14, 0, 0, "Luz Baja", "Sustrato Cactus"),
    EspecieSugerida("Ficus Lyrata", "🌳", 5, 0, 0, "Luz Brillante", "Drenante"),
    EspecieSugerida("Pothos N-Joy", "🍃", 7, 0, 0, "Luz Indirecta", "Universal"),
    EspecieSugerida("Aloe Vera", "🌱", 10, 0, 0, "Luz Directa", "Arenoso"),
    EspecieSugerida("Helecho de Boston", "🌿", 2, 0, 0, "Sombra", "Orgánico")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onPlantAdded: (nombre: String, especie: String, minutos: Int, imagenUri: String, tipoLuz: String, tipoSuelo: String, notas: String) -> Unit,
    plantaAEditar: Planta? = null
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // Estados Identidad
    val (nombre, setNombre) = remember { mutableStateOf(TextFieldValue(plantaAEditar?.nombre ?: "")) }
    val (especie, setEspecie) = remember { mutableStateOf(plantaAEditar?.especie ?: "") }
    val (imagenUri, setImagenUri) = remember { mutableStateOf<Uri?>(plantaAEditar?.imagenUri?.let { Uri.parse(it) }) }
    
    // Estados Cuidados (Intervalo en D, H, M)
    val totalMinutosInicial = plantaAEditar?.minutosEntreRiegos ?: 10080
    val (d, setD) = remember { mutableIntStateOf(totalMinutosInicial / 1440) }
    val (h, setH) = remember { mutableIntStateOf((totalMinutosInicial % 1440) / 60) }
    val (m, setM) = remember { mutableIntStateOf(totalMinutosInicial % 60) }

    val (tipoDeLuz, setTipoDeLuz) = remember { mutableStateOf(plantaAEditar?.tipoDeLuz ?: "Luz Indirecta") }
    val (tipoDeSuelo, setTipoDeSuelo) = remember { mutableStateOf(plantaAEditar?.tipoDeSuelo ?: "Universal Premium") }
    val (notas, setNotas) = remember { mutableStateOf(TextFieldValue(plantaAEditar?.notas ?: "")) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { setImagenUri(it) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
                colors = CardDefaults.cardColors(containerColor = SoftWhite)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- 1. ETAPAS DE REGISTRO SUPERIOR ---
                    RegistrationStepper(pagerState.currentPage) {
                        coroutineScope.launch { pagerState.animateScrollToPage(it) }
                    }

                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), userScrollEnabled = false) { page ->
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                            when (page) {
                                0 -> IdentitySection(
                                    nombre = nombre, onNombreChange = setNombre,
                                    especie = especie, onEspecieChange = { input ->
                                        setEspecie(input)
                                        CATALOGO_MASTER.find { it.nombre.equals(input, true) }?.let {
                                            setD(it.d); setH(it.h); setM(it.m)
                                            setTipoDeLuz(it.luz); setTipoDeSuelo(it.suelo)
                                        }
                                    },
                                    imagenUri = imagenUri,
                                    onImageClick = { imageLauncher.launch("image/*") }
                                )
                                1 -> CareSection(
                                    d = d, h = h, m = m,
                                    onTimeChange = { nd, nh, nm -> setD(nd); setH(nh); setM(nm) },
                                    luz = tipoDeLuz, setLuz = setTipoDeLuz,
                                    suelo = tipoDeSuelo, setSuelo = setTipoDeSuelo,
                                    notas = notas, onNotasChange = setNotas
                                )
                            }
                        }
                    }

                    FooterNavigation(
                        isFirst = pagerState.currentPage == 0,
                        isValid = nombre.text.isNotBlank() && especie.isNotBlank(),
                        onNext = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        onFinish = {
                            val totalMin = (d * 1440) + (h * 60) + m
                            onPlantAdded(nombre.text, especie, totalMin, imagenUri?.toString() ?: plantaAEditar?.imagenUri ?: "", tipoDeLuz, tipoDeSuelo, notas.text)
                        },
                        onClose = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationStepper(currentPage: Int, onStepClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(Color.LightGray.copy(0.4f)))
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepItem("Identidad", isActive = currentPage >= 0, isCurrent = currentPage == 0) { onStepClick(0) }
            Box(modifier = Modifier.weight(1f).height(2.dp).background(if (currentPage >= 1) PlantPrimary else Color.LightGray.copy(0.3f)))
            StepItem("Cuidados", isActive = currentPage >= 1, isCurrent = currentPage == 1) { onStepClick(1) }
        }
    }
}

@Composable
fun StepItem(label: String, isActive: Boolean, isCurrent: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (isActive) PlantPrimary else Color.LightGray.copy(0.5f)))
        Text(label, fontSize = 11.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isActive) PlantDark else Color.Gray)
    }
}

@Composable
fun IdentitySection(
    nombre: TextFieldValue, onNombreChange: (TextFieldValue) -> Unit,
    especie: String, onEspecieChange: (String) -> Unit,
    imagenUri: Uri?, onImageClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        
        Box(
            modifier = Modifier.size(130.dp).shadow(16.dp, RoundedCornerShape(48.dp)).clip(RoundedCornerShape(48.dp)).background(Color.White).clickable(onClick = onImageClick),
            contentAlignment = Alignment.Center
        ) {
            if (imagenUri != null) {
                AsyncImage(model = imagenUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = PlantPrimary, modifier = Modifier.size(36.dp))
                    Text("Retrato", fontWeight = FontWeight.Black, color = PlantPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- CAMPO NOMBRE CON CARRUSEL PROACTIVO EN EL LABEL ---
        var isNameFocused by remember { mutableStateOf(false) }
        val suggestedNames = listOf("Confi", "Verdiz", "Esperanza", "Bebé", "Clorofila", "Jade")
        var nIdx by remember { mutableIntStateOf(0) }
        LaunchedEffect(isNameFocused, nombre.text) {
            if (!isNameFocused && nombre.text.isEmpty()) {
                while (true) { delay(2500); nIdx = (nIdx + 1) % suggestedNames.size }
            }
        }

        OutlinedTextField(
            value = nombre, onValueChange = onNombreChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { isNameFocused = it.isFocused },
            label = {
                if (!isNameFocused && nombre.text.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nombre (ej: ")
                        AnimatedContent(
                            targetState = suggestedNames[nIdx],
                            transitionSpec = { (slideInVertically { h -> h } + fadeIn()).togetherWith(slideOutVertically { h -> -h } + fadeOut()) },
                            label = "NameCarousel"
                        ) { name -> Text(name, color = PlantPrimary.copy(alpha = 0.8f), fontWeight = FontWeight.Bold) }
                        Text(")")
                    }
                } else {
                    Text("Nombre")
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlantPrimary, focusedLabelColor = PlantPrimary),
            singleLine = true
        )

        Spacer(Modifier.height(28.dp))

        // --- SELECCIÓN DE PLANTA (ELITE) ---
        Text("¿Qué planta es?", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, color = PlantDark), modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(
            value = especie, onValueChange = onEspecieChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Escribe o selecciona abajo...") },
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = PlantPrimary) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlantPrimary)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Sugerencias populares", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(10.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CATALOGO_MASTER.forEach { item ->
                    val isSelected = especie == item.nombre
                    Surface(
                        onClick = { onEspecieChange(item.nombre) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) PlantPrimary else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) PlantPrimary else Color.LightGray.copy(0.4f)),
                        modifier = Modifier.animateContentSize().shadow(if (isSelected) 12.dp else 4.dp, RoundedCornerShape(16.dp))
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(item.nombre, fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.White else PlantDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CareSection(
    d: Int, h: Int, m: Int,
    onTimeChange: (Int, Int, Int) -> Unit,
    luz: String, setLuz: (String) -> Unit,
    suelo: String, setSuelo: (String) -> Unit,
    notas: TextFieldValue, onNotasChange: (TextFieldValue) -> Unit
) {
    Text("Configurar Cuidados", style = TextStyle(fontWeight = FontWeight.Black, fontSize = 24.sp, color = PlantDark))
    Text("Define el ritmo perfecto para su salud.", fontSize = 14.sp, color = Color.Gray)
    Spacer(Modifier.height(28.dp))
    Text("Intervalo de Riego", fontWeight = FontWeight.Bold, color = PlantDark, fontSize = 16.sp)
    Spacer(Modifier.height(12.dp))
    WateringWheelPicker(d, h, m, onTimeChange)
    Spacer(Modifier.height(32.dp))
    CareAdjustmentCard("Iluminación Ideal", Icons.Rounded.WbSunny, luz) {
        setLuz(when(luz) { "Luz Indirecta" -> "Luz Directa" "Luz Directa" -> "Sombra" else -> "Luz Indirecta" })
    }
    CareAdjustmentCard("Tipo de Sustrato", Icons.Rounded.Grass, suelo) {
        setSuelo(when(suelo) { "Universal Premium" -> "Drenante" "Drenante" -> "Orgánico" else -> "Universal Premium" })
    }
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = notas, onValueChange = onNotasChange,
        label = { Text("Notas o recordatorios...") },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun WateringWheelPicker(d: Int, h: Int, m: Int, onValueChange: (Int, Int, Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)).background(PlantLight.copy(0.4f)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeWheelColumn(label = "Días", range = 0..31, current = d) { onValueChange(it, h, m) }
        TimeWheelColumn(label = "Horas", range = 0..23, current = h) { onValueChange(d, it, m) }
        TimeWheelColumn(label = "Min", range = 0..59, current = m) { onValueChange(d, h, it) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.TimeWheelColumn(label: String, range: IntRange, current: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = range.indexOf(current))
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex in 0 until (range.last - range.first + 1)) {
                onSelect(range.first + centerIndex)
            }
        }
    }
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = PlantPrimary)
        Box(modifier = Modifier.height(100.dp)) {
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                contentPadding = PaddingValues(vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(range.last - range.first + 1) { i ->
                    val value = range.first + i
                    val isSelected = value == current
                    Text(
                        text = value.toString().padStart(2, '0'),
                        fontSize = if (isSelected) 22.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        color = if (isSelected) PlantDark else Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp).alpha(if (isSelected) 1f else 0.3f)
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(PlantPrimary.copy(0.2f)).align(Alignment.TopCenter).offset(y = 34.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(PlantPrimary.copy(0.2f)).align(Alignment.BottomCenter).offset(y = (-34).dp))
        }
    }
}

@Composable
fun CareAdjustmentCard(label: String, icon: ImageVector, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray.copy(0.2f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PlantPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value, fontWeight = FontWeight.Bold, color = PlantDark)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun FooterNavigation(isFirst: Boolean, isValid: Boolean, onNext: () -> Unit, onFinish: () -> Unit, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onClose) { Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = if (isFirst) onNext else onFinish,
            enabled = isValid,
            modifier = Modifier.height(54.dp).widthIn(min = 130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary)
        ) {
            Text(if (isFirst) "Siguiente" else "¡Listo!", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}
