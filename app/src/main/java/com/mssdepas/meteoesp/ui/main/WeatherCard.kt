package com.mssdepas.meteoesp.ui.main

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mssdepas.meteoesp.data.remote.ProximoDia
import com.mssdepas.meteoesp.data.remote.WeatherResponse
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherCard(
    weather: WeatherResponse,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = colors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Info: Location, Temp, Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.municipio?.nombre ?: "Ubicación",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    weather.estadoCielo?.descripcion?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                weather.temperaturaActual?.let {
                    Text(
                        text = "$it°",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            // High / Low Temp
            weather.temperaturas?.let { temps ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Max: ${temps.max}°   Min: ${temps.min}°",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Data generation time
            weather.elaborado?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Datos generados: ${formatElaboradoDate(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Hourly Forecast (Today)
            weather.pronostico?.hoy?.let { hoy ->
                Text("Pronóstico para hoy", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val hours = hoy.temperatura?.indices ?: IntRange.EMPTY
                    items(hours.count()) { index ->
                        HourlyForecastItem(
                            hour = hoy.temperatura?.getOrNull(index)?.let { "$index:00" } ?: "",
                            temp = hoy.temperatura?.getOrNull(index) ?: "",
                            description = hoy.estadoCieloDescripcion?.getOrNull(index) ?: ""
                        )
                    }
                }
            }

            // Daily Forecast
            weather.proximosDias?.let {
                if (it.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Próximos días", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        it.forEach { dia ->
                            DailyForecastItem(dia)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastItem(hour: String, temp: String, description: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(hour, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        // Here you could map description to an Icon
        Text(
            "$temp°",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

@Composable
private fun DailyForecastItem(dia: ProximoDia) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dia.atributos?.fecha?.let { formatDate(it) } ?: "Día",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        // Here you could map description to an Icon
        Text(
            text = dia.estadoCieloDescripcion?.firstOrNull() ?: "",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${dia.temperatura?.maxima}° / ${dia.temperatura?.minima}°",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatElaboradoDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(dateString)
        DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEE, d MMM", Locale("es", "ES"))
        parser.parse(dateString)?.let { formatter.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
