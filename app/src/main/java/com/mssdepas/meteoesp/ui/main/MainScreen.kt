package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mssdepas.meteoesp.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {

    val items by vm.items.collectAsState()
    val weather by vm.selectedWeather.collectAsState()

    var query by remember { mutableStateOf("") }

    if (weather != null) {
        WeatherDialog(weather!!) { vm.filter(query) } // dismiss -> back to list
        return
    }

    Scaffold(topBar = {
        LargeTopAppBar(title = { Text("Tiempo España") })
    }) { padding ->

        Column(Modifier.padding(padding)) {

            val colors = SearchBarDefaults.colors()

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = {
                            query = it
                            vm.filter(it)
                        },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        enabled = true,
                        placeholder = { Text("Buscar provincia o municipio") },
                        leadingIcon = null,
                        trailingIcon = null,
                        colors = SearchBarDefaults.inputFieldColors(),
                        interactionSource = null
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.fillMaxWidth(),
                shape = SearchBarDefaults.inputFieldShape,
                colors = colors,
                tonalElevation = SearchBarDefaults.TonalElevation,
                shadowElevation = SearchBarDefaults.ShadowElevation,
                windowInsets = SearchBarDefaults.windowInsets
            ) {
                // Aquí puedes poner sugerencias, si quieres en el futuro
            }

            LazyColumn {
                items(items) { uiItem ->
                    when (uiItem) {
                        is MainViewModel.UiItem.Prov ->
                            ProvinciaRow(uiItem.p)
                        is MainViewModel.UiItem.Mun  ->
                            MunicipioRow(uiItem.m) { vm.loadWeather(uiItem.m) }
                    }
                }
            }
        }
    }
}