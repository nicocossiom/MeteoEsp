package com.mssdepas.meteoesp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia
import com.mssdepas.meteoesp.data.remote.RetrofitInstance
import com.mssdepas.meteoesp.data.remote.WeatherResponse
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    sealed interface UiItem {                        // union type for the list
        data class Prov(val p: Provincia) : UiItem
        data class Mun(val m: Municipio) : UiItem
    }

    private val api = RetrofitInstance.api

    private val _items = MutableStateFlow<List<UiItem>>(emptyList())
    val items: StateFlow<List<UiItem>> = _items

    private val _selectedWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedWeather: StateFlow<WeatherResponse?> = _selectedWeather

    init {
        viewModelScope.launch {
            try {// 1. Load provincias
                val provs = api.getProvincias().provincias
                val provItems = provs.map { UiItem.Prov(it) }
                _items.value = provItems

                // 2. Prefetch ALL municipios once (52 requests, ~8 000 items => OK)
                provs.forEach { prov ->
                    launch {
                        try {
                            val muns = api.getMunicipios(prov.id).municipios
                            val munItems = muns.map { UiItem.Mun(it) }
                            _items.update { it + munItems }  // append
                        } catch (e: Exception) {
                            AppLogger.e("Error cargando municipios de ${prov.nombre}:\n${e.message}", throwable = e )
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error cargando provincias:\n${e.message}", throwable = e )
                e.printStackTrace()
            }
        }
    }

    /** Called from UI when query changes */
    fun filter(query: String) = viewModelScope.launch {
        if (query.isBlank()) {
            _items.update { it.sortedByName() }
            return@launch
        }
        _items.update { list ->
            list.filter {
                when (it) {
                    is UiItem.Prov -> it.p.nombre.contains(query, true)
                    i   s UiItem.Mun  -> it.m.nombre.contains(query, true)
                }
            }
        }
    }

    /** Called from UI when user clicks a municipio */
    fun loadWeather(m: Municipio) = viewModelScope.launch {
        val id5 = m.codigoINE.take(5)           // first 5 digits
        _selectedWeather.value =
            api.getWeather(m.codProv, id5)
    }

    private fun List<UiItem>.sortedByName() =
        sortedWith(compareBy {
            when (it) {
                is UiItem.Prov -> it.p.nombre
                is UiItem.Mun  -> it.m.nombre
            }
        })
}