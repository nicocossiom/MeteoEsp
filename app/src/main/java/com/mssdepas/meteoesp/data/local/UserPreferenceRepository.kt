package com.mssdepas.meteoesp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferenceRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastKnownMunicipalityId: String?
        get() = prefs.getString(KEY_LAST_MUNICIPALITY_ID, null)
        set(value) {
            prefs.edit { putString(KEY_LAST_MUNICIPALITY_ID, value) }
        }

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_LAST_MUNICIPALITY_ID = "last_known_municipality_id"
    }
}