package com.hdya.imagetagging.data

import androidx.room.*

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "file_labels")
data class FileLabel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val labelId: Long,
    val assignedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "file_groups")
data class FileGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String?,
    val threshold: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_files")
data class GroupFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val filePath: String
)

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    suspend fun getAllLabels(): List<Label>
    
    @Insert
    suspend fun insertLabel(label: Label): Long
    
    @Update
    suspend fun updateLabel(label: Label)
    
    @Delete
    suspend fun deleteLabel(label: Label)
    
    @Query("SELECT * FROM labels WHERE name LIKE :search ORDER BY name ASC")
    suspend fun searchLabels(search: String): List<Label>
}

@Dao
interface FileLabelDao {
    @Query("SELECT * FROM file_labels WHERE filePath = :filePath")
    suspend fun getLabelsForFile(filePath: String): List<FileLabel>
    
    @Insert
    suspend fun insertFileLabel(fileLabel: FileLabel)
    
    @Query("DELETE FROM file_labels WHERE filePath = :filePath AND labelId = :labelId")
    suspend fun removeFileLabel(filePath: String, labelId: Long)
    
    @Query("SELECT * FROM file_labels")
    suspend fun getAllFileLabels(): List<FileLabel>
}

@Dao
interface FileGroupDao {
    @Query("SELECT * FROM file_groups ORDER BY createdAt DESC")
    suspend fun getAllGroups(): List<FileGroup>
    
    @Insert
    suspend fun insertGroup(group: FileGroup): Long
    
    @Update
    suspend fun updateGroup(group: FileGroup)
    
    @Delete
    suspend fun deleteGroup(group: FileGroup)
}

@Dao
interface GroupFileDao {
    @Query("SELECT * FROM group_files WHERE groupId = :groupId")
    suspend fun getFilesInGroup(groupId: Long): List<GroupFile>
    
    @Insert
    suspend fun insertGroupFile(groupFile: GroupFile)
    
    @Query("DELETE FROM group_files WHERE groupId = :groupId")
    suspend fun deleteFilesFromGroup(groupId: Long)
}

@Database(
    entities = [Label::class, FileLabel::class, FileGroup::class, GroupFile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun labelDao(): LabelDao
    abstract fun fileLabelDao(): FileLabelDao
    abstract fun fileGroupDao(): FileGroupDao
    abstract fun groupFileDao(): GroupFileDao
}