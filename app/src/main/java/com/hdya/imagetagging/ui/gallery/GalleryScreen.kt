package com.hdya.imagetagging.ui.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.hdya.imagetagging.R
import com.hdya.imagetagging.data.AppDatabase
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.utils.MediaFile

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryScreen(
    database: AppDatabase,
    preferencesRepository: PreferencesRepository,
    viewModel: GalleryViewModel = viewModel { GalleryViewModel(database, preferencesRepository) }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Permission handling
    val storagePermission = rememberPermissionState(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    LaunchedEffect(storagePermission.status.isGranted) {
        if (storagePermission.status.isGranted) {
            viewModel.loadFiles()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!storagePermission.status.isGranted) {
            // Permission request UI
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.permission_required),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { storagePermission.launchPermissionRequest() }
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        } else if (uiState.selectedDirectory == null) {
            // No directory selected
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_directory_selected),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Go to Settings to select a directory first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (uiState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.files.isEmpty()) {
            // No files found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No images or videos found in the selected directory.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            // File list
            val listState = rememberLazyListState()
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Action buttons - responsive layout
                item {
                    if (screenWidth > 600.dp) {
                        // Wide screen - single row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.jumpToNextUnlabeled(listState) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Jump to Next Unlabeled")
                            }
                            Button(
                                onClick = { viewModel.exportCurrentPageCSV(context) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isGeneratingCSV
                            ) {
                                if (uiState.isGeneratingCSV) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Export Current Page")
                            }
                        }
                    } else {
                        // Narrow screen - two rows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.jumpToNextUnlabeled(listState) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Jump to Next Unlabeled")
                            }
                            Button(
                                onClick = { viewModel.exportCurrentPageCSV(context) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isGeneratingCSV
                            ) {
                                if (uiState.isGeneratingCSV) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Export Current Page")
                            }
                        }
                    }
                }
                
                // Page selector
                if (uiState.pages.size > 1) {
                    item {
                        PageSelector(
                            pages = uiState.pages,
                            currentPage = uiState.currentPage,
                            totalFiles = uiState.totalFiles,
                            onPageSelected = { pageIndex ->
                                viewModel.loadSpecificPage(pageIndex)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                }
                
                if (uiState.groupByDate) {
                    // Grouped view
                    uiState.groupedFiles.forEach { (groupIndex, files) ->
                        item {
                            GroupHeader(
                                groupIndex = groupIndex,
                                fileCount = files.size,
                                availableLabels = uiState.availableLabels,
                                viewModel = viewModel,
                                onLabelClick = { label ->
                                    viewModel.toggleGroupLabel(groupIndex, label)
                                }
                            )
                        }
                        items(files) { file ->
                            MediaFileItem(
                                file = file,
                                labels = uiState.fileLabels[file.path] ?: emptyList(),
                                availableLabels = uiState.availableLabels,
                                viewModel = viewModel,
                                onLabelClick = { label ->
                                    viewModel.toggleFileLabel(file.path, label)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    // Flat list view
                    items(uiState.files) { file ->
                        MediaFileItem(
                            file = file,
                            labels = uiState.fileLabels[file.path] ?: emptyList(),
                            availableLabels = uiState.availableLabels,
                            viewModel = viewModel,
                            onLabelClick = { label ->
                                viewModel.toggleFileLabel(file.path, label)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // CSV Display Dialog
    uiState.csvContent?.let { csvContent ->
        Dialog(
            onDismissRequest = { viewModel.clearCSVContent() }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
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
                            text = "CSV Content - Page ${uiState.currentPage + 1}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("CSV Export", csvContent)
                                    clipboard.setPrimaryClip(clip)
                                }
                            ) {
                                Text("Copy")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { viewModel.clearCSVContent() }
                            ) {
                                Text("Close")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SelectionContainer {
                        Text(
                            text = csvContent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}