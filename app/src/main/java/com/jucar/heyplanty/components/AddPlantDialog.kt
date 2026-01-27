package com.jucar.heyplanty.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Definimos las especies y sus tiempos sugeridos (Horas, Minutos, Consejo)
data class EspecieSugerida(val nombre: String, val h: Int, val m: Int, val tip: String)

val catalogoEspecies = listOf(
    EspecieSugerida("Personalizada 🪴", 0, 30, "Tú decides el ritmo de riego."),
    EspecieSugerida("Cactus 🌵", 360, 0, "Aman el sol. Riega solo cuando la tierra esté muy seca."),
    EspecieSugerida("Suculenta 🪴", 168, 0, "Prefieren poca agua pero constante cada semana."),
    EspecieSugerida("Helecho 🌿", 48, 0, "Les encanta la humedad. No dejes que se sequen."),
    EspecieSugerida("Cuna de Moisés 🌸", 72, 0, "Bajan sus hojas cuando tienen sed. Escúchalas."),
    EspecieSugerida("Lengua de Suegra 🐍", 504, 0, "Son guerreras. Aguantan mucho sin agua.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onPlantAdded: (String, String, Int, Int, String?, String) -> Unit // Añadimos especie y consejo
) {
    var nombre by remember { mutableStateOf("") }
    var horas by remember { mutableIntStateOf(0) }
    var minutos by remember { mutableIntStateOf(30) }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para el selector de especies
    var especieSeleccionada by remember { mutableStateOf(catalogoEspecies[0]) }
    var expanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imagenUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nueva Integrante 🪴", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la planta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SELECTOR DE ESPECIE (Dropdown)
                Text("Tipo de Planta", style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(especieSeleccionada.nombre, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        catalogoEspecies.forEach { especie ->
                            DropdownMenuItem(
                                text = { Text(especie.nombre) },
                                onClick = {
                                    especieSeleccionada = especie
                                    horas = especie.h
                                    minutos = especie.m
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // CAJA DE CONSEJO INTELIGENTE
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${especieSeleccionada.tip}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "¿Cada cuánto necesita agua?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeCounter(label = "Horas", value = horas, range = 0..999) { horas = it }
                    Text(":", style = MaterialTheme.typography.headlineLarge)
                    TimeCounter(label = "Minutos", value = minutos, range = 0..59) { minutos = it }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imagenUri == null) "Añadir Foto 📸" else "¡Foto lista! ✅")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onPlantAdded(
                            nombre,
                            especieSeleccionada.nombre,
                            horas,
                            minutos,
                            imagenUri?.toString(),
                            especieSeleccionada.tip
                        )
                    }
                }
            ) { Text("Adoptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun TimeCounter(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Icon(Icons.Default.HorizontalRule, contentDescription = null)
            }
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(45.dp)
            )
            IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
}