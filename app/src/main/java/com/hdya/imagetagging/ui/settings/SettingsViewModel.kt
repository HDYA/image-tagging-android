package com.hdya.imagetagging.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdya.imagetagging.data.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedDirectory: String? = null,
    val timeThreshold: Int = 3600,
    val groupByDate: Boolean = false,
    val isExporting: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _isExporting = MutableStateFlow(false)
    
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.selectedDirectory,
        preferencesRepository.timeThreshold,
        preferencesRepository.groupByDate,
        _isExporting
    ) { directory, threshold, groupByDate, isExporting ->
        SettingsUiState(
            selectedDirectory = directory,
            timeThreshold = threshold,
            groupByDate = groupByDate,
            isExporting = isExporting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
    
    suspend fun setSelectedDirectory(directory: String) {
        preferencesRepository.setSelectedDirectory(directory)
    }
    
    suspend fun setTimeThreshold(threshold: Int) {
        preferencesRepository.setTimeThreshold(threshold)
    }
    
    suspend fun setGroupByDate(enabled: Boolean) {
        preferencesRepository.setGroupByDate(enabled)
    }
    
    fun exportCSV(context: Context) {
        viewModelScope.launch {
            try {
                _isExporting.value = true
                // TODO: Implement actual CSV export with file labels from database
                // For now, create an empty CSV file
                kotlinx.coroutines.delay(1000) // Simulate work
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isExporting.value = false
            }
        }
    }
}