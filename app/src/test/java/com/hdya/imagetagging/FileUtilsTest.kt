package com.hdya.imagetagging.utils

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class FileUtilsTest {

    @Test
    fun testIsMediaFile() {
        assertTrue(FileUtils.isMediaFile(File("test.jpg")))
        assertTrue(FileUtils.isMediaFile(File("test.jpeg")))
        assertTrue(FileUtils.isMediaFile(File("test.png")))
        assertTrue(FileUtils.isMediaFile(File("test.mp4")))
        assertTrue(FileUtils.isMediaFile(File("test.MOV"))) // Test case insensitive
        
        assertFalse(FileUtils.isMediaFile(File("test.txt")))
        assertFalse(FileUtils.isMediaFile(File("test.pdf")))
        assertFalse(FileUtils.isMediaFile(File("test")))
    }

    @Test
    fun testIsVideoFile() {
        assertTrue(FileUtils.isVideoFile(File("test.mp4")))
        assertTrue(FileUtils.isVideoFile(File("test.avi")))
        assertTrue(FileUtils.isVideoFile(File("test.MOV"))) // Test case insensitive
        
        assertFalse(FileUtils.isVideoFile(File("test.jpg")))
        assertFalse(FileUtils.isVideoFile(File("test.png")))
        assertFalse(FileUtils.isVideoFile(File("test.txt")))
    }

    @Test
    fun testGroupFilesByTime() {
        val files = listOf(
            MediaFile("file1.jpg", "file1.jpg", 100, 1000, 1000, false),
            MediaFile("file2.jpg", "file2.jpg", 100, 2000, 2000, false),
            MediaFile("file3.jpg", "file3.jpg", 100, 5000, 5000, false)
        )
        
        // Group with 3 second threshold
        val groups = FileUtils.groupFilesByTime(files, 3)
        assertEquals(2, groups.size) // file1&2 in one group, file3 in another
        
        // Group with 10 second threshold  
        val groupsLarge = FileUtils.groupFilesByTime(files, 10)
        assertEquals(1, groupsLarge.size) // All files in one group
    }
}