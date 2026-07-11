package com.iamsubho.drivesync.domain.repository

import com.iamsubho.drivesync.domain.model.FileTypeCategory
import kotlinx.coroutines.flow.Flow

/** Editable extension lists per category (IMAGES/VIDEOS/DOCUMENTS/AUDIO/ARCHIVES). */
interface ExtensionRepository {
    fun observe(category: FileTypeCategory): Flow<List<String>>
    suspend fun snapshotAll(): Map<FileTypeCategory, Set<String>>
    suspend fun add(category: FileTypeCategory, ext: String)
    suspend fun remove(category: FileTypeCategory, ext: String)
    suspend fun restoreDefaults(category: FileTypeCategory)
}
