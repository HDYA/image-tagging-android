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
    val sortBy: String = "NAME",
    val sortAscending: Boolean = true,
    val availableLabels: List<Label> = emptyList(),
    val fileLabels: Map<String, List<Label>> = emptyMap(),
    val isLoading: Boolean = false,
    val currentPage: Int = 0,
    val pageSize: Int = 100,
    val hasMoreFiles: Boolean = false
)

class GalleryViewModel(
    private val database: AppDatabase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    private var allFiles: List<MediaFile> = emptyList()
    
    init {
        // Observe preferences changes
        viewModelScope.launch {
            combine(
                preferencesRepository.selectedDirectory,
                preferencesRepository.groupByDate,
                preferencesRepository.timeThreshold,
                preferencesRepository.dateType,
                preferencesRepository.sortBy,
                preferencesRepository.sortAscending
            ) { directory, groupByDate, threshold, dateType, sortBy, sortAscending ->
                _uiState.value = _uiState.value.copy(
                    selectedDirectory = directory,
                    groupByDate = groupByDate,
                    timeThreshold = threshold,
                    dateType = dateType,
                    sortBy = sortBy,
                    sortAscending = sortAscending
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
            
            _uiState.value = currentState.copy(isLoading = true, currentPage = 0)
            
            try {
                val directory = File(currentState.selectedDirectory)
                val rawFiles = FileUtils.getMediaFiles(directory)
                
                // Sort files based on preferences
                allFiles = sortFiles(rawFiles, currentState.sortBy, currentState.sortAscending, currentState.dateType)
                
                // Load first page
                loadPage(0)
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isLoading = false)
            }
        }
    }
    
    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.hasMoreFiles && !currentState.isLoading) {
            loadPage(currentState.currentPage + 1)
        }
    }
    
    private suspend fun loadPage(page: Int) {
        val currentState = _uiState.value
        val startIndex = page * currentState.pageSize
        val endIndex = minOf(startIndex + currentState.pageSize, allFiles.size)
        
        if (startIndex >= allFiles.size) {
            _uiState.value = currentState.copy(isLoading = false, hasMoreFiles = false)
            return
        }
        
        val pageFiles = allFiles.subList(startIndex, endIndex)
        val allDisplayedFiles = if (page == 0) pageFiles else currentState.files + pageFiles
        
        val groupedFiles = if (currentState.groupByDate) {
            FileUtils.groupFilesByTime(allDisplayedFiles, currentState.timeThreshold, currentState.dateType)
                .mapIndexed { index, group -> index to group }
                .toMap()
        } else {
            emptyMap()
        }
        
        _uiState.value = currentState.copy(
            files = allDisplayedFiles,
            groupedFiles = groupedFiles,
            currentPage = page,
            hasMoreFiles = endIndex < allFiles.size,
            isLoading = false
        )
        
        // Load file labels for the new files
        loadFileLabels(pageFiles.map { it.path })
    }
    
    private fun sortFiles(files: List<MediaFile>, sortBy: String, ascending: Boolean, dateType: String): List<MediaFile> {
        return when (sortBy) {
            "NAME" -> if (ascending) files.sortedBy { it.name } else files.sortedByDescending { it.name }
            "DATE" -> {
                when (dateType) {
                    "EXIF" -> if (ascending) files.sortedBy { it.exifDate ?: it.lastModified } else files.sortedByDescending { it.exifDate ?: it.lastModified }
                    "CREATE" -> if (ascending) files.sortedBy { it.dateAdded } else files.sortedByDescending { it.dateAdded }
                    "MODIFY" -> if (ascending) files.sortedBy { it.lastModified } else files.sortedByDescending { it.lastModified }
                    else -> files
                }
            }
            else -> files
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
    
    fun toggleGroupLabel(groupIndex: Int, label: Label) {
        viewModelScope.launch {
            try {
                val groupFiles = _uiState.value.groupedFiles[groupIndex] ?: return@launch
                
                // Check if all files in group have this label
                val allHaveLabel = groupFiles.all { file ->
                    val fileLabels = _uiState.value.fileLabels[file.path] ?: emptyList()
                    fileLabels.any { it.id == label.id }
                }
                
                // If all files have the label, remove it from all; otherwise add it to all
                for (file in groupFiles) {
                    val currentLabels = _uiState.value.fileLabels[file.path] ?: emptyList()
                    val hasLabel = currentLabels.any { it.id == label.id }
                    
                    if (allHaveLabel && hasLabel) {
                        // Remove label
                        database.fileLabelDao().removeFileLabel(file.path, label.id)
                    } else if (!allHaveLabel && !hasLabel) {
                        // Add label
                        val fileLabel = FileLabel(filePath = file.path, labelId = label.id)
                        database.fileLabelDao().insertFileLabel(fileLabel)
                    }
                }
                
                // Refresh file labels
                loadFileLabels(_uiState.value.files.map { it.path })
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}