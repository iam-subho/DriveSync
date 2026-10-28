package com.iamsubho.drivesync.data.repository

import com.iamsubho.drivesync.data.local.dao.SavedDriveFolderDao
import com.iamsubho.drivesync.data.local.entity.SavedDriveFolderEntity
import com.iamsubho.drivesync.domain.model.SavedDriveFolder
import com.iamsubho.drivesync.domain.repository.SavedDriveFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedDriveFolderRepositoryImpl @Inject constructor(
    private val dao: SavedDriveFolderDao,
) : SavedDriveFolderRepository {
    override fun observeForAccount(accountEmail: String): Flow<List<SavedDriveFolder>> =
        dao.observeForAccount(accountEmail).map { folders ->
            folders.map { SavedDriveFolder(it.accountEmail, it.folderId, it.folderName) }
        }

    override suspend fun save(folder: SavedDriveFolder) {
        dao.save(
            SavedDriveFolderEntity(folder.accountEmail, folder.folderId, folder.folderName),
        )
    }

    override suspend fun remove(accountEmail: String, folderId: String) {
        dao.remove(accountEmail, folderId)
    }
}
