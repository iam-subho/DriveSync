package com.iamsubho.drivesync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iamsubho.drivesync.data.local.entity.SavedDriveFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDriveFolderDao {
    @Query("SELECT * FROM SavedDriveFolderEntity WHERE accountEmail = :accountEmail ORDER BY folderName COLLATE NOCASE")
    fun observeForAccount(accountEmail: String): Flow<List<SavedDriveFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(folder: SavedDriveFolderEntity)

    @Query("DELETE FROM SavedDriveFolderEntity WHERE accountEmail = :accountEmail AND folderId = :folderId")
    suspend fun remove(accountEmail: String, folderId: String)
}
