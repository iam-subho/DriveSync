package com.iamsubho.drivesync.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iamsubho.drivesync.data.local.dao.SyncJobDao
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.repository.SyncController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resumes jobs that were mid-sync when the device shut down. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var jobDao: SyncJobDao

    @Inject
    lateinit var syncController: SyncController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                jobDao.byStatus(SyncStatus.SYNCING.name).forEach { job ->
                    syncController.start(job.id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
