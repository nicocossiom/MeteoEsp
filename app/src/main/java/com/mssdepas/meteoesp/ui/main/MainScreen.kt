package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.ui.MainViewModel
import com.mssdepas.meteoesp.ui.SearchMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {

    val uiItems by vm.uiItems.collectAsState()
    val weather by vm.selectedWeather.collectAsState()
    val currentLocationWeather by vm.currentLocationWeather.collectAsState()
    val favoriteWeathers by vm.favoriteWeathers.collectAsState()
    val searchMode by vm.searchMode.collectAsState()

    var query by remember { mutableStateOf("") }

    if (weather != null) {
        WeatherDialog(weather!!) { vm.dismissWeather() }
        return
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

            LazyColumn(modifier = Modifier.weight(1f)) {

                item {
                    currentLocationWeather?.let {
                        Text(
                            "Tiempo en tu ubicación",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                        WeatherCard(weather = it, modifier = Modifier.padding(16.dp))
                    }
                }

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