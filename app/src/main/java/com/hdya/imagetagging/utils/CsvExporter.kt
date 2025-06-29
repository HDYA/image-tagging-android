package com.hdya.imagetagging.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.hdya.imagetagging.data.FileLabel
import com.hdya.imagetagging.data.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter

object CsvExporter {
    
    suspend fun exportToCSV(
        context: Context,
        fileLabels: List<FileLabel>,
        labels: List<Label>
    ): File = withContext(Dispatchers.IO) {
        val labelMap = labels.associateBy { it.id }
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        
        val csvFile = File(cacheDir, "image_labels_${System.currentTimeMillis()}.csv")
        
        csvFile.bufferedWriter().use { writer ->
            // Write header
            writer.write("File Path,Labels\n")
            
            // Group file labels by file path
            val groupedByFile = fileLabels.groupBy { it.filePath }
            
            for ((filePath, labels) in groupedByFile) {
                val labelNames = labels.mapNotNull { fileLabel ->
                    labelMap[fileLabel.labelId]?.name
                }.joinToString(";")
                
                // Escape commas and quotes in file path
                val escapedPath = if (filePath.contains(",") || filePath.contains("\"")) {
                    "\"${filePath.replace("\"", "\"\"")}\"" 
                } else {
                    filePath
                }
                
                writer.write("$escapedPath,$labelNames\n")
            }
        }
        
        csvFile
    }
    
    fun shareCSVFile(context: Context, csvFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Image Labels Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share CSV"))
    }
}