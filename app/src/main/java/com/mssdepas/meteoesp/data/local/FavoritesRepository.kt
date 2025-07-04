package com.mssdepas.meteoesp.data.local

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepository(context: Context) {

    private val localPrefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    private val favoritesKey = "favorite_municipios"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    init {
        // Load local favorites first
        _favorites.value = localPrefs.getStringSet(favoritesKey, emptySet()) ?: emptySet()

        // Sync with Firestore if user is authenticated
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                syncFromFirestore()
            }
        }
    }

    private fun syncFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e("Error syncing favorites from Firestore", throwable = error)
                    return@addSnapshotListener
                }

                val firestoreFavorites = snapshot?.documents?.mapNotNull {
                    it.getString("municipioId")
                }?.toSet() ?: emptySet()

                _favorites.value = firestoreFavorites
                saveToLocal(firestoreFavorites)
            }
    }

    fun isFavorite(municipio: Municipio): Boolean {
        return _favorites.value.contains(municipio.codigoINE)
    }

    fun toggleFavorite(municipio: Municipio) {
        val currentFavorites = _favorites.value.toMutableSet()
        val municipioId = municipio.codigoINE

        if (isFavorite(municipio)) {
            currentFavorites.remove(municipioId)
            removeFromFirestore(municipioId)
        } else {
            currentFavorites.add(municipioId)
            addToFirestore(municipio)
        }

        _favorites.value = currentFavorites
        saveToLocal(currentFavorites)
    }

    private fun saveToLocal(favorites: Set<String>) {
        localPrefs.edit().putStringSet(favoritesKey, favorites).apply()
    }

    private fun addToFirestore(municipio: Municipio) {
        val userId = auth.currentUser?.uid ?: return

        val favoriteData = mapOf(
            "municipioId" to municipio.codigoINE,
            "municipioName" to municipio.nombre,
            "provinciaId" to municipio.codProv,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(municipio.codigoINE)
            .set(favoriteData)
            .addOnFailureListener { exception ->
                AppLogger.e("Error adding favorite to Firestore", throwable = exception)
            }
    }

    private fun removeFromFirestore(municipioId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(municipioId)
            .delete()
            .addOnFailureListener { exception ->
                AppLogger.e("Error removing favorite from Firestore", throwable = exception)
            }
    }

    fun getFavoriteMunicipios(allMunicipios: List<Municipio>): List<Municipio> {
        val favoriteIds = _favorites.value
        return allMunicipios.filter { favoriteIds.contains(it.codigoINE) }
    }
}