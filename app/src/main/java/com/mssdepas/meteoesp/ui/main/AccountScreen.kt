package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mssdepas.meteoesp.ui.AuthState
import com.mssdepas.meteoesp.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    var showChangePassword by remember { mutableStateOf(false) }
    var showChangeEmail by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { authViewModel.clearError() },
            title = { Text("Información") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { authViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    if (authState is AuthState.Authenticated) {
        val user = (authState as AuthState.Authenticated).user

        if (showChangePassword) {
            ChangePasswordDialog(
                onDismiss = { showChangePassword = false },
                onChangePassword = { newPassword ->
                    authViewModel.updatePassword(newPassword)
                    showChangePassword = false
                },
                isLoading = isLoading
            )
        }

        if (showChangeEmail) {
            ChangeEmailDialog(
                currentEmail = user.email ?: "",
                onDismiss = { showChangeEmail = false },
                onChangeEmail = { newEmail ->
                    authViewModel.updateEmail(newEmail)
                    showChangeEmail = false
                },
                isLoading = isLoading
            )
        }

        if (showDeleteAccount) {
            DeleteAccountDialog(
                onDismiss = { showDeleteAccount = false },
                onDeleteAccount = {
                    authViewModel.deleteAccount()
                    showDeleteAccount = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gestión de cuenta") })
        }
    ) { padding ->
        if (authState is AuthState.Authenticated) {
            val user = (authState as AuthState.Authenticated).user
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // User Info
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Información Personal",
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Text("Email: ${user.email ?: "No disponible"}")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Text("Proveedor: ${if (user.providerData.any { it.providerId == "google.com" }) "Google" else "Email"}")
                    }

                    if (!user.isEmailVerified && user.email != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Email no verificado",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = { authViewModel.sendEmailVerification() },
                                enabled = !isLoading
                            ) {
                                Text("Verificar")
                            }
                        }
                    }
                }

                // Account Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Configuración de Cuenta",
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()

                    if (user.providerData.any { it.providerId == "password" }) {
                        Button(
                            onClick = { showChangePassword = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("Cambiar Contraseña")
                        }
                    }

                    Button(
                        onClick = { showChangeEmail = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Cambiar Email")
                    }

                    Button(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Cerrar Sesión")
                    }

                    OutlinedButton(
                        onClick = { showDeleteAccount = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar Cuenta")
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (String) -> Unit,
    isLoading: Boolean
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword == confirmPassword
    val canChange = newPassword.isNotBlank() && confirmPassword.isNotBlank() &&
            passwordsMatch && newPassword.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading,
                    supportingText = {
                        if (newPassword.isNotBlank() && newPassword.length < 6) {
                            Text("Mínimo 6 caracteres")
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Nueva Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotBlank() && !passwordsMatch) {
                            Text("Las contraseñas no coinciden")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canChange) {
                        onChangePassword(newPassword)
                    }
                },
                enabled = !isLoading && canChange
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Cambiar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onChangeEmail: (String) -> Unit,
    isLoading: Boolean
) {
    var newEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Email actual: $currentEmail")
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Nuevo Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newEmail.isNotBlank() && newEmail != currentEmail) {
                        onChangeEmail(newEmail)
                    }
                },
                enabled = !isLoading && newEmail.isNotBlank() && newEmail != currentEmail
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Cambiar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Cuenta") },
        text = {
            Text("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer y perderás todos tus datos.")
        },
        confirmButton = {
            TextButton(
                onClick = onDeleteAccount,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
