package com.jucar.heyplanty

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jucar.heyplanty.screens.HomeScreen
import com.jucar.heyplanty.ui.theme.HeyPlantyTheme

class MainActivity : ComponentActivity() {

    // Registramos la solicitud de permiso para notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
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

        // Verificamos y pedimos el permiso si es Android 13+ (Tiramisu)
        pedirPermisoNotificaciones()

        enableEdgeToEdge()
        setContent {
            HeyPlantyTheme {
                // Llamada a la pantalla principal
                HomeScreen()
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        // Build.VERSION_CODES.TIRAMISU es el API 33 (Android 13)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Lanzamos la solicitud oficial del sistema
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}