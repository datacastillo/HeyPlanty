package com.jucar.heyplanty.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onPlantAdded: (String, String, String?) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var horas by remember { mutableStateOf("") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imagenUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nueva Integrante 🪴") },
        text = {
            Column {
                TextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = horas, onValueChange = { horas = it }, label = { Text("Riego cada (horas)") })
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
            Button(onClick = {
                if (nombre.isNotBlank() && horas.isNotBlank()) {
                    onPlantAdded(nombre, horas, imagenUri?.toString())
                    onDismiss()
                }
            }) { Text("Adoptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}