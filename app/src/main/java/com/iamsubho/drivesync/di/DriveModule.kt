package com.iamsubho.drivesync.di

import com.iamsubho.drivesync.data.remote.DriveStorageProvider
import com.iamsubho.drivesync.data.repository.AccountRepositoryImpl
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.domain.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveModule {

    @Binds
    abstract fun bindCloudStorageProvider(impl: DriveStorageProvider): CloudStorageProvider

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
