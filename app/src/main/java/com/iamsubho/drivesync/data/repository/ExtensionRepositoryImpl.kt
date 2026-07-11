package com.iamsubho.drivesync.data.repository

import com.iamsubho.drivesync.data.local.ExtensionDataStore
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.repository.ExtensionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRepositoryImpl @Inject constructor(
    private val dataStore: ExtensionDataStore,
) : ExtensionRepository {

    private val editableCategories = listOf(
        FileTypeCategory.IMAGES, FileTypeCategory.VIDEOS, FileTypeCategory.DOCUMENTS,
        FileTypeCategory.AUDIO, FileTypeCategory.ARCHIVES,
    )

    override fun observe(category: FileTypeCategory): Flow<List<String>> =
        dataStore.observe(category)

    override suspend fun snapshotAll(): Map<FileTypeCategory, Set<String>> =
        editableCategories.associateWith { dataStore.snapshot(it).toSet() }

    override suspend fun add(category: FileTypeCategory, ext: String) {
        val normalized = ext.trim().lowercase().removePrefix(".")
        if (normalized.isEmpty()) return
        val current = dataStore.snapshot(category)
        if (normalized in current) return
        dataStore.save(category, current + normalized)
    }

    override suspend fun remove(category: FileTypeCategory, ext: String) {
        dataStore.save(category, dataStore.snapshot(category) - ext)
    }

    override suspend fun restoreDefaults(category: FileTypeCategory) {
        dataStore.restoreDefaults(category)
    }
}
