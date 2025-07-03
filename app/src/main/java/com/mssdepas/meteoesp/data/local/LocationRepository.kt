package com.mssdepas.meteoesp.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.mssdepas.meteoesp.util.AppLogger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationRepository(private val context: Context) {

    // ─────────────────────────── PUBLIC API ───────────────────────────

    /**
     * Return municipality or *null* if permission is absent **or** location cannot be resolved.
     * Suitable when the Activity already requested *ACCESS_FINE_LOCATION*.
     */
    suspend fun getCurrentMunicipalityName(): String? {
        if (!hasFineLocationPermission(context)) {
            return null
        }

        val location = try {
            getFreshLocation()
        } catch (se: SecurityException) {
            AppLogger.e("Lost location permission during getFreshLocation", throwable = se)
            return null
        }

        return location?.let { reverseGeocode(it) }
    }

    /**
     * Convenience variant that **asks for the permission once** (standard dialog). Call it from
     * an Activity if you prefer not to deal with permission boilerplate yourself.
     */
        suspend fun getCurrentMunicipalityNameWithPermission(activity: ComponentActivity): String? {
        val granted = ensureFineLocationPermission(activity)
        return if (granted) getCurrentMunicipalityName() else null
    }

    // ────────────────────────── Internals ──────────────────────────

    private val geocoder by lazy { Geocoder(context, Locale.getDefault()) }
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun getFreshLocation(): Location? = withContext(Dispatchers.Default) {
        if (!hasFineLocationPermission(context)) return@withContext null

        fusedClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()
            ?.takeIf(::isRecent)
            ?: fusedClient.lastLocation.await()?.takeIf(::isRecent)
    }

    /** Converts a Location into a municipality name (or null). */
    private suspend fun reverseGeocode(location: Location): String? =
        withContext(Dispatchers.IO) {
            try {
                val firstAddress: Address? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // ─── New async API (API 33+) ───
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                /* maxResults = */ 1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        cont.resume(addresses.firstOrNull())
                                    }

                                    override fun onError(errorMessage: String?) {
                                        AppLogger.w("Geocoder error → $errorMessage")
                                        cont.resume(null)
                                    }
                                }
                            )
                        }
                    } else {
                        // ─── Legacy synchronous call ───
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            /* maxResults = */ 1
                        )?.firstOrNull()
                    }

                // Pick the best text field for a municipality / locality
                firstAddress?.locality
                    ?: firstAddress?.subAdminArea
                    ?: firstAddress?.subLocality
            } catch (e: Exception) {
                AppLogger.e("Reverse-geocoding failed", throwable = e)
                null
            }
        }

    private suspend fun ensureFineLocationPermission(activity: ComponentActivity): Boolean =
        withContext(Dispatchers.Main) {
            if (hasFineLocationPermission(context)) return@withContext true

            suspendCancellableCoroutine<Boolean> { cont ->
                var launcher: ActivityResultLauncher<String>? = null

                launcher = activity.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    cont.resume(granted)
                    launcher?.unregister()
                }

                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                cont.invokeOnCancellation { launcher?.unregister() }
            }
        }



    private fun isRecent(loc: Location): Boolean =
        loc.time >= System.currentTimeMillis() - FIVE_MINUTES_MS

    companion object {
        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

        public fun hasFineLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
