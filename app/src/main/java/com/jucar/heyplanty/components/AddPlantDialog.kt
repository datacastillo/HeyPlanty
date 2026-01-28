package com.jucar.heyplanty.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onPlantAdded: (String, String, Int, Int, String?, Color) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf("") }
    var imagenUri by remember { mutableStateOf<String?>(null) }

    val sugerencias = mapOf(
        "Cactus 🌵" to Triple(15, 0, 0),
        "Suculenta 🪴" to Triple(7, 0, 0),
        "Rosal 🌹" to Triple(2, 0, 0),
        "Helecho 🌿" to Triple(1, 0, 0),
        "Lavanda 💜" to Triple(3, 0, 0),
        "Monstera 🍃" to Triple(5, 0, 0)
    )

    var selDias by remember { mutableIntStateOf(0) }
    var selHoras by remember { mutableIntStateOf(0) }
    var selMinutos by remember { mutableIntStateOf(30) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val file = File(context.filesDir, "img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            imagenUri = file.absolutePath
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nueva Integrante 🌿", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))

                Spacer(Modifier.height(16.dp))

                Box(Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFF1F8E9)).clickable { launcher.launch("image/*") }) {
                    if (imagenUri != null) {
                        AsyncImage(model = File(imagenUri!!), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.align(Alignment.Center), tint = Color(0xFF4CAF50))
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        focusedLabelColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(12.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(sugerencias.keys.toList()) { sug ->
                        SuggestionChip(
                            onClick = {
                                especie = sug
                                val tiempo = sugerencias[sug]!!
                                selDias = tiempo.first
                                selHoras = tiempo.second
                                selMinutos = tiempo.third
                            },
                            label = { Text(sug) },
                            shape = RoundedCornerShape(12.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color(0xFF2E7D32), containerColor = Color(0xFFE8F5E9))
                        )
                    }
                }

                OutlinedTextField(
                    value = especie, onValueChange = { especie = it },
                    label = { Text("Especie") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        focusedLabelColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(20.dp))
                Text("Frecuencia de Riego", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)

                Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TimeWheelPicker(count = 31, label = "Días", value = selDias) { selDias = it }
                    TimeWheelPicker(count = 24, label = "Horas", value = selHoras) { selHoras = it }
                    TimeWheelPicker(count = 60, label = "Min", value = selMinutos) { selMinutos = it }
                }

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, Modifier.weight(1f)) { Text("CANCELAR", color = Color.Gray) }
                    Button(
                        onClick = {
                            val totalMin = (selDias * 1440) + (selHoras * 60) + selMinutos
                            if (nombre.isNotBlank() && totalMin > 0) {
                                onPlantAdded(nombre, especie, totalMin, 100, imagenUri, Color(0xFF4CAF50))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("GUARDAR", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun TimeWheelPicker(count: Int, label: String, value: Int, onSelect: (Int) -> Unit) {
    val itemHeight = 50.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != value) {
            scope.launch { listState.animateScrollToItem(value) }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onSelect(listState.firstVisibleItemIndex)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        Box(Modifier.height(itemHeight * 3).width(75.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(itemHeight).background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)))
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = itemHeight)
            ) {
                items(count) { i ->
                    Box(Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = i.toString(),
                            fontSize = 22.sp,
                            fontWeight = if (i == listState.firstVisibleItemIndex) FontWeight.Black else FontWeight.Normal,
                            color = if (i == listState.firstVisibleItemIndex) Color(0xFF1B5E20) else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}