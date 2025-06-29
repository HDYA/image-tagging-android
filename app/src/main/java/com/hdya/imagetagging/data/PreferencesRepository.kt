package com.hdya.imagetagging.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    
    companion object {
        val SELECTED_DIRECTORY = stringPreferencesKey("selected_directory")
        val TIME_THRESHOLD = intPreferencesKey("time_threshold")
        val GROUP_BY_DATE = booleanPreferencesKey("group_by_date")
        val DATE_TYPE = stringPreferencesKey("date_type")
    }
    
    val selectedDirectory: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_DIRECTORY]
        }
    
    val timeThreshold: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[TIME_THRESHOLD] ?: 3600 // Default 1 hour
        }
    
    val groupByDate: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[GROUP_BY_DATE] ?: false
        }
    
    val dateType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DATE_TYPE] ?: "EXIF" // Default to EXIF taken time
        }
    
    suspend fun setSelectedDirectory(directory: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_DIRECTORY] = directory
        }
    }
    
    suspend fun setTimeThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[TIME_THRESHOLD] = threshold
        }
    }
    
    suspend fun setGroupByDate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GROUP_BY_DATE] = enabled
        }
    }
    
    suspend fun setDateType(dateType: String) {
        context.dataStore.edit { preferences ->
            preferences[DATE_TYPE] = dateType
        }
    }
}