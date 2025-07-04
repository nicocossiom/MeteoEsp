package com.mssdepas.meteoesp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.mssdepas.meteoesp.data.local.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                _authState.value = if (user != null) {
                    AuthState.Authenticated(user)
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.signInWithEmailAndPassword(email, password)
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.createUserWithEmailAndPassword(email, password)
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

//    fun signInWithGoogle() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _errorMessage.value = null
//
//            authRepository.signInWithGoogle()
//                .onFailure { exception ->
//                    _errorMessage.value = getErrorMessage(exception)
//                }
//
//            _isLoading.value = false
//        }
//    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _errorMessage.value = "Se ha enviado un enlace de recuperación a tu email"
                }
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.updatePassword(newPassword)
                .onSuccess {
                    _errorMessage.value = "Contraseña actualizada correctamente"
                }
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.updateEmail(newEmail)
                .onSuccess {
                    _errorMessage.value = "Email actualizado correctamente"
                }
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.sendEmailVerification()
                .onSuccess {
                    _errorMessage.value = "Se ha enviado un email de verificación"
                }
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.deleteUser()
                .onFailure { exception ->
                    _errorMessage.value = getErrorMessage(exception)
                }

            _isLoading.value = false
        }
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("password") == true -> "Contraseña incorrecta"
            exception.message?.contains("email") == true -> "Email no válido"
            exception.message?.contains("user-not-found") == true -> "Usuario no encontrado"
            exception.message?.contains("email-already-in-use") == true -> "El email ya está en uso"
            exception.message?.contains("weak-password") == true -> "La contraseña es muy débil"
            exception.message?.contains("network") == true -> "Error de conexión"
            exception.message?.contains("canceled") == true -> "Inicio de sesión cancelado"
            exception.message?.contains("credential") == true -> "Error de credenciales"
            else -> "Error de autenticación: ${exception.message}"
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}