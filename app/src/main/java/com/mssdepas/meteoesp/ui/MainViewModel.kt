package com.mssdepas.meteoesp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mssdepas.meteoesp.data.local.FavoritesRepository
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia
import com.mssdepas.meteoesp.data.local.LocationRepository
import com.mssdepas.meteoesp.data.remote.RetrofitInstance
import com.mssdepas.meteoesp.data.remote.WeatherResponse
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    private val _selectedProvincia = MutableStateFlow<Provincia?>(null)
    val selectedProvincia: StateFlow<Provincia?> = _selectedProvincia.asStateFlow()

    private val _selectedMunicipio = MutableStateFlow<Municipio?>(null)
    val selectedMunicipio: StateFlow<Municipio?> = _selectedMunicipio.asStateFlow()

    private val _provinciasFiltradas = MutableStateFlow<List<Provincia>>(emptyList())
    val provinciasFiltradas: StateFlow<List<Provincia>> = _provinciasFiltradas.asStateFlow()

    private val _municipiosFiltrados = MutableStateFlow<List<Municipio>>(emptyList())
    val municipiosFiltrados: StateFlow<List<Municipio>> = _municipiosFiltrados.asStateFlow()

    private val _showFavoritesManager = MutableStateFlow(false)
    val showFavoritesManager: StateFlow<Boolean> = _showFavoritesManager.asStateFlow()

    private val _favoriteMunicipios = MutableStateFlow<List<Municipio>>(emptyList())
    val favoriteMunicipios: StateFlow<List<Municipio>> = _favoriteMunicipios.asStateFlow()

    private val _selectedWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedWeather: StateFlow<WeatherResponse?> = _selectedWeather.asStateFlow()

    private val _currentLocationWeather = MutableStateFlow<WeatherResponse?>(null)
    val currentLocationWeather: StateFlow<WeatherResponse?> = _currentLocationWeather.asStateFlow()

    private val _isLoadingCurrentLocation = MutableStateFlow(false)
    val isLoadingCurrentLocation: StateFlow<Boolean> = _isLoadingCurrentLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _favoriteWeathers = MutableStateFlow<List<WeatherResponse>>(emptyList())
    val favoriteWeathers: StateFlow<List<WeatherResponse>> = _favoriteWeathers.asStateFlow()

    private val _selectedProvinciaWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedProvinciaWeather: StateFlow<WeatherResponse?> = _selectedProvinciaWeather.asStateFlow()

    private var lastQuery = ""

    init {
        viewModelScope.launch {
            loadInitialData()
        }
        observeFavorites()
    }

    fun onLocationPermissionGranted() {
        fetchCurrentLocationWeather()
    }

    fun retryLocationWeather() {
        fetchCurrentLocationWeather()
    }

    fun dismissLocationErrorDialog() {
        _locationError.value = null
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            combine(_municipios, favoritesRepository.favorites) { municipios, favIds ->
                Pair(municipios, favIds)
            }.collect { (municipios, _) ->
                fetchFavoriteWeathers(municipios)
                updateFavoriteMunicipios(municipios)
            }
        }
    }

    private fun updateFavoriteMunicipios(allMunicipios: List<Municipio>) {
        _favoriteMunicipios.value = favoritesRepository.getFavoriteMunicipios(allMunicipios)
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
            _provinciasFiltradas.value = provs

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
            updateFavoriteMunicipios(allMuns)
        } catch (e: Exception) {
            AppLogger.e("Error cargando provincias:\n${e.message}", throwable = e)
        }
    }

    private fun fetchCurrentLocationWeather() = viewModelScope.launch {
        _isLoadingCurrentLocation.value = true
        _locationError.value = null

        try {
            // Add timeout to prevent hanging
            val municipalityName = withTimeoutOrNull(15000L) { // 15 second timeout
                locationRepository.getCurrentMunicipalityName()
            }

            if (municipalityName != null) {
                try {
                    val municipio = getMunicipio(municipalityName)
                    val id5 = municipio.codigoINE.take(5)
                    _currentLocationWeather.value = api.getWeather(municipio.codProv, id5)
                    AppLogger.i("Successfully fetched weather for current location: ${municipio.nombre}")

                } catch (e: Exception) {
                    AppLogger.e("Error fetching weather for current location", throwable = e)
                    _locationError.value = "No se pudo obtener el tiempo para la ubicación encontrada '${municipalityName}'."
                }
            } else {
                AppLogger.w("Could not get municipality name from location.")
                _locationError.value = "No se pudo determinar tu ubicación. Verifica que el GPS esté activado y tengas conexión a internet."
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get current location weather", throwable = e)
            _locationError.value = "Tiempo agotado al obtener la ubicación. Verifica que el GPS esté activado."
        } finally {
            _isLoadingCurrentLocation.value = false
        }
    }

    private fun getMunicipio(municipalityName: String): Municipio {
        return _municipios.value.firstOrNull { it.nombre.equals(municipalityName, true) }
            ?: throw IllegalArgumentException("Municipio no encontrado: $municipalityName")
    }

    // Combo box methods
    fun filterProvincias(query: String) {
        _provinciasFiltradas.value = if (query.isBlank()) {
            _provincias.value
        } else {
            _provincias.value.filter { it.nombre.contains(query, ignoreCase = true) }
        }
    }

    fun selectProvincia(provincia: Provincia) {
        _selectedProvincia.value = provincia
        _selectedMunicipio.value = null
        _selectedProvinciaWeather.value = null
        updateMunicipiosFiltrados("")
        // Load weather for the capital or first municipality of the province
        loadWeatherForProvincia(provincia)
    }

    private fun loadWeatherForProvincia(provincia: Provincia) = viewModelScope.launch {
        try {
            // Get the first municipality of the province (usually the capital)
            val municipiosProvincia = _municipios.value.filter { it.codProv == provincia.id }
            val firstMunicipio = municipiosProvincia.firstOrNull()

            if (firstMunicipio != null) {
                val id5 = firstMunicipio.codigoINE.take(5)
                _selectedProvinciaWeather.value = api.getWeather(firstMunicipio.codProv, id5)
                AppLogger.i("Successfully fetched weather for province ${provincia.nombre}: ${firstMunicipio.nombre}")
            }
        } catch (e: Exception) {
            AppLogger.e("Error fetching weather for province ${provincia.nombre}", throwable = e)
        }
    }

    fun addProvinciaToFavorites() {
        val provincia = _selectedProvincia.value
        if (provincia != null) {
            // Add the first municipality of the province to favorites
            val municipiosProvincia = _municipios.value.filter { it.codProv == provincia.id }
            val firstMunicipio = municipiosProvincia.firstOrNull()
            if (firstMunicipio != null && !favoritesRepository.isFavorite(firstMunicipio)) {
                favoritesRepository.toggleFavorite(firstMunicipio)
            }
        }
    }

    fun filterMunicipios(query: String) {
        updateMunicipiosFiltrados(query)
    }

    private fun updateMunicipiosFiltrados(query: String) {
        val provinciaSeleccionada = _selectedProvincia.value
        if (provinciaSeleccionada != null) {
            val municipiosProvincia = _municipios.value.filter { it.codProv == provinciaSeleccionada.id }
            _municipiosFiltrados.value = if (query.isBlank()) {
                municipiosProvincia
            } else {
                municipiosProvincia.filter { it.nombre.contains(query, ignoreCase = true) }
            }
        } else {
            _municipiosFiltrados.value = emptyList()
        }
    }

    fun selectMunicipio(municipio: Municipio) {
        _selectedMunicipio.value = municipio
    }

    fun clearProvinciaSelection() {
        _selectedProvincia.value = null
        _selectedMunicipio.value = null
        _municipiosFiltrados.value = emptyList()
    }

    fun clearMunicipioSelection() {
        _selectedMunicipio.value = null
    }

    // Action buttons
    fun loadWeatherForSelected() = viewModelScope.launch {
        val municipio = _selectedMunicipio.value
        if (municipio != null) {
            loadWeather(municipio)
        }
    }

    fun addSelectedToFavorites() {
        val municipio = _selectedMunicipio.value
        if (municipio != null && !favoritesRepository.isFavorite(municipio)) {
            favoritesRepository.toggleFavorite(municipio)
        }
    }

    // Favorites management
    fun showFavoritesManager() {
        _showFavoritesManager.value = true
    }

    fun hideFavoritesManager() {
        _showFavoritesManager.value = false
    }

    fun removeFavorite(municipio: Municipio) {
        favoritesRepository.toggleFavorite(municipio)
    }

    fun moveFavoriteUp(municipio: Municipio) {
        // For simplicity, we'll just maintain the current order based on how they were added
        // A more sophisticated implementation would maintain a custom order
    }

    fun moveFavoriteDown(municipio: Municipio) {
        // For simplicity, we'll just maintain the current order based on how they were added
        // A more sophisticated implementation would maintain a custom order
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
}