package com.hdya.imagetagging.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hdya.imagetagging.R
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.utils.CsvExporter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(preferencesRepository) }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Convert document tree URI to usable directory path
            val directoryPath = getDirectoryPathFromUri(context, it)
            if (directoryPath != null) {
                scope.launch {
                    viewModel.setSelectedDirectory(directoryPath)
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Directory Selection
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_directory),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.selectedDirectory ?: stringResource(R.string.no_directory_selected),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { directoryPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.select_directory))
                }
            }
        }
        
        // Time Threshold Setting
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.time_threshold),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.timeThreshold.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { threshold ->
                            scope.launch {
                                viewModel.setTimeThreshold(threshold)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.time_threshold)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Group by Date Setting
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.group_by_date),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = uiState.groupByDate,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                viewModel.setGroupByDate(enabled)
                            }
                        }
                    )
                }
                
                if (uiState.groupByDate) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Date Type for Grouping",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val dateTypes = listOf("EXIF" to "EXIF Taken Time", "CREATE" to "File Create Time", "MODIFY" to "File Modify Time")
                    dateTypes.forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.dateType == value,
                                onClick = {
                                    scope.launch {
                                        viewModel.setDateType(value)
                                    }
                                }
                            )
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // CSV Export
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.export_csv),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.exportCSV(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.export_csv))
                }
            }
        }
    }
}

/**
 * Convert document tree URI to a usable directory path for file operations
 */
private fun getDirectoryPathFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        // For document tree URIs, we need to handle them specially
        when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                when {
                    uri.authority == "com.android.externalstorage.documents" -> {
                        val split = docId.split(":")
                        if (split.size >= 2) {
                            val type = split[0]
                            val path = split[1]
                            when (type) {
                                "primary" -> android.os.Environment.getExternalStorageDirectory().toString() + "/" + path
                                else -> "/storage/$type/$path"
                            }
                        } else null
                    }
                    else -> null
                }
            }
            uri.scheme == "content" && uri.authority == "com.android.externalstorage.documents" -> {
                // Handle tree URIs
                val treeId = DocumentsContract.getTreeDocumentId(uri)
                val split = treeId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val path = if (split.size > 1) split[1] else ""
                    when (type) {
                        "primary" -> {
                            val basePath = android.os.Environment.getExternalStorageDirectory().toString()
                            if (path.isEmpty()) basePath else "$basePath/$path"
                        }
                        else -> {
                            if (path.isEmpty()) "/storage/$type" else "/storage/$type/$path"
                        }
                    }
                } else null
            }
            else -> uri.path
        }
    } catch (e: Exception) {
        // Fallback to common directories if URI parsing fails
        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).absolutePath
    }
}