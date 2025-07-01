package com.hdya.imagetagging.ui.gallery

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hdya.imagetagging.data.Label
import com.hdya.imagetagging.utils.MediaFile
import com.hdya.imagetagging.utils.PinyinUtils
import com.hdya.imagetagging.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaFileItem(
    file: MediaFile,
    labels: List<Label>,
    availableLabels: List<Label>,
    viewModel: GalleryViewModel,
    onLabelClick: (Label) -> Unit
) {
    var showLabelSelector by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            if (FileUtils.supportsThumbnail(File(file.path))) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (file.isVideo) {
                                // Open video in external app
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val videoFile = File(file.path)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            videoFile
                                        )
                                        setDataAndType(uri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback: try to open with simple intent
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(android.net.Uri.parse("file://${file.path}"), "video/*")
                                        }
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        // Handle error - could show a toast or snackbar
                                    }
                                }
                            } else {
                                showPreview = true
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                // Show a placeholder for unsupported formats
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (file.isVideo) {
                                // Open video in external app
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val videoFile = File(file.path)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            videoFile
                                        )
                                        setDataAndType(uri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback: try to open with simple intent
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(android.net.Uri.parse("file://${file.path}"), "video/*")
                                        }
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        // Handle error - could show a toast or snackbar
                                    }
                                }
                            } else {
                                showPreview = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = file.name.take(3).uppercase(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // File info and labels
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatFileDate(file.captureDate ?: file.lastModified),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Labels
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(labels) { label ->
                        AssistChip(
                            onClick = { onLabelClick(label) },
                            label = { Text(label.name) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showLabelSelector = true },
                            label = { Text("+") }
                        )
                    }
                }
            }
        }
    }
    
    if (showLabelSelector) {
        LabelSelectorDialog(
            availableLabels = viewModel.getSortedLabelsWithRecentFirst(),
            selectedLabels = labels,
            onDismiss = { showLabelSelector = false },
            onLabelToggle = { label ->
                onLabelClick(label)
            }
        )
    }
    
    if (showPreview) {
        ImagePreviewDialog(
            filePath = file.path,
            onDismiss = { showPreview = false }
        )
    }
}

@Composable
fun GroupHeader(
    groupIndex: Int,
    fileCount: Int,
    availableLabels: List<Label>,
    viewModel: GalleryViewModel,
    onLabelClick: (Label) -> Unit
) {
    var showLabelSelector by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showLabelSelector = true }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Group ${groupIndex + 1}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$fileCount files",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Tap to assign labels to all files in this group",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showLabelSelector) {
        LabelSelectorDialog(
            availableLabels = viewModel.getSortedLabelsWithRecentFirst(),
            selectedLabels = emptyList(), // For groups, we don't show pre-selected labels
            onDismiss = { showLabelSelector = false },
            onLabelToggle = { label ->
                onLabelClick(label)
            }
        )
    }
}

@Composable
fun LabelSelectorDialog(
    availableLabels: List<Label>,
    selectedLabels: List<Label>,
    onDismiss: () -> Unit,
    onLabelToggle: (Label) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val selectedLabelIds = selectedLabels.map { it.id }.toSet()
    
    val filteredLabels = if (searchText.isBlank()) {
        // Show only first 10 labels when no search text
        availableLabels.take(10)
    } else {
        // Show search results when typing (with Pinyin support)
        availableLabels.filter { 
            PinyinUtils.matchesSearch(it.name, searchText)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Labels") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search labels") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Make the labels list scrollable with a defined height
                LazyColumn(
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLabels) { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLabelToggle(label) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedLabelIds.contains(label.id),
                                onCheckedChange = { onLabelToggle(label) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ImagePreviewDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(filePath)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun formatFileDate(timestamp: Long): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}