package com.hdya.imagetagging.ui.labels

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdya.imagetagging.data.AppDatabase
import com.hdya.imagetagging.data.Label
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class LabelsUiState(
    val labels: List<Label> = emptyList(),
    val isLoading: Boolean = false
)

class LabelsViewModel(
    private val database: AppDatabase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LabelsUiState())
    val uiState: StateFlow<LabelsUiState> = _uiState.asStateFlow()
    
    init {
        loadLabels()
    }
    
    private fun loadLabels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val labels = database.labelDao().getAllLabels()
                _uiState.value = _uiState.value.copy(
                    labels = labels,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun addLabel(name: String) {
        viewModelScope.launch {
            try {
                val label = Label(name = name.trim())
                database.labelDao().insertLabel(label)
                loadLabels() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateLabel(label: Label) {
        viewModelScope.launch {
            try {
                database.labelDao().updateLabel(label.copy(name = label.name.trim()))
                loadLabels() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteLabel(label: Label) {
        viewModelScope.launch {
            try {
                database.labelDao().deleteLabel(label)
                loadLabels() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun importLabelsFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                
                val labelsToImport = mutableListOf<String>()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    line?.let { 
                        // Support different formats:
                        // 1. Each line is a label
                        // 2. CSV format with labels separated by commas
                        val labels = if (it.contains(",")) {
                            it.split(",").map { label -> label.trim() }
                        } else {
                            listOf(it.trim())
                        }
                        
                        labels.forEach { label ->
                            if (label.isNotBlank()) {
                                labelsToImport.add(label)
                            }
                        }
                    }
                }
                
                reader.close()
                inputStream?.close()
                
                // Get existing labels to avoid duplicates
                val existingLabels = database.labelDao().getAllLabels().map { it.name.lowercase() }
                
                // Filter out duplicates and insert new labels
                val newLabels = labelsToImport
                    .filter { it.lowercase() !in existingLabels }
                    .distinct()
                    .map { Label(name = it) }
                
                if (newLabels.isNotEmpty()) {
                    database.labelDao().insertLabels(newLabels)
                }
                
                loadLabels() // Refresh the list
                
            } catch (e: Exception) {
                // Handle error - could show a toast or error message
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun importLabelsFromClipboard(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                
                if (clipData != null && clipData.itemCount > 0) {
                    val clipText = clipData.getItemAt(0).text?.toString()
                    
                    if (!clipText.isNullOrBlank()) {
                        val labelsToImport = mutableListOf<String>()
                        
                        // Split by newlines and process each line
                        clipText.split("\n").forEach { line ->
                            val trimmedLine = line.trim()
                            if (trimmedLine.isNotBlank()) {
                                labelsToImport.add(trimmedLine)
                            }
                        }
                        
                        // Get existing labels to avoid duplicates
                        val existingLabels = database.labelDao().getAllLabels().map { it.name.lowercase() }
                        
                        // Filter out duplicates and insert new labels
                        val newLabels = labelsToImport
                            .filter { it.lowercase() !in existingLabels }
                            .distinct()
                            .map { Label(name = it) }
                        
                        if (newLabels.isNotEmpty()) {
                            database.labelDao().insertLabels(newLabels)
                        }
                        
                        loadLabels() // Refresh the list
                    }
                }
                
            } catch (e: Exception) {
                // Handle error - could show a toast or error message
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun clearAllLabels() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get all labels
                val allLabels = database.labelDao().getAllLabels()
                // Get all file labels to see which labels are in use
                val allFileLabels = database.fileLabelDao().getAllFileLabels()
                val usedLabelIds = allFileLabels.map { it.labelId }.toSet()
                
                // Only delete unused labels
                for (label in allLabels) {
                    if (!usedLabelIds.contains(label.id)) {
                        database.labelDao().deleteLabel(label)
                    }
                }
                
                loadLabels() // Refresh the list
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}