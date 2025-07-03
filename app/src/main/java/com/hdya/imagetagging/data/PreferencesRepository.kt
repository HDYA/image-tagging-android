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
        val SORT_BY = stringPreferencesKey("sort_by")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val PAGE_SIZE = intPreferencesKey("page_size")
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
    
    val sortBy: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SORT_BY] ?: "NAME" // Default to sort by name
        }
    
    val sortAscending: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SORT_ASCENDING] ?: true // Default to ascending
        }
    
    val pageSize: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PAGE_SIZE] ?: 150 // Default page size
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
    
    suspend fun setSortBy(sortBy: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_BY] = sortBy
        }
    }
    
    suspend fun setSortAscending(ascending: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ASCENDING] = ascending
        }
    }
    
    suspend fun setPageSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PAGE_SIZE] = size
        }
    }
}