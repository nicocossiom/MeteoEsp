package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mssdepas.meteoesp.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    val comboBoxColors = TextFieldDefaults.colors(
        focusedTextColor = Color.Black.copy(alpha = 0.87f),
        unfocusedTextColor = Color.Black.copy(alpha = 0.87f),
        disabledTextColor = Color.Black.copy(alpha = 0.38f),
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Black.copy(alpha = 0.38f),
        disabledIndicatorColor = Color.Black.copy(alpha = 0.12f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color.Black.copy(alpha = 0.6f),
        disabledLabelColor = Color.Black.copy(alpha = 0.38f),
        focusedTrailingIconColor = Color.Black.copy(alpha = 0.54f),
        unfocusedTrailingIconColor = Color.Black.copy(alpha = 0.54f),
        disabledTrailingIconColor = Color.Black.copy(alpha = 0.38f)
    )

    val searchTextFieldColors = TextFieldDefaults.colors(
        focusedTextColor = Color.Black.copy(alpha = 0.87f),
        unfocusedTextColor = Color.Black.copy(alpha = 0.87f),
        disabledTextColor = Color.Black.copy(alpha = 0.38f),
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Black.copy(alpha = 0.38f),
        disabledIndicatorColor = Color.Black.copy(alpha = 0.12f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color.Black.copy(alpha = 0.6f),
        disabledLabelColor = Color.Black.copy(alpha = 0.38f),
        focusedTrailingIconColor = Color.Black.copy(alpha = 0.54f),
        unfocusedTrailingIconColor = Color.Black.copy(alpha = 0.54f),
        disabledTrailingIconColor = Color.Black.copy(alpha = 0.38f)
    )

    val dropdownMenuItemColors = MenuDefaults.itemColors(
        textColor = Color.Black.copy(alpha = 0.87f),
        leadingIconColor = Color.Black.copy(alpha = 0.54f),
        trailingIconColor = Color.Black.copy(alpha = 0.54f),
        disabledTextColor = Color.Black.copy(alpha = 0.38f),
        disabledLeadingIconColor = Color.Black.copy(alpha = 0.38f),
        disabledTrailingIconColor = Color.Black.copy(alpha = 0.38f)
    )

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
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        onClear = { vm.clearProvinciaSelection() },
                        colors = comboBoxColors,
                        searchTextFieldColors = searchTextFieldColors,
                        dropdownMenuModifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .shadow(8.dp),
                        dropdownMenuItemColors = dropdownMenuItemColors
                    )

                    SearchableComboBox(
                        label = "Municipio",
                        items = municipiosFiltrados,
                        selectedItem = selectedMunicipio,
                        onItemSelected = { vm.selectMunicipio(it) },
                        onQueryChanged = { vm.filterMunicipios(it) },
                        itemText = { it.nombre },
                        enabled = selectedProvincia != null,
                        onClear = { vm.clearMunicipioSelection() },
                        colors = comboBoxColors,
                        searchTextFieldColors = searchTextFieldColors,
                        dropdownMenuModifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .shadow(8.dp),
                        dropdownMenuItemColors = dropdownMenuItemColors
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
