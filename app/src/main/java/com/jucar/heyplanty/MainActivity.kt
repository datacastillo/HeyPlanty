package com.jucar.heyplanty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jucar.heyplanty.screens.HomeScreen // IMPORT CLAVE: Actualizado a la nueva carpeta
import com.jucar.heyplanty.ui.theme.HeyPlantyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeyPlantyTheme {
                // Aquí ya no debería haber línea roja debajo de HomeScreen
                HomeScreen()
            }
        }
    }
}