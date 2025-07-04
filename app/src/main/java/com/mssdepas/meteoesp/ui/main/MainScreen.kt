package com.mssdepas.meteoesp.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mssdepas.meteoesp.data.local.LocationRepository
import com.mssdepas.meteoesp.ui.AuthViewModel
import com.mssdepas.meteoesp.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, authViewModel: AuthViewModel) {

    val currentLocationWeather by vm.currentLocationWeather.collectAsState()
    val isLoadingCurrentLocation by vm.isLoadingCurrentLocation.collectAsState()
    val locationError by vm.locationError.collectAsState()
    val weather by vm.selectedWeather.collectAsState()

    val selectedProvincia by vm.selectedProvincia.collectAsState()
    val selectedMunicipio by vm.selectedMunicipio.collectAsState()
    val selectedMunicipioWeather by vm.selectedMunicipioWeather.collectAsState()
    val provinciasFiltradas by vm.provinciasFiltradas.collectAsState()
    val municipiosFiltrados by vm.municipiosFiltrados.collectAsState()
    val showFavoritesManager by vm.showFavoritesManager.collectAsState()
    val favoriteMunicipios by vm.favoriteMunicipios.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showUserProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (weather != null) {
        WeatherDialog(weather!!) { vm.dismissWeather() }
        return
    }

    if (showFavoritesManager) {
        FavoritesManagerDialog(
            favorites = favoriteMunicipios,
            onDismiss = { vm.hideFavoritesManager() },
            onRemove = { vm.removeFavorite(it) },
            onViewWeather = { vm.loadWeather(it) }
        )
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
                Text("Esta aplicación necesita acceso a su ubicación para mostrar el tiempo local.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    if (showUserProfile) {
        UserProfileDialog(
            authViewModel = authViewModel,
            onDismiss = { showUserProfile = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tiempo España") },
                actions = {
                    IconButton(onClick = { showUserProfile = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil de usuario")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Current Location Weather Section
            item {
                CurrentLocationSection(
                    isLoading = isLoadingCurrentLocation,
                    weather = currentLocationWeather,
                    onRetry = { vm.retryLocationWeather() }
                )
            }

            // Favorites Section - Always show
            item {
                FavoritesSection(
                    favoriteMunicipios = favoriteMunicipios,
                    onManageFavorites = { vm.showFavoritesManager() },
                    onFavoriteClick = { vm.loadWeather(it) }
                )
            }

            // Province and Municipality Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Buscar ubicación",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Province Combo Box
                        SearchableComboBox(
                            label = "Provincia",
                            items = provinciasFiltradas,
                            selectedItem = selectedProvincia,
                            onItemSelected = { vm.selectProvincia(it) },
                            onQueryChanged = { vm.filterProvincias(it) },
                            itemText = { it.nombre },
                            onClear = { vm.clearProvinciaSelection() }
                        )

                        // Municipality Combo Box (enabled only when province is selected)
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

                        // Show weather for selected municipality
                        if (selectedMunicipio != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            selectedMunicipioWeather?.let { weather ->
                                WeatherCard(weather = weather)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { vm.addSelectedToFavorites() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Añadir a favoritos")
                                }
                            } ?: Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    favoriteMunicipios: List<com.mssdepas.meteoesp.data.model.Municipio>,
    onManageFavorites: () -> Unit,
    onFavoriteClick: (com.mssdepas.meteoesp.data.model.Municipio) -> Unit
) {
    Column {
        // Header with manage button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Favoritos",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(
                onClick = onManageFavorites
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Gestionar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        if (favoriteMunicipios.isNotEmpty()) {
            favoriteMunicipios.forEach { favMunicipio ->
                FavoriteItemRow(
                    municipio = favMunicipio,
                    onClick = { onFavoriteClick(favMunicipio) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No tienes favoritos",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Agrega ubicaciones para acceso rápido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteItemRow(
    municipio: com.mssdepas.meteoesp.data.model.Municipio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = municipio.nombre,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun CurrentLocationSection(
    isLoading: Boolean,
    weather: com.mssdepas.meteoesp.data.remote.WeatherResponse?,
    onRetry: () -> Unit
) {
    Column {
        Text(
            "Tu ubicación",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when {
            isLoading -> {
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
            weather != null -> {
                WeatherCard(weather = weather)
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
                        TextButton(onClick = onRetry) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}