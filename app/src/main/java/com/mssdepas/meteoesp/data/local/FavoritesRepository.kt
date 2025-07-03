package com.mssdepas.meteoesp.data.local

import android.content.Context
import com.mssdepas.meteoesp.data.model.Municipio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val favoritesKey = "favorite_municipios"

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    init {
        _favorites.value = prefs.getStringSet(favoritesKey, emptySet()) ?: emptySet()
    }

    fun isFavorite(municipio: Municipio): Boolean {
        return _favorites.value.contains(municipio.codigoINE)
    }

    fun toggleFavorite(municipio: Municipio) {
        val currentFavorites = _favorites.value.toMutableSet()
        if (isFavorite(municipio)) {
            currentFavorites.remove(municipio.codigoINE)
        } else {
            currentFavorites.add(municipio.codigoINE)
        }
        _favorites.value = currentFavorites
        prefs.edit().putStringSet(favoritesKey, currentFavorites).apply()
    }

    fun getFavoriteMunicipios(allMunicipios: List<Municipio>): List<Municipio> {
        val favoriteIds = _favorites.value
        return allMunicipios.filter { favoriteIds.contains(it.codigoINE) }
    }
}