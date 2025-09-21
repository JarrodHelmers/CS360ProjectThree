package com.example.weight_trackerapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// one datastore, dedicated to the PIN
private val Context.pinDataStore by preferencesDataStore(name = "pin_store")

class PinStore(private val context: Context) {
    private val KEY = stringPreferencesKey("pin")

    // read current pin (null if not set)
    val pinFlow: Flow<String?> = context.pinDataStore.data.map { prefs ->
        prefs[KEY]
    }

    // save (or update) pin
    suspend fun setPin(pin: String) {
        context.pinDataStore.edit { prefs -> prefs[KEY] = pin }
    }

    // clear pin (not used yet)
    suspend fun clearPin() {
        context.pinDataStore.edit { it.remove(KEY) }
    }
}