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
    val dateType: String = "EXIF",
    val sortBy: String = "NAME",
    val sortAscending: Boolean = true,
    val isGeneratingCSV: Boolean = false,
    val csvContent: String? = null
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _isGeneratingCSV = MutableStateFlow(false)
    private val _csvContent = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.selectedDirectory,
        preferencesRepository.timeThreshold,
        preferencesRepository.groupByDate,
        preferencesRepository.dateType,
        preferencesRepository.sortBy,
        preferencesRepository.sortAscending,
        _isGeneratingCSV,
        _csvContent
    ) { directory, threshold, groupByDate, dateType, sortBy, sortAscending, isGeneratingCSV, csvContent ->
        SettingsUiState(
            selectedDirectory = directory,
            timeThreshold = threshold,
            groupByDate = groupByDate,
            dateType = dateType,
            sortBy = sortBy,
            sortAscending = sortAscending,
            isGeneratingCSV = isGeneratingCSV,
            csvContent = csvContent
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
    
    suspend fun setDateType(dateType: String) {
        preferencesRepository.setDateType(dateType)
    }
    
    suspend fun setSortBy(sortBy: String) {
        preferencesRepository.setSortBy(sortBy)
    }
    
    suspend fun setSortAscending(ascending: Boolean) {
        preferencesRepository.setSortAscending(ascending)
    }
    
    fun generateCSVContent(context: Context, database: com.hdya.imagetagging.data.AppDatabase) {
        viewModelScope.launch {
            try {
                _isGeneratingCSV.value = true
                
                // Get all file labels and labels from database
                val fileLabels = database.fileLabelDao().getAllFileLabels()
                val labels = database.labelDao().getAllLabels()
                val labelMap = labels.associateBy { it.id }
                
                val csvContent = StringBuilder()
                csvContent.append("File Path,Labels\n")
                
                // Group file labels by file path
                val groupedByFile = fileLabels.groupBy { it.filePath }
                
                for ((filePath, fileLabelsForFile) in groupedByFile) {
                    val labelNames = fileLabelsForFile.mapNotNull { fileLabel ->
                        labelMap[fileLabel.labelId]?.name
                    }.joinToString(";")
                    
                    // Escape commas and quotes in file path
                    val escapedPath = if (filePath.contains(",") || filePath.contains("\"")) {
                        "\"${filePath.replace("\"", "\"\"")}\"" 
                    } else {
                        filePath
                    }
                    
                    csvContent.append("$escapedPath,$labelNames\n")
                }
                
                _csvContent.value = csvContent.toString()
                
            } catch (e: Exception) {
                // Handle error
                _csvContent.value = "Error generating CSV: ${e.message}"
            } finally {
                _isGeneratingCSV.value = false
            }
        }
    }
    
    fun clearCSVContent() {
        _csvContent.value = null
    }
}