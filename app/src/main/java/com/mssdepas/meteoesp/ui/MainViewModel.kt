package com.mssdepas.meteoesp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.mssdepas.meteoesp.data.local.FavoritesRepository
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia
import com.mssdepas.meteoesp.data.local.LocationRepository
import com.mssdepas.meteoesp.data.local.UserPreferenceRepository
import com.mssdepas.meteoesp.data.remote.RetrofitInstance
import com.mssdepas.meteoesp.data.remote.WeatherResponse
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitInstance.api
    private val locationRepository = LocationRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val userPreferencesRepository = UserPreferenceRepository(application)

    private val _provincias = MutableStateFlow<List<Provincia>>(emptyList())
    private val _municipios = MutableStateFlow<List<Municipio>>(emptyList())
    val provincias: StateFlow<List<Provincia>> = _provincias.asStateFlow()
    val municipios: StateFlow<List<Municipio>> = _municipios.asStateFlow()

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

    data class FavoriteItem(val municipio: Municipio, val provinciaNombre: String)
    private val _favoriteItems = MutableStateFlow<List<FavoriteItem>>(emptyList())
    val favoriteItems: StateFlow<List<FavoriteItem>> = _favoriteItems.asStateFlow()

    private val _favoritesLastUpdated = MutableStateFlow<Long>(0L)
    val favoritesLastUpdated: StateFlow<Long> = _favoritesLastUpdated.asStateFlow()

    private val _selectedWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedWeather: StateFlow<WeatherResponse?> = _selectedWeather.asStateFlow()

    private val _currentLocationWeather = MutableStateFlow<WeatherResponse?>(null)
    val currentLocationWeather: StateFlow<WeatherResponse?> = _currentLocationWeather.asStateFlow()

    private val _isLoadingCurrentLocation = MutableStateFlow(false)
    val isLoadingCurrentLocation: StateFlow<Boolean> = _isLoadingCurrentLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _isCurrentLocationFromCache = MutableStateFlow(false)
    val isCurrentLocationFromCache: StateFlow<Boolean> = _isCurrentLocationFromCache.asStateFlow()

    private var initialLocationFetched = false

    private val _selectedMunicipioWeather = MutableStateFlow<WeatherResponse?>(null)
    val selectedMunicipioWeather: StateFlow<WeatherResponse?> = _selectedMunicipioWeather.asStateFlow()

    // For map screen
    private val _showFavoritesOnMap = MutableStateFlow(false)
    val showFavoritesOnMap: StateFlow<Boolean> = _showFavoritesOnMap.asStateFlow()

    private val _mapCameraPosition = MutableStateFlow<CameraPosition?>(null)
    val mapCameraPosition: StateFlow<CameraPosition?> = _mapCameraPosition.asStateFlow()

    val mapMarkers: StateFlow<List<Municipio>> = combine(
        selectedMunicipio,
        favoriteMunicipios,
        showFavoritesOnMap
    ) { selected, favorites, showFavorites ->
        val markers = mutableSetOf<Municipio>()
        selected?.let { markers.add(it) }
        if (showFavorites) {
            markers.addAll(favorites)
        }
        markers.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        viewModelScope.launch {
            loadInitialData()
        }
        observeFavorites()
    }

    fun toggleShowFavoritesOnMap() {
        _showFavoritesOnMap.value = !_showFavoritesOnMap.value
    }

    fun onLocationPermissionGranted() {
        if (!initialLocationFetched) {
            fetchCurrentLocationWeather()
        }
    }

    fun retryLocationWeather() {
        fetchCurrentLocationWeather()
    }

    fun dismissLocationErrorDialog() {
        _locationError.value = null
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            combine(_municipios, favoritesRepository.favorites, _provincias) { municipios, favorites, provincias ->
                _favoritesLastUpdated.value = System.currentTimeMillis()
                Triple(municipios, favorites, provincias)
            }.collect { (municipios, _, provincias) ->
                updateFavoriteItems(municipios, provincias)
                updateFavoriteMunicipios(municipios)
            }
        }
    }

    private fun updateFavoriteItems(allMunicipios: List<Municipio>, allProvincias: List<Provincia>) {
        val favoriteMunicipios = favoritesRepository.getFavoriteMunicipios(allMunicipios)
        val provinciaMap = allProvincias.associateBy { it.id }
        _favoriteItems.value = favoriteMunicipios.map { municipio ->
            FavoriteItem(
                municipio = municipio,
                provinciaNombre = provinciaMap[municipio.codProv]?.nombre ?: ""
            )
        }
    }

    private fun updateFavoriteMunicipios(allMunicipios: List<Municipio>) {
        _favoriteMunicipios.value = favoritesRepository.getFavoriteMunicipios(allMunicipios)
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
            updateFavoriteMunicipios(allMuns)
        } catch (e: Exception) {
            AppLogger.e("Error cargando provincias:\n${e.message}", throwable = e)
        }
    }

    private fun fetchCurrentLocationWeather() = viewModelScope.launch {
        _isLoadingCurrentLocation.value = true
        _locationError.value = null
        _isCurrentLocationFromCache.value = false
        initialLocationFetched = true

        try {
            val municipalityName = withTimeoutOrNull(15000L) { // 15 second timeout
                locationRepository.getCurrentMunicipalityName()
            }

            if (municipalityName != null) {
                try {
                    val municipio = getMunicipioByName(municipalityName)
                    val id5 = municipio.codigoINE.take(5)
                    _currentLocationWeather.value = api.getWeather(municipio.codProv, id5)
                    // Save successful location
                    userPreferencesRepository.lastKnownMunicipalityId = municipio.codigoINE
                    AppLogger.i("Successfully fetched weather for current location: ${municipio.nombre}")

                } catch (e: Exception) {
                    AppLogger.e("Error fetching weather for current location", throwable = e)
                    loadWeatherFromCacheOrShowError("No se pudo obtener el tiempo para la ubicación encontrada '${municipalityName}'.")
                }
            } else {
                AppLogger.w("Could not get municipality name from location.")
                loadWeatherFromCacheOrShowError("No se pudo determinar tu ubicación. Verifica que el GPS esté activado y tengas conexión a internet.")
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to get current location weather", throwable = e)
            loadWeatherFromCacheOrShowError("Tiempo agotado al obtener la ubicación. Verifica que el GPS esté activado.")
        } finally {
            _isLoadingCurrentLocation.value = false
        }
    }

    private fun loadWeatherFromCacheOrShowError(errorMsg: String) = viewModelScope.launch {
        val cachedId = userPreferencesRepository.lastKnownMunicipalityId
        if (cachedId != null) {
            try {
                val cachedMunicipio = getMunicipioByIne(cachedId)
                loadWeather(cachedMunicipio, updateCurrent = true)
                _isCurrentLocationFromCache.value = true
                AppLogger.i("Loaded weather from cached location: ${cachedMunicipio.nombre}")
            } catch (e: Exception) {
                _locationError.value = errorMsg
                AppLogger.e("Failed to load weather from cached ID: $cachedId", throwable = e)
            }
        } else {
            _locationError.value = errorMsg
        }
    }

    private fun getMunicipioByName(municipalityName: String): Municipio {
        return _municipios.value.firstOrNull { it.nombre.equals(municipalityName, true) }
            ?: throw IllegalArgumentException("Municipio no encontrado: $municipalityName")
    }

    private fun getMunicipioByIne(codigoIne: String): Municipio {
        return _municipios.value.firstOrNull { it.codigoINE == codigoIne }
            ?: throw IllegalArgumentException("Municipio no encontrado por INE: $codigoIne")
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
        _selectedMunicipioWeather.value = null
        updateMunicipiosFiltrados("")
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
        loadWeatherForMunicipio(municipio)

        val lat = municipio.latitud.replace(',', '.').toDoubleOrNull()
        val lon = municipio.longitud.replace(',', '.').toDoubleOrNull()
        if (lat != null && lon != null) {
            _mapCameraPosition.value = CameraPosition.fromLatLngZoom(LatLng(lat, lon), 12f)
        }
    }

    private fun loadWeatherForMunicipio(municipio: Municipio) = viewModelScope.launch {
        _selectedMunicipioWeather.value = null // Show loading maybe? For now just clear.
        try {
            val id5 = municipio.codigoINE.take(5)
            _selectedMunicipioWeather.value = api.getWeather(municipio.codProv, id5)
        } catch (e: Exception) {
            AppLogger.e("Error fetching weather for selected municipio ${municipio.nombre}", throwable = e)
        }
    }

    fun clearProvinciaSelection() {
        _selectedProvincia.value = null
        _selectedMunicipio.value = null
        _municipiosFiltrados.value = emptyList()
        _selectedMunicipioWeather.value = null
        _mapCameraPosition.value = null
    }

    fun clearMunicipioSelection() {
        _selectedMunicipio.value = null
        _selectedMunicipioWeather.value = null
        _mapCameraPosition.value = null
    }

    // Action buttons
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


    /** Called from UI when user clicks a municipio */
    fun loadWeather(m: Municipio, updateCurrent: Boolean = false) = viewModelScope.launch {
        val id5 = m.codigoINE.take(5)           // first 5 digits
        val weather = api.getWeather(m.codProv, id5)
        if (updateCurrent) {
            _currentLocationWeather.value = weather
        } else {
            _selectedWeather.value = weather
        }
    }

    fun dismissWeather() {
        _selectedWeather.value = null
    }

    fun getWeatherForLatLng(lat: Double, lon: Double) = viewModelScope.launch {
        _selectedWeather.value = null
        val closestMunicipio = findClosestMunicipio(lat, lon)
        if (closestMunicipio != null) {
            loadWeather(closestMunicipio)
        } else {
            // Handle case where no municipio is found (e.g., click in the ocean)
            AppLogger.w("No municipio found for coordinates: lat=$lat, lon=$lon")
        }
    }

    private fun findClosestMunicipio(lat: Double, lon: Double): Municipio? {
        var closestMunicipio: Municipio? = null
        var minDistance = Double.MAX_VALUE

        // This is a simplified search. For a large number of municipalities,
        // a spatial index (like a k-d tree or quadtree) would be more efficient.
        _municipios.value.forEach { municipio ->
            // The AEMET API provides lat/lon as strings, so we need to parse them.
            // A production app should handle potential parsing errors gracefully.
            val munLat = municipio.latitud.replace(',', '.').toDoubleOrNull()
            val munLon = municipio.longitud.replace(',', '.').toDoubleOrNull()

            if (munLat != null && munLon != null) {
                val distance = haversineDistance(lat, lon, munLat, munLon)
                if (distance < minDistance) {
                    minDistance = distance
                    closestMunicipio = municipio
                }
            }
        }
        // We can set a threshold to avoid picking a municipality that is too far away.
        // For example, if (minDistance < 50) return closestMunicipio
        return closestMunicipio
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius of Earth in kilometers

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
}