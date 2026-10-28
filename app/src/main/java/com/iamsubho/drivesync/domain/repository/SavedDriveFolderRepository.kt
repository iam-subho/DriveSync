package com.iamsubho.drivesync.domain.repository

import com.iamsubho.drivesync.domain.model.SavedDriveFolder
import kotlinx.coroutines.flow.Flow

interface SavedDriveFolderRepository {
    fun observeForAccount(accountEmail: String): Flow<List<SavedDriveFolder>>
    suspend fun save(folder: SavedDriveFolder)
    suspend fun remove(accountEmail: String, folderId: String)
}
