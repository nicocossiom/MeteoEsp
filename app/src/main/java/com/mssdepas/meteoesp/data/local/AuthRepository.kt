package com.mssdepas.meteoesp.data.local

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mssdepas.meteoesp.R
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            AppLogger.e("Error signing in with email", throwable = e)
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            AppLogger.e("Error creating user with email", throwable = e)
            Result.failure(e)
        }
    }

//    suspend fun signInWithGoogle(): Result<FirebaseUser> {
//        return try {
//            val googleIdOption = GetGoogleIdOption.Builder()
//                .setFilterByAuthorizedAccounts(false)
//                .setServerClientId(context.getString(R.string.default_web_client_id))
//                .build()
//
//            val request = GetCredentialRequest.Builder()
//                .addCredentialOption(googleIdOption)
//                .build()
//
//            val result = credentialManager.getCredential(
//                request = request,
//                context = context,
//            )
//
//            val googleIdTokenCredential = GoogleIdTokenCredential
//                .createFrom(result.credential.data)
//
//            val googleCredentials = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
//            val authResult = auth.signInWithCredential(googleCredentials).await()
//
//            Result.success(authResult.user!!)
//        } catch (e: GetCredentialException) {
//            AppLogger.e("Error with credential manager", throwable = e)
//            Result.failure(e)
//        } catch (e: GoogleIdTokenParsingException) {
//            AppLogger.e("Error parsing Google ID token", throwable = e)
//            Result.failure(e)
//        } catch (e: Exception) {
//            AppLogger.e("Error signing in with Google", throwable = e)
//            Result.failure(e)
//        }
//    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("Error sending password reset email", throwable = e)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error updating password", throwable = e)
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.updateEmail(newEmail).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error updating email", throwable = e)
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error sending email verification", throwable = e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            AppLogger.e("Error deleting user", throwable = e)
            Result.failure(e)
        }
    }
}