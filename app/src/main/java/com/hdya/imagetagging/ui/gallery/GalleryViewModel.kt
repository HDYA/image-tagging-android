package com.hdya.imagetagging.ui.gallery

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdya.imagetagging.data.*
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.utils.FileUtils
import com.hdya.imagetagging.utils.MediaFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class PageInfo(
    val pageIndex: Int,
    val startIndex: Int,
    val endIndex: Int,
    val firstFileName: String,
    val lastFileName: String,
    val firstFileDate: String,
    val lastFileDate: String
)

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
    val isProcessing: Boolean = false, // New state for UI processing
    val currentPage: Int = 0,
    val pageSize: Int = 150, // Updated default
    val hasMoreFiles: Boolean = false,
    val pages: List<PageInfo> = emptyList(),
    val totalFiles: Int = 0,
    val isGeneratingCSV: Boolean = false,
    val csvContent: String? = null,
    val showNoUnlabeledMessage: Boolean = false // New state for "no unlabeled" message
)

class GalleryViewModel(
    private val database: AppDatabase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    private var allFiles: List<MediaFile> = emptyList()
    private val recentlyUsedLabels = mutableListOf<Label>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    private val _isGeneratingCSV = MutableStateFlow(false)
    private val _csvContent = MutableStateFlow<String?>(null)
    
    init {
        // Observe preferences changes
        viewModelScope.launch {
            combine(
                preferencesRepository.selectedDirectory,
                preferencesRepository.groupByDate,
                preferencesRepository.timeThreshold,
                preferencesRepository.dateType,
                preferencesRepository.sortBy,
                preferencesRepository.sortAscending,
                preferencesRepository.pageSize,
                _isGeneratingCSV,
                _csvContent
            ) { values ->
                val directory = values[0] as String?
                val groupByDate = values[1] as Boolean
                val threshold = values[2] as Int
                val dateType = values[3] as String
                val sortBy = values[4] as String
                val sortAscending = values[5] as Boolean
                val pageSize = values[6] as Int
                val isGeneratingCSV = values[7] as Boolean
                val csvContent = values[8] as String?
                
                _uiState.value = _uiState.value.copy(
                    selectedDirectory = directory,
                    groupByDate = groupByDate,
                    timeThreshold = threshold,
                    dateType = dateType,
                    sortBy = sortBy,
                    sortAscending = sortAscending,
                    pageSize = pageSize,
                    isGeneratingCSV = isGeneratingCSV,
                    csvContent = csvContent
                )
                if (directory != null) {
                    loadFiles()
                }
            }.collect()
        }
        
        // Load available labels initially and set up periodic refresh
        loadAvailableLabels()
        
        // Refresh labels periodically to catch changes from other tabs
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000) // Refresh every 2 seconds
                loadAvailableLabels()
            }
        }
    }
    
    fun loadFiles() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.selectedDirectory == null) return@launch
            
            _uiState.value = currentState.copy(isLoading = true, isProcessing = true, currentPage = 0)
            
            try {
                val directory = File(currentState.selectedDirectory)
                val rawFiles = FileUtils.getMediaFiles(directory)
                
                // Sort files based on preferences
                allFiles = sortFiles(rawFiles, currentState.sortBy, currentState.sortAscending, currentState.dateType)
                
                // Generate page information
                val pages = generatePageInfo(allFiles, currentState.pageSize, currentState.dateType)
                
                _uiState.value = currentState.copy(
                    pages = pages,
                    totalFiles = allFiles.size,
                    hasMoreFiles = allFiles.size > currentState.pageSize
                )
                
                // Load first page
                loadPage(0)
                
                // Load all file labels for cross-page search functionality
                loadFileLabels(allFiles.map { it.path })
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isLoading = false, isProcessing = false)
            }
        }
    }
    
    fun loadNextPage() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.hasMoreFiles && !currentState.isLoading) {
                loadPage(currentState.currentPage + 1)
            }
        }
    }
    
    fun loadSpecificPage(pageIndex: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (pageIndex >= 0 && pageIndex < currentState.pages.size && !currentState.isLoading) {
                loadPage(pageIndex)
            }
        }
    }
    
    private fun generatePageInfo(files: List<MediaFile>, pageSize: Int, dateType: String): List<PageInfo> {
        val pages = mutableListOf<PageInfo>()
        
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
    
    private suspend fun loadPage(page: Int) {
        val currentState = _uiState.value
        val pageInfo = currentState.pages.getOrNull(page)
        
        if (pageInfo == null) {
            _uiState.value = currentState.copy(isLoading = false, isProcessing = false, hasMoreFiles = false)
            return
        }
        
        val pageFiles = allFiles.subList(pageInfo.startIndex, pageInfo.endIndex + 1)
        
        val groupedFiles = if (currentState.groupByDate) {
            val groups = FileUtils.groupFilesByTime(pageFiles, currentState.timeThreshold, currentState.dateType)
            
            // Sort groups based on the representative date of each group (using the first file's date as representative)
            val sortedGroups = groups.sortedBy { group ->
                val representativeFile = group.firstOrNull()
                representativeFile?.let { file ->
                    when (currentState.dateType) {
                        "CREATE" -> file.dateAdded
                        "MODIFY" -> file.lastModified
                        "EXIF" -> file.captureDate ?: file.lastModified
                        else -> file.captureDate ?: file.lastModified
                    }
                } ?: 0L
            }.let { sortedGroupsList ->
                if (currentState.sortAscending) sortedGroupsList else sortedGroupsList.reversed()
            }
            
            sortedGroups.mapIndexed { index, group -> index to group }.toMap()
        } else {
            emptyMap()
        }
        
        _uiState.value = currentState.copy(
            files = pageFiles,
            groupedFiles = groupedFiles,
            currentPage = page,
            hasMoreFiles = page < currentState.pages.size - 1,
            isLoading = false,
            isProcessing = false
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
            
            val newFileLabelsMap = allFileLabels
                .filter { filePaths.contains(it.filePath) }
                .groupBy { it.filePath }
                .mapValues { (_, fileLabels) ->
                    fileLabels.mapNotNull { labelMap[it.labelId] }
                }
            
            // Merge with existing file labels instead of replacing
            val currentFileLabels = _uiState.value.fileLabels.toMutableMap()
            currentFileLabels.putAll(newFileLabelsMap)
            
            _uiState.value = _uiState.value.copy(fileLabels = currentFileLabels)
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
                    addToRecentlyUsed(label) // Track recently used label
                }
                
                // Refresh just this file's labels by merging into existing map
                val allFileLabels = database.fileLabelDao().getLabelsForFile(filePath)
                val allLabels = database.labelDao().getAllLabels()
                val labelMap = allLabels.associateBy { it.id }
                
                val updatedLabels = allFileLabels.mapNotNull { labelMap[it.labelId] }
                val currentFileLabels = _uiState.value.fileLabels.toMutableMap()
                currentFileLabels[filePath] = updatedLabels
                
                _uiState.value = _uiState.value.copy(fileLabels = currentFileLabels)
                
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
                        addToRecentlyUsed(label) // Track recently used label
                    }
                }
                
                // Refresh labels for all files in the group
                val allLabels = database.labelDao().getAllLabels()
                val labelMap = allLabels.associateBy { it.id }
                val currentFileLabels = _uiState.value.fileLabels.toMutableMap()
                
                for (file in groupFiles) {
                    val fileLabels = database.fileLabelDao().getLabelsForFile(file.path)
                    currentFileLabels[file.path] = fileLabels.mapNotNull { labelMap[it.labelId] }
                }
                
                _uiState.value = _uiState.value.copy(fileLabels = currentFileLabels)
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun jumpToNextUnlabeled(listState: LazyListState) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Find unlabeled file starting from current page
            val result = findNextUnlabeledAcrossPages(currentState)
            
            when {
                result != null -> {
                    // Found unlabeled file - navigate to it
                    val (pageIndex, fileIndex) = result
                    
                    if (pageIndex != currentState.currentPage) {
                        // Load the target page first
                        loadPage(pageIndex)
                        // Wait for page to load before scrolling
                        kotlinx.coroutines.delay(100)
                    }
                    
                    // Scroll to the specific file
                    if (currentState.groupByDate) {
                        // In grouped view, find the correct item index
                        val targetIndex = findItemIndexInGroupedView(currentState, fileIndex)
                        targetIndex?.let { index ->
                            listState.animateScrollToItem(index)
                        }
                    } else {
                        // In flat view, scroll directly to file index
                        val currentPageFiles = currentState.files
                        if (fileIndex < currentPageFiles.size) {
                            listState.animateScrollToItem(fileIndex)
                        }
                    }
                }
                else -> {
                    // No unlabeled files found - show message
                    _uiState.value = currentState.copy(showNoUnlabeledMessage = true)
                }
            }
        }
    }
    
    private fun findNextUnlabeledAcrossPages(uiState: GalleryUiState): Pair<Int, Int>? {
        // Get all available labels for all files
        val allFileLabels = uiState.fileLabels
        
        // Start searching from the current page
        for (pageIndex in uiState.currentPage until uiState.pages.size) {
            val pageInfo = uiState.pages[pageIndex]
            val pageFiles = allFiles.subList(pageInfo.startIndex, pageInfo.endIndex + 1)
            
            // Find the first unlabeled file in this page
            for ((localIndex, file) in pageFiles.withIndex()) {
                val labels = allFileLabels[file.path] ?: emptyList()
                if (labels.isEmpty()) {
                    return Pair(pageIndex, localIndex)
                }
            }
        }
        
        // If not found in remaining pages, search from the beginning
        for (pageIndex in 0 until uiState.currentPage) {
            val pageInfo = uiState.pages[pageIndex]
            val pageFiles = allFiles.subList(pageInfo.startIndex, pageInfo.endIndex + 1)
            
            // Find the first unlabeled file in this page
            for ((localIndex, file) in pageFiles.withIndex()) {
                val labels = allFileLabels[file.path] ?: emptyList()
                if (labels.isEmpty()) {
                    return Pair(pageIndex, localIndex)
                }
            }
        }
        
        return null // No unlabeled files found anywhere
    }
    
    private fun findItemIndexInGroupedView(uiState: GalleryUiState, targetFileIndex: Int): Int? {
        // This function converts a file index to the corresponding item index in grouped LazyColumn
        val currentPageFiles = uiState.files
        if (targetFileIndex >= currentPageFiles.size) return null
        
        val targetFile = currentPageFiles[targetFileIndex]
        var itemIndex = 0
        
        for ((_, files) in uiState.groupedFiles.toSortedMap()) {
            // Add group header
            itemIndex += 1
            
            // Check files in this group
            for (file in files) {
                if (file.path == targetFile.path) {
                    return itemIndex
                }
                itemIndex += 1
            }
            
            // Add spacer
            itemIndex += 1
        }
        
        return null
    }
    
    private fun findNextUnlabeledInFlat(uiState: GalleryUiState): Int? {
        return uiState.files.indexOfFirst { file ->
            val labels = uiState.fileLabels[file.path] ?: emptyList()
            labels.isEmpty()
        }.takeIf { it >= 0 }
    }
    
    private fun findNextUnlabeledInGroups(uiState: GalleryUiState): Int? {
        var itemIndex = 0
        
        for ((_, files) in uiState.groupedFiles.toSortedMap()) {
            // Check if any file in this group is unlabeled
            val hasUnlabeledFile = files.any { file ->
                val labels = uiState.fileLabels[file.path] ?: emptyList()
                labels.isEmpty()
            }
            
            if (hasUnlabeledFile) {
                return itemIndex // Return the index of the group header
            }
            
            itemIndex += 1 + files.size + 1 // Group header + files + spacer
        }
        
        return null
    }
    
    fun getSortedLabelsWithRecentFirst(): List<Label> {
        val currentLabels = _uiState.value.availableLabels
        val remainingLabels = currentLabels.filter { label -> 
            !recentlyUsedLabels.any { recent -> recent.id == label.id } 
        }.sortedBy { it.name }
        
        return recentlyUsedLabels + remainingLabels
    }
    
    private fun addToRecentlyUsed(label: Label) {
        // Remove if already in list to avoid duplicates
        recentlyUsedLabels.removeAll { it.id == label.id }
        // Add to front
        recentlyUsedLabels.add(0, label)
        // Keep all recently used labels (no limit)
    }
    
    fun exportCurrentPageCSV(context: Context) {
        viewModelScope.launch {
            _isGeneratingCSV.value = true
            
            try {
                val currentState = _uiState.value
                val currentPageInfo = currentState.pages.getOrNull(currentState.currentPage)
                
                if (currentPageInfo == null) {
                    _csvContent.value = "Error: No current page found"
                    return@launch
                }
                
                generateCSVContent(currentPageInfo)
                
            } catch (e: Exception) {
                _csvContent.value = "Error generating CSV: ${e.message}"
            } finally {
                _isGeneratingCSV.value = false
            }
        }
    }
    
    private suspend fun generateCSVContent(pageInfo: PageInfo) {
        try {
            val pageFiles = allFiles.subList(pageInfo.startIndex, pageInfo.endIndex + 1)
            
            // Get all file labels for this page
            val allFileLabels = database.fileLabelDao().getAllFileLabels()
            val pageFileLabels = allFileLabels.filter { fileLabel ->
                pageFiles.any { it.path == fileLabel.filePath }
            }
            
            // Get all labels
            val allLabels = database.labelDao().getAllLabels()
            val labelMap = allLabels.associateBy { it.id }
            
            // Generate CSV content
            val csvBuilder = StringBuilder()
            csvBuilder.append("File Path,Labels\n")
            
            // Group file labels by file path
            val groupedByFile = pageFileLabels.groupBy { it.filePath }
            
            for (file in pageFiles) {
                val fileLabels = groupedByFile[file.path] ?: emptyList()
                val labelNames = fileLabels.mapNotNull { fileLabel ->
                    labelMap[fileLabel.labelId]?.name
                }.joinToString(";")
                
                // Escape commas and quotes in file path
                val escapedPath = if (file.path.contains(",") || file.path.contains("\"")) {
                    "\"${file.path.replace("\"", "\"\"")}\"" 
                } else {
                    file.path
                }
                
                csvBuilder.append("$escapedPath,$labelNames\n")
            }
            
            _csvContent.value = csvBuilder.toString()
            
        } catch (e: Exception) {
            _csvContent.value = "Error generating CSV content: ${e.message}"
        }
    }
    
    fun clearCSVContent() {
        _csvContent.value = null
    }
    
    fun dismissNoUnlabeledMessage() {
        _uiState.value = _uiState.value.copy(showNoUnlabeledMessage = false)
    }
}