package com.hdya.imagetagging.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.ui.gallery.PageInfo
import com.hdya.imagetagging.utils.FileUtils
import com.hdya.imagetagging.utils.MediaFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class SettingsUiState(
    val selectedDirectory: String? = null,
    val timeThreshold: Int = 3600,
    val groupByDate: Boolean = false,
    val dateType: String = "EXIF",
    val sortBy: String = "NAME",
    val sortAscending: Boolean = true,
    val pageSize: Int = 150,
    val isGeneratingCSV: Boolean = false,
    val csvContent: String? = null,
    val pages: List<com.hdya.imagetagging.ui.gallery.PageInfo> = emptyList(),
    val showPageSelector: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _isGeneratingCSV = MutableStateFlow(false)
    private val _csvContent = MutableStateFlow<String?>(null)
    private val _pages = MutableStateFlow<List<com.hdya.imagetagging.ui.gallery.PageInfo>>(emptyList())
    private val _showPageSelector = MutableStateFlow(false)
    
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.selectedDirectory,
        preferencesRepository.timeThreshold,
        preferencesRepository.groupByDate,
        preferencesRepository.dateType,
        preferencesRepository.sortBy,
        preferencesRepository.sortAscending,
        preferencesRepository.pageSize,
        _isGeneratingCSV,
        _csvContent,
        _pages,
        _showPageSelector
    ) { values ->
        SettingsUiState(
            selectedDirectory = values[0] as String?,
            timeThreshold = values[1] as Int,
            groupByDate = values[2] as Boolean,
            dateType = values[3] as String,
            sortBy = values[4] as String,
            sortAscending = values[5] as Boolean,
            pageSize = values[6] as Int,
            isGeneratingCSV = values[7] as Boolean,
            csvContent = values[8] as String?,
            pages = values[9] as List<com.hdya.imagetagging.ui.gallery.PageInfo>,
            showPageSelector = values[10] as Boolean
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
    
    suspend fun setPageSize(size: Int) {
        preferencesRepository.setPageSize(size)
    }
    
    fun prepareCSVExport(context: Context, database: com.hdya.imagetagging.data.AppDatabase) {
        viewModelScope.launch {
            try {
                // Get selected directory from preferences
                val selectedDirectory = preferencesRepository.selectedDirectory.first()
                if (selectedDirectory == null) {
                    _csvContent.value = "Error: No directory selected"
                    return@launch
                }
                
                val currentState = uiState.first()
                
                // Get all media files from the selected directory
                val allFiles = FileUtils.getMediaFiles(File(selectedDirectory))
                val sortedFiles = sortFiles(allFiles, currentState.sortBy, currentState.sortAscending, currentState.dateType)
                
                // Generate page information
                val pages = generatePageInfo(sortedFiles, currentState.pageSize, currentState.dateType)
                _pages.value = pages
                
                if (pages.size <= 1) {
                    // If only one page, generate CSV directly
                    generateCSVContent(context, database, pages.firstOrNull())
                } else {
                    // Show page selector
                    _showPageSelector.value = true
                }
                
            } catch (e: Exception) {
                _csvContent.value = "Error preparing CSV export: ${e.message}"
            }
        }
    }
    
    fun generateCSVForPage(context: Context, database: com.hdya.imagetagging.data.AppDatabase, pageInfo: PageInfo?) {
        _showPageSelector.value = false
        generateCSVContent(context, database, pageInfo)
    }
    
    fun hidePageSelector() {
        _showPageSelector.value = false
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
    
    private fun generatePageInfo(files: List<MediaFile>, pageSize: Int, dateType: String): List<PageInfo> {
        val pages = mutableListOf<PageInfo>()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        for (i in files.indices step pageSize) {
            val endIndex = minOf(i + pageSize, files.size)
            val pageFiles = files.subList(i, endIndex)
            
            if (pageFiles.isNotEmpty()) {
                val firstFile = pageFiles.first()
                val lastFile = pageFiles.last()
                
                val firstDate = getFileDate(firstFile, dateType)
                val lastDate = getFileDate(lastFile, dateType)
                
                pages.add(PageInfo(
                    pageIndex = i / pageSize,
                    startIndex = i,
                    endIndex = endIndex - 1,
                    firstFileName = firstFile.name,
                    lastFileName = lastFile.name,
                    firstFileDate = dateFormatter.format(Date(firstDate)),
                    lastFileDate = dateFormatter.format(Date(lastDate))
                ))
            }
        }
        
        return pages
    }
    
    private fun getFileDate(file: MediaFile, dateType: String): Long {
        return when (dateType) {
            "CREATE" -> file.dateAdded
            "MODIFY" -> file.lastModified
            "EXIF" -> file.captureDate ?: file.lastModified
            else -> file.captureDate ?: file.lastModified
        }
    }
    private fun generateCSVContent(context: Context, database: com.hdya.imagetagging.data.AppDatabase, pageInfo: PageInfo?) {
        viewModelScope.launch {
            try {
                _isGeneratingCSV.value = true
                
                // Get selected directory from preferences
                val selectedDirectory = preferencesRepository.selectedDirectory.first()
                if (selectedDirectory == null) {
                    _csvContent.value = "Error: No directory selected"
                    return@launch
                }
                
                // Get all media files from the selected directory
                val allFiles = FileUtils.getMediaFiles(File(selectedDirectory))
                val currentState = uiState.first()
                val sortedFiles = sortFiles(allFiles, currentState.sortBy, currentState.sortAscending, currentState.dateType)
                
                // Get files for the specific page
                val targetFiles = if (pageInfo != null) {
                    sortedFiles.subList(pageInfo.startIndex, pageInfo.endIndex + 1)
                } else {
                    sortedFiles
                }
                
                val filePaths = targetFiles.map { it.path }.toSet()
                
                // Get all file labels and labels from database
                val fileLabels = database.fileLabelDao().getAllFileLabels()
                val labels = database.labelDao().getAllLabels()
                val labelMap = labels.associateBy { it.id }
                
                // Filter file labels to only include files from the current page/directory
                val filteredFileLabels = fileLabels.filter { filePaths.contains(it.filePath) }
                
                // Group file labels by file path
                val groupedByFile = filteredFileLabels.groupBy { it.filePath }
                
                val csvContent = StringBuilder()
                csvContent.append("File Path,Labels\n")
                
                // Include ONLY files that have labels
                for ((filePath, fileLabelsForFile) in groupedByFile) {
                    val labelNames = fileLabelsForFile.mapNotNull { fileLabel ->
                        labelMap[fileLabel.labelId]?.name
                    }.joinToString(";")
                    
                    // Only include if there are actually labels
                    if (labelNames.isNotEmpty()) {
                        // Escape commas and quotes in file path
                        val escapedPath = if (filePath.contains(",") || filePath.contains("\"")) {
                            "\"${filePath.replace("\"", "\"\"")}\"" 
                        } else {
                            filePath
                        }
                        
                        csvContent.append("$escapedPath,$labelNames\n")
                    }
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