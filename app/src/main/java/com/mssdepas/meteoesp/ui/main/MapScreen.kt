package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mssdepas.meteoesp.ui.MainViewModel

@Composable
fun MapScreen(vm: MainViewModel) {
    val weather by vm.selectedWeather.collectAsState()
    val mapMarkers by vm.mapMarkers.collectAsState()
    val showFavoritesOnMap by vm.showFavoritesOnMap.collectAsState()
    val mapCameraPosition by vm.mapCameraPosition.collectAsState()

    val provinciasFiltradas by vm.provinciasFiltradas.collectAsState()
    val municipiosFiltrados by vm.municipiosFiltrados.collectAsState()
    val selectedProvincia by vm.selectedProvincia.collectAsState()
    val selectedMunicipio by vm.selectedMunicipio.collectAsState()

    // Centered on Spain
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.416775, -3.703790), 5f)
    }

    // Animate camera when a municipio is selected
    LaunchedEffect(mapCameraPosition) {
        mapCameraPosition?.let {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(it),
                1000
            )
        }
    }

    if (weather != null) {
        WeatherDialog(weather!!) {
            vm.dismissWeather()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                vm.getWeatherForLatLng(latLng.latitude, latLng.longitude)
            }
        ) {
            mapMarkers.forEach { municipio ->
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Buscar ubicación en el mapa",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SearchableComboBox(
                        label = "Provincia",
                        items = provinciasFiltradas,
                        selectedItem = selectedProvincia,
                        onItemSelected = { vm.selectProvincia(it) },
                        onQueryChanged = { vm.filterProvincias(it) },
                        itemText = { it.nombre },
                        onClear = { vm.clearProvinciaSelection() }
                    )

                    SearchableComboBox(
                        label = "Municipio",
                        items = municipiosFiltrados,
                        selectedItem = selectedMunicipio,
                        onItemSelected = { vm.selectMunicipio(it) },
                        onQueryChanged = { vm.filterMunicipios(it) },
                        itemText = { it.nombre },
                        enabled = selectedProvincia != null,
                        onClear = { vm.clearMunicipioSelection() }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { vm.toggleShowFavoritesOnMap() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (showFavoritesOnMap) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Mostrar/Ocultar Favoritos"
            )
        }
    }
}
