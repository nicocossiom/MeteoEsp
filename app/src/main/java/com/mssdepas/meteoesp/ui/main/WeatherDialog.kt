package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mssdepas.meteoesp.data.remote.WeatherResponse

@Composable
fun WeatherDialog(
    w: WeatherResponse,
    onDismiss: () -> Unit
) = AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
        TextButton(onClick = onDismiss) { Text("Cerrar") }
    },
    title = { Text(w.municipio?.nombre ?: "Tiempo") },
    text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            WeatherCard(
                weather = w,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
)
