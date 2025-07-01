package com.hdya.imagetagging.ui.labels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hdya.imagetagging.R
import com.hdya.imagetagging.data.AppDatabase
import com.hdya.imagetagging.data.Label
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    database: AppDatabase,
    viewModel: LabelsViewModel = viewModel { LabelsViewModel(database) }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLabel by remember { mutableStateOf<Label?>(null) }
    var filterText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_label))
            }
            
            Button(
                onClick = { 
                    scope.launch {
                        viewModel.importLabelsFromClipboard(context)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Import Clipboard")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Clear All button
        Button(
            onClick = {
                scope.launch {
                    viewModel.clearAllLabels()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Clear All")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter text field
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            label = { Text("Filter labels") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Labels list
        val filteredLabels = if (filterText.isBlank()) {
            uiState.labels
        } else {
            uiState.labels.filter { label ->
                label.name.lowercase().contains(filterText.lowercase().trim())
            }
        }
        
        if (filteredLabels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterText.isBlank()) {
                        "No labels yet. Add some labels to get started!"
                    } else {
                        "No labels match the filter \"$filterText\""
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLabels) { label ->
                    LabelItem(
                        label = label,
                        onEdit = { editingLabel = label },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteLabel(label)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Add/Edit dialog
    if (showAddDialog || editingLabel != null) {
        LabelDialog(
            label = editingLabel,
            onDismiss = {
                showAddDialog = false
                editingLabel = null
            },
            onSave = { labelName ->
                scope.launch {
                    if (editingLabel != null) {
                        viewModel.updateLabel(editingLabel!!.copy(name = labelName))
                    } else {
                        viewModel.addLabel(labelName)
                    }
                    showAddDialog = false
                    editingLabel = null
                }
            }
        )
    }
}

@Composable
fun LabelItem(
    label: Label,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun LabelDialog(
    label: Label?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var labelName by remember { mutableStateOf(label?.name ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (label != null) 
                    stringResource(R.string.edit_label) 
                else 
                    stringResource(R.string.add_label)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = labelName,
                    onValueChange = { labelName = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(labelName) },
                enabled = labelName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}