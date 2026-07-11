package com.iamsubho.drivesync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iamsubho.drivesync.domain.model.DriveAccount

@Entity(tableName = "drive_accounts")
data class DriveAccountEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val photoUrl: String?,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val addedAt: Long,
) {
    fun toDomain() = DriveAccount(
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        storageUsedBytes = storageUsedBytes,
        storageTotalBytes = storageTotalBytes,
        addedAt = addedAt,
    )
}

fun DriveAccount.toEntity() = DriveAccountEntity(
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    storageUsedBytes = storageUsedBytes,
    storageTotalBytes = storageTotalBytes,
    addedAt = addedAt,
)
