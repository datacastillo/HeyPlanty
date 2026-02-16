package com.jucar.heyplanty

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.jucar.heyplanty.classification.PlantClassifier
import com.jucar.heyplanty.data.AppDatabase
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.screens.CameraScreen
import com.jucar.heyplanty.screens.HomeScreen
import com.jucar.heyplanty.ui.theme.HeyPlantyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.classifier.Classifications

class MainActivity : ComponentActivity(), PlantClassifier.ClassificationListener {

    private lateinit var plantClassifier: PlantClassifier
    private val database by lazy { AppDatabase.getDatabase(this) }

    private var imageUriForAnalysis by mutableStateOf<Uri?>(null)
    private var classificationResult by mutableStateOf<Pair<String, Float>?>(null)
    private var showDialog by mutableStateOf(false)

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("HeyPlanty", "Permiso de cámara concedido")
        } else {
            Log.d("HeyPlanty", "Permiso de cámara denegado")
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("HeyPlanty", "Permiso de notificaciones concedido")
        } else {
            Log.d("HeyPlanty", "Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        plantClassifier = PlantClassifier(this, this)

        pedirPermisos()

        enableEdgeToEdge()
        setContent {
            HeyPlantyTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable("camera") {
                        CameraScreen(navController = navController) { uri ->
                            imageUriForAnalysis = uri
                            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = ImageDecoder.createSource(contentResolver, uri)
                                ImageDecoder.decodeBitmap(source)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            }
                            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            analizarImagen(mutableBitmap)
                        }
                    }
                }

                if (showDialog) {
                    classificationResult?.let {
                        ResultDialog(
                            result = it,
                            imageUri = imageUriForAnalysis,
                            onDismiss = { showDialog = false },
                            onSave = {
                                val uriString = imageUriForAnalysis?.toString() ?: ""
                                guardarPlanta(it.first, uriString)
                                showDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun analizarImagen(bitmap: Bitmap) {
        plantClassifier.classify(bitmap, 0)
    }

    private fun guardarPlanta(nombre: String, imagenUri: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.plantaDao().insertPlanta(
                    Planta(
                        nombre = nombre,
                        especie = nombre,
                        minutosEntreRiegos = 0,
                        fechaUltimoRiego = 0L,
                        imagenUri = imagenUri
                    )
                )
            }
        }
    }

    override fun onError(error: String) {
        Log.e("MainActivity", "Classification error: $error")
        runOnUiThread {
            showDialog = false
        }
    }

    override fun onClassificationResult(results: List<Classifications>?) {
        runOnUiThread {
            val bestCategory = results?.firstOrNull()?.categories?.maxByOrNull { it.score }
            if (bestCategory != null && bestCategory.label != "_background_") {
                classificationResult = Pair(bestCategory.label, bestCategory.score)
                showDialog = true
            } else {
                classificationResult = Pair("No se pudo identificar", 0.0f)
                showDialog = true
            }
        }
    }

    private fun pedirPermisos() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun ResultDialog(result: Pair<String, Float>, imageUri: Uri?, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Planta Identificada") },
        text = {
            Column {
                imageUri?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "Planta analizada",
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(text = "Nombre: ${result.first}")
                Text(text = "Confianza: ${String.format("%.2f%%", result.second * 100)}")
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Descartar")
            }
        }
    )
}
