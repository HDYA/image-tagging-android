package com.hdya.imagetagging.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class MediaFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val dateAdded: Long = lastModified,
    val exifDate: Long? = null,
    val captureDate: Long? = null,
    val isVideo: Boolean = false
)

object FileUtils {
    
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    private val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "3gp", "webm")
    
    fun isMediaFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return imageExtensions.contains(extension) || videoExtensions.contains(extension)
    }
    
    fun isVideoFile(file: File): Boolean {
        return videoExtensions.contains(file.extension.lowercase())
    }
    
    fun getMediaFiles(directory: File): List<MediaFile> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        
        return directory.walkTopDown()
            .filter { it.isFile && isMediaFile(it) }
            .map { file ->
                val exifDate = getCaptureDate(file)
                MediaFile(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    dateAdded = getFileCreationDate(file) ?: file.lastModified(),
                    exifDate = exifDate,
                    captureDate = exifDate,
                    isVideo = isVideoFile(file)
                )
            }
            .sortedBy { it.name }
            .toList()
    }
    
    private fun getCaptureDate(file: File): Long? {
        return try {
            if (isVideoFile(file)) {
                // For videos, use file modification time as capture date
                file.lastModified()
            } else {
                // For images, try to get EXIF data
                val exif = ExifInterface(file.absolutePath)
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                dateTime?.let {
                    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    format.parse(it)?.time
                } ?: file.lastModified()
            }
        } catch (e: Exception) {
            file.lastModified()
        }
    }
    
    private fun getFileCreationDate(file: File): Long? {
        return try {
            // On Android, file creation time is often the same as last modified time
            // In newer Android versions, we could use BasicFileAttributes but keeping it simple
            file.lastModified()
        } catch (e: Exception) {
            file.lastModified()
        }
    }
    
    fun groupFilesByTime(files: List<MediaFile>, thresholdSeconds: Int, dateType: String = "EXIF"): List<List<MediaFile>> {
        if (files.isEmpty()) return emptyList()
        
        val sorted = files.sortedBy { 
            when (dateType) {
                "CREATE" -> it.createdAt ?: it.lastModified
                "MODIFY" -> it.lastModified
                "EXIF" -> it.captureDate ?: it.lastModified
                else -> it.captureDate ?: it.lastModified
            }
        }
        
        val groups = mutableListOf<MutableList<MediaFile>>()
        var currentGroup = mutableListOf<MediaFile>()
        
        for (file in sorted) {
            val fileTime = when (dateType) {
                "CREATE" -> file.createdAt ?: file.lastModified
                "MODIFY" -> file.lastModified
                "EXIF" -> file.captureDate ?: file.lastModified
                else -> file.captureDate ?: file.lastModified
            }
            
            if (currentGroup.isEmpty()) {
                currentGroup.add(file)
            } else {
                val lastFileTime = when (dateType) {
                    "CREATE" -> currentGroup.last().createdAt ?: currentGroup.last().lastModified
                    "MODIFY" -> currentGroup.last().lastModified
                    "EXIF" -> currentGroup.last().captureDate ?: currentGroup.last().lastModified
                    else -> currentGroup.last().captureDate ?: currentGroup.last().lastModified
                }
                val timeDiff = Math.abs(fileTime - lastFileTime) / 1000 // Convert to seconds
                
                if (timeDiff <= thresholdSeconds) {
                    currentGroup.add(file)
                } else {
                    groups.add(currentGroup)
                    currentGroup = mutableListOf(file)
                }
            }
        }
        
        if (currentGroup.isNotEmpty()) {
            groups.add(currentGroup)
        }
        
        return groups
    }
}