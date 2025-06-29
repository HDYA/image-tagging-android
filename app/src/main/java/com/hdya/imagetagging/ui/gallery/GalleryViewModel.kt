package com.hdya.imagetagging.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdya.imagetagging.data.*
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.utils.FileUtils
import com.hdya.imagetagging.utils.MediaFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class GalleryUiState(
    val selectedDirectory: String? = null,
    val files: List<MediaFile> = emptyList(),
    val groupedFiles: Map<Int, List<MediaFile>> = emptyMap(),
    val groupByDate: Boolean = false,
    val timeThreshold: Int = 3600,
    val dateType: String = "EXIF",
    val availableLabels: List<Label> = emptyList(),
    val fileLabels: Map<String, List<Label>> = emptyMap(),
    val isLoading: Boolean = false
)

class GalleryViewModel(
    private val database: AppDatabase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    init {
        // Observe preferences changes
        viewModelScope.launch {
            combine(
                preferencesRepository.selectedDirectory,
                preferencesRepository.groupByDate,
                preferencesRepository.timeThreshold,
                preferencesRepository.dateType
            ) { directory, groupByDate, threshold, dateType ->
                _uiState.value = _uiState.value.copy(
                    selectedDirectory = directory,
                    groupByDate = groupByDate,
                    timeThreshold = threshold,
                    dateType = dateType
                )
                if (directory != null) {
                    loadFiles()
                }
            }.collect()
        }
        
        // Load available labels
        loadAvailableLabels()
    }
    
    fun loadFiles() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.selectedDirectory == null) return@launch
            
            _uiState.value = currentState.copy(isLoading = true)
            
            try {
                val directory = File(currentState.selectedDirectory)
                val files = FileUtils.getMediaFiles(directory)
                
                val groupedFiles = if (currentState.groupByDate) {
                    FileUtils.groupFilesByTime(files, currentState.timeThreshold, currentState.dateType)
                        .mapIndexed { index, group -> index to group }
                        .toMap()
                } else {
                    emptyMap()
                }
                
                _uiState.value = currentState.copy(
                    files = files,
                    groupedFiles = groupedFiles,
                    isLoading = false
                )
                
                // Load file labels
                loadFileLabels(files.map { it.path })
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isLoading = false)
            }
        }
    }
    
    private fun loadAvailableLabels() {
        viewModelScope.launch {
            try {
                val labels = database.labelDao().getAllLabels()
                _uiState.value = _uiState.value.copy(availableLabels = labels)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private suspend fun loadFileLabels(filePaths: List<String>) {
        try {
            val allFileLabels = database.fileLabelDao().getAllFileLabels()
            val allLabels = database.labelDao().getAllLabels()
            val labelMap = allLabels.associateBy { it.id }
            
            val fileLabelsMap = allFileLabels
                .filter { filePaths.contains(it.filePath) }
                .groupBy { it.filePath }
                .mapValues { (_, fileLabels) ->
                    fileLabels.mapNotNull { labelMap[it.labelId] }
                }
            
            _uiState.value = _uiState.value.copy(fileLabels = fileLabelsMap)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun toggleFileLabel(filePath: String, label: Label) {
        viewModelScope.launch {
            try {
                val currentLabels = _uiState.value.fileLabels[filePath] ?: emptyList()
                val hasLabel = currentLabels.any { it.id == label.id }
                
                if (hasLabel) {
                    // Remove label
                    database.fileLabelDao().removeFileLabel(filePath, label.id)
                } else {
                    // Add label
                    val fileLabel = FileLabel(filePath = filePath, labelId = label.id)
                    database.fileLabelDao().insertFileLabel(fileLabel)
                }
                
                // Refresh file labels
                loadFileLabels(_uiState.value.files.map { it.path })
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}