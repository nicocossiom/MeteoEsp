package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mssdepas.meteoesp.data.remote.WeatherResponse

@Composable
fun WeatherDialog(
    w: WeatherResponse,
    onDismiss: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
        TextButton(onClick = onDismiss) { Text("OK") }
    },
    title = { Text("Ahora mismo") },
    text = {
        Column {
            Text("🌡️ Temperatura: ${w.temperaturaActual ?: "--"} ºC")
            Text("☁️ Cielo: ${w.estadoCielo?.descripcion ?: "--"}")
            Text("💧 Humedad: ${w.humedad ?: "--"} %")
        }
    }
)
