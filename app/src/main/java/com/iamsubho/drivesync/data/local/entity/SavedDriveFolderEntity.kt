package com.iamsubho.drivesync.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["accountEmail", "folderId"])
data class SavedDriveFolderEntity(
    val accountEmail: String,
    val folderId: String,
    val folderName: String,
)
