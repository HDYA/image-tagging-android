package com.hdya.imagetagging.ui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.groupByDate) {
                    // Grouped view
                    uiState.groupedFiles.forEach { (groupIndex, files) ->
                        item {
                            GroupHeader(
                                groupIndex = groupIndex,
                                fileCount = files.size,
                                availableLabels = uiState.availableLabels,
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
                            onLabelClick = { label ->
                                viewModel.toggleFileLabel(file.path, label)
                            }
                        )
                    }
                }
                
                // Load more button/indicator
                if (uiState.hasMoreFiles) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { viewModel.loadNextPage() },
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Load More")
                            }
                        }
                    }
                }
            }
            
            // Auto-load next page when reaching the end
            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && 
                            lastVisibleIndex >= uiState.files.size - 5 && 
                            uiState.hasMoreFiles && 
                            !uiState.isLoading) {
                            viewModel.loadNextPage()
                        }
                    }
            }
        }
    }
}