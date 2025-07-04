package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mssdepas.meteoesp.ui.MainViewModel

@Composable
fun MapScreen(vm: MainViewModel) {
    val municipios by vm.municipios.collectAsState()
    val weather by vm.selectedWeather.collectAsState()

    // Centered on Spain
    val spainCameraPosition = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.416775, -3.703790), 5f)
    }

    if (weather != null) {
        WeatherDialog(weather!!) {
            vm.dismissWeather()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = spainCameraPosition,
            onMapClick = { latLng ->
                vm.getWeatherForLatLng(latLng.latitude, latLng.longitude)
            }
        ) {
            municipios.forEach { municipio ->
                val lat = municipio.latitud.replace(',', '.').toDoubleOrNull()
                val lon = municipio.longitud.replace(',', '.').toDoubleOrNull()

                if (lat != null && lon != null) {
                    val position = LatLng(lat, lon)
                    Marker(
                        state = MarkerState(position = position),
                        title = municipio.nombre,
                        snippet = "Pulsa para ver el tiempo",
                        onInfoWindowClick = {
                            vm.loadWeather(municipio)
                        }
                    )
                }
            }
        }
    }
}

