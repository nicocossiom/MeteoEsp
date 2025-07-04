package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mssdepas.meteoesp.data.remote.WeatherResponse

@Composable
fun WeatherCard(weather: WeatherResponse, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = weather.municipio?.nombre ?: "Ubicación desconocida",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${weather.temperaturaActual ?: "--"}°C",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Max: ${weather.temperaturas?.max ?: "--"}°C",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Min: ${weather.temperaturas?.min ?: "--"}°C",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            InfoRow(label = "Cielo", value = weather.estadoCielo?.descripcion ?: "--")
            InfoRow(label = "Humedad", value = "${weather.humedad ?: "--"}%")
            InfoRow(label = "Viento", value = "${weather.viento ?: "--"} km/h")
            InfoRow(label = "Precipitación", value = "${weather.precipitacion ?: "0"} mm")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}