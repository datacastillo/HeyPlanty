package com.jucar.heyplanty.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddPlantDialog(
    onDismiss: () -> Unit,
    onPlantAdded: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nueva Integrante 🪴") },
        text = {
            Column {
                TextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = dias, onValueChange = { dias = it }, label = { Text("Días") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isNotBlank() && dias.isNotBlank()) {
                    onPlantAdded(nombre, dias)
                    onDismiss()
                }
            }) { Text("Adoptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}