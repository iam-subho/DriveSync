package com.iamsubho.drivesync.di

import com.iamsubho.drivesync.data.repository.ExtensionRepositoryImpl
import com.iamsubho.drivesync.data.repository.SyncControllerImpl
import com.iamsubho.drivesync.data.repository.SyncJobRepositoryImpl
import com.iamsubho.drivesync.data.repository.TransferLogRepositoryImpl
import com.iamsubho.drivesync.domain.repository.ExtensionRepository
import com.iamsubho.drivesync.domain.repository.SyncController
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import com.iamsubho.drivesync.domain.repository.TransferLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSyncJobRepository(impl: SyncJobRepositoryImpl): SyncJobRepository

    @Binds
    abstract fun bindTransferLogRepository(impl: TransferLogRepositoryImpl): TransferLogRepository

    @Binds
    abstract fun bindExtensionRepository(impl: ExtensionRepositoryImpl): ExtensionRepository

    @Binds
    abstract fun bindSyncController(impl: SyncControllerImpl): SyncController
}
