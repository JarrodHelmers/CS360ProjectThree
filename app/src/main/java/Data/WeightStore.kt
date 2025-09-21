package com.example.weight_trackerapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.weight_trackerapp.WeightEntry

// 1) One DataStore instance tied to Context
private val Context.dataStore by preferencesDataStore(name = "weights_store")

class WeightStore(private val context: Context) {
    private val gson = Gson()
    private val KEY = stringPreferencesKey("weights_json")

    // 2) Read: Flow of the whole list
    val weights: Flow<List<WeightEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY] ?: "[]"
        val type = object : TypeToken<List<WeightEntry>>() {}.type
        gson.fromJson<List<WeightEntry>>(json, type) ?: emptyList()
    }

    // 3) Write: add an entry (newest first)
    suspend fun add(entry: WeightEntry) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs).toMutableList()
            current.add(0, entry)
            prefs[KEY] = gson.toJson(current)
        }
    }

    // 4) Write: delete by id
    suspend fun delete(entryId: Long) {
        context.dataStore.edit { prefs ->
            val next = decode(prefs).filterNot { it.id == entryId }
            prefs[KEY] = gson.toJson(next)
        }
    }

    private fun decode(prefs: Preferences): List<WeightEntry> {
        val json = prefs[KEY] ?: "[]"
        val type = object : TypeToken<List<WeightEntry>>() {}.type
        return gson.fromJson<List<WeightEntry>>(json, type) ?: emptyList()
    }
}
