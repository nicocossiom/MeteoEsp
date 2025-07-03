package com.mssdepas.meteoesp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mssdepas.meteoesp.data.local.LocationRepository
import com.mssdepas.meteoesp.ui.MainViewModel
import com.mssdepas.meteoesp.ui.SearchMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {

    val uiItems by vm.uiItems.collectAsState()
    val weather by vm.selectedWeather.collectAsState()
    val currentLocationWeather by vm.currentLocationWeather.collectAsState()
    val isLoadingCurrentLocation by vm.isLoadingCurrentLocation.collectAsState()
    val favoriteWeathers by vm.favoriteWeathers.collectAsState()
    val searchMode by vm.searchMode.collectAsState()
    val locationError by vm.locationError.collectAsState()

    var query by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Simplified single permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                vm.onLocationPermissionGranted()
            } else {
                showPermissionDialog = true
            }
        }
    )

    // Check permissions on startup
    LaunchedEffect(Unit) {
        val hasLocationPermission = LocationRepository.hasFineLocationPermission(context)

        if (hasLocationPermission) {
            vm.onLocationPermissionGranted()
        } else {
            // Request coarse location permission (sufficient for weather)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION )
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (weather != null) {
        WeatherDialog(weather!!) { vm.dismissWeather() }
        return
    }

    locationError?.let { error ->
        AlertDialog(
            onDismissRequest = { vm.dismissLocationErrorDialog() },
            title = { Text("Error de ubicación") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { vm.dismissLocationErrorDialog() }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.dismissLocationErrorDialog()
                    vm.retryLocationWeather()
                }) {
                    Text("Reintentar")
                }
            }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permisos de ubicación") },
            text = {
                Text("Esta aplicación necesita acceso a su ubicación para mostrar el tiempo local. Puede conceder el permiso ahora o más tarde en la configuración.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) {
                    Text("Conceder permiso")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Ahora no")
                }
            }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Tiempo España") },
            navigationIcon = {
                // This is a bit of a hack to show a back button.
                // A proper navigation solution (e.g., NavController) would be better.
                if (uiItems.all { it is MainViewModel.UiItem.Mun } && searchMode != SearchMode.MUNICIPIO) {
                    IconButton(onClick = { vm.onBackToProvincias() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver a provincias")
                    }
                }
            }
        )
    }) { padding ->

        Column(Modifier.padding(padding)) {

            // Current Location Weather Section - Always show at top
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Tu ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                when {
                    isLoadingCurrentLocation -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Obteniendo ubicación...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    currentLocationWeather != null -> {
                        WeatherCard(weather = currentLocationWeather!!)
                    }
                    else -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Ubicación no disponible",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { vm.retryLocationWeather() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {

                if (favoriteWeathers.isNotEmpty()) {
                    item {
                        Text(
                            "Favoritos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )
                    }
                    items(favoriteWeathers) { favWeather ->
                        WeatherCard(weather = favWeather, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }

                item {
                    SearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        query = query,
                        onQueryChange = {
                            query = it
                            vm.filter(it)
                        },
                        onSearch = { vm.filter(query) },
                        active = false,
                        onActiveChange = {},
                        placeholder = { Text("Buscar...") },
                        content = {}
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SearchMode.values().forEach { mode ->
                            Row(
                                Modifier
                                    .selectable(
                                        selected = (mode == searchMode),
                                        onClick = { vm.setSearchMode(mode) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (mode == searchMode),
                                    onClick = null // null recommended for accessibility with selectable
                                )
                                Text(
                                    text = mode.name
                                        .lowercase()
                                        .replaceFirstChar { it.titlecase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                items(uiItems) { uiItem ->
                    when (uiItem) {
                        is MainViewModel.UiItem.Prov ->
                            ProvinciaRow(uiItem.p) { vm.onProvinciaClicked(uiItem.p) }
                        is MainViewModel.UiItem.Mun  ->
                            MunicipioRow(
                                m = uiItem.m,
                                isFavorite = uiItem.isFavorite,
                                onRowClick = { vm.loadWeather(uiItem.m) },
                                onFavClick = { vm.toggleFavorite(uiItem.m) }
                            )
                    }
                }
            }
        }
    }
}