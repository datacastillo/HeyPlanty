package com.jucar.heyplanty.screens

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantAnalysisScreen(navController: NavController, imageUri: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de Planta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            AsyncImage(
                model = Uri.parse(imageUri),
                contentDescription = "Planta capturada",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            // Aquí irá el análisis de la IA
            Text(text = "Analizando imagen...", modifier = Modifier.padding(paddingValues))
        }
    }
}