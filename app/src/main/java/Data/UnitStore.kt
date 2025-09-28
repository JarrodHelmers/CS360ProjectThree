package com.example.weight_trackerapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.weight_trackerapp.util.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Give DataStore a unique name
private val Context.unitDataStore by preferencesDataStore("unit_prefs")

// Key we’ll use to save the choice
private val KEY_UNIT = stringPreferencesKey("unit_system")

/**
 * Simple wrapper to save and load the unit choice (kg or lb).
 * Works just like our PinStore and WeightStore.
 */
class UnitStore(private val context: Context) {
    // Every time this is collected, we get the current unit setting.
    val unitFlow: Flow<UnitSystem> = context.unitDataStore.data.map { prefs ->
        when (prefs[KEY_UNIT]) {
            "LB" -> UnitSystem.LB
            else -> UnitSystem.KG
        }
    }

    // Save a new choice into DataStore.
    suspend fun setUnit(unit: UnitSystem) {
        context.unitDataStore.edit { prefs ->
            prefs[KEY_UNIT] = when (unit) {
                UnitSystem.KG -> "KG"
                UnitSystem.LB -> "LB"
            }
        }
    }
}