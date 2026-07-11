package com.iamsubho.drivesync.data.sync

import com.iamsubho.drivesync.domain.model.SyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory live progress per job, shared between the worker (writer) and UI (readers). */
@Singleton
class SyncProgressBus @Inject constructor() {

    private val progressByJob = MutableStateFlow<Map<Long, SyncProgress>>(emptyMap())

    fun update(jobId: Long, progress: SyncProgress?) {
        progressByJob.update { map ->
            if (progress == null) map - jobId else map + (jobId to progress)
        }
    }

    fun flow(jobId: Long): Flow<SyncProgress?> =
        progressByJob.map { it[jobId] }.distinctUntilChanged()
}
