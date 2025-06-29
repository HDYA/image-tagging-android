package com.hdya.imagetagging.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdya.imagetagging.data.AppDatabase
import com.hdya.imagetagging.data.Label
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
}