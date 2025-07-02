package com.mssdepas.meteoesp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mssdepas.meteoesp.data.local.FavoritesRepository
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia
import com.mssdepas.meteoesp.data.remote.LocationRepository
import com.mssdepas.meteoesp.data.remote.RetrofitInstance
import com.mssdepas.meteoesp.data.remote.WeatherResponse
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchMode { PROVINCIA, MUNICIPIO, AMBOS }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface UiItem {                        // union type for the list
        data class Prov(val p: Provincia) : UiItem
        data class Mun(val m: Municipio, val isFavorite: Boolean) : UiItem
    }

    private val api = RetrofitInstance.api
    private val locationRepository = LocationRepository(application)
    private val favoritesRepository = FavoritesRepository(application)

    private val _provincias = MutableStateFlow<List<Provincia>>(emptyList())
    private val _municipios = MutableStateFlow<List<Municipio>>(emptyList())

    private val _uiItems = MutableStateFlow<List<UiItem>>(emptyList())
    val uiItems: StateFlow<List<UiItem>> = _uiItems.asStateFlow()

    private val _selectedWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedWeather: StateFlow<WeatherResponse?> = _selectedWeather.asStateFlow()

    private val _currentLocationWeather = MutableStateFlow<WeatherResponse?>(null)
    val currentLocationWeather: StateFlow<WeatherResponse?> = _currentLocationWeather.asStateFlow()

    private val _favoriteWeathers = MutableStateFlow<List<WeatherResponse>>(emptyList())
    val favoriteWeathers: StateFlow<List<WeatherResponse>> = _favoriteWeathers.asStateFlow()

    private val _searchMode = MutableStateFlow(SearchMode.AMBOS)
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    private var _selectedProvincia: Provincia? = null

    private var lastQuery = ""

    init {
        viewModelScope.launch {
            loadInitialData()
            fetchCurrentLocationWeather()
        }
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            combine(_municipios, favoritesRepository.favorites) { municipios, favIds ->
                Pair(municipios, favIds)
            }.collect { (municipios, _) ->
                updateUiItems()
                fetchFavoriteWeathers(municipios)
            }
        }
    }

    private suspend fun fetchFavoriteWeathers(allMunicipios: List<Municipio>) {
        val favoriteMunicipios = favoritesRepository.getFavoriteMunicipios(allMunicipios)
        val weathers = favoriteMunicipios.mapNotNull { m ->
            try {
                val id5 = m.codigoINE.take(5)
                api.getWeather(m.codProv, id5)
            } catch (e: Exception) {
                AppLogger.e("Error fetching weather for favorite ${m.nombre}", throwable = e)
                null
            }
        }
        _favoriteWeathers.value = weathers
    }

    private suspend fun loadInitialData() {
        try {
            val provs = api.getProvincias().provincias
            _provincias.value = provs
            updateUiItems()

            val allMuns = mutableListOf<Municipio>()
            provs.forEach { prov ->
                try {
                    val muns = api.getMunicipios(prov.id).municipios
                    allMuns.addAll(muns)
                } catch (e: Exception) {
                    AppLogger.e("Error cargando municipios de ${prov.nombre}:\n${e.message}", throwable = e)
                }
            }
            _municipios.value = allMuns
            // Initial fetch, will be re-triggered by observer if needed
            fetchFavoriteWeathers(allMuns)
            updateUiItems() // Update again with municipios
        } catch (e: Exception) {
            AppLogger.e("Error cargando provincias:\n${e.message}", throwable = e)
        }
    }

    private fun fetchCurrentLocationWeather() = viewModelScope.launch {
        val municipalityName = locationRepository.getCurrentMunicipalityName()
        if (municipalityName != null) {
            try {
                val munResponse = api.getMunicipio(municipalityName)
                munResponse.municipios.firstOrNull()?.let {
                    val id5 = it.codigoINE.take(5)
                    _currentLocationWeather.value = api.getWeather(it.codProv, id5)
                }
            } catch (e: Exception) {
                AppLogger.e("Error fetching weather for current location", throwable = e)
            }
        }
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        _selectedProvincia = null // Reset provincia selection when changing mode
        updateUiItems()
    }

    fun onProvinciaClicked(p: Provincia) {
        _selectedProvincia = p
        updateUiItems()
    }

    fun onBackToProvincias() {
        _selectedProvincia = null
        updateUiItems()
    }

    private fun updateUiItems() {
        val provItems = _provincias.value
            .filter { it.nombre.contains(lastQuery, true) }
            .map { UiItem.Prov(it) }

        val munItems = _municipios.value
            .filter { it.nombre.contains(lastQuery, true) }
            .map { UiItem.Mun(it, favoritesRepository.isFavorite(it)) }

        _uiItems.value = when {
            _selectedProvincia != null -> {
                _municipios.value
                    .filter { it.codProv == _selectedProvincia!!.id && it.nombre.contains(lastQuery, true) }
                    .map { UiItem.Mun(it, favoritesRepository.isFavorite(it)) }
            }
            else -> when (_searchMode.value) {
                SearchMode.PROVINCIA -> provItems
                SearchMode.MUNICIPIO -> munItems
                SearchMode.AMBOS -> (provItems + munItems).sortedByName()
            }
        }
    }

    /** Called from UI when query changes */
    fun filter(query: String) {
        lastQuery = query
        updateUiItems()
    }

    /** Called from UI when user clicks a municipio */
    fun loadWeather(m: Municipio) = viewModelScope.launch {
        val id5 = m.codigoINE.take(5)           // first 5 digits
        _selectedWeather.value =
            api.getWeather(m.codProv, id5)
    }

    fun dismissWeather() {
        _selectedWeather.value = null
    }

    fun toggleFavorite(m: Municipio) {
        favoritesRepository.toggleFavorite(m)
    }

    private fun List<UiItem>.sortedByName() =
        sortedWith(compareBy {
            when (it) {
                is UiItem.Prov -> it.p.nombre
                is UiItem.Mun  -> it.m.nombre
            }
        })
}