package com.hdya.imagetagging.ui.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hdya.imagetagging.data.Label
import com.hdya.imagetagging.utils.MediaFile
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaFileItem(
    file: MediaFile,
    labels: List<Label>,
    availableLabels: List<Label>,
    onLabelClick: (Label) -> Unit
) {
    var showLabelSelector by remember { mutableStateOf(false) }
    
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file.path)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            
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
            availableLabels = availableLabels,
            selectedLabels = labels,
            onDismiss = { showLabelSelector = false },
            onLabelToggle = { label ->
                onLabelClick(label)
            }
        )
    }
}

@Composable
fun GroupHeader(
    groupIndex: Int,
    fileCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
        }
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
        availableLabels
    } else {
        availableLabels.filter { 
            it.name.contains(searchText, ignoreCase = true) 
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
                
                filteredLabels.forEach { label ->
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private fun formatFileDate(timestamp: Long): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}