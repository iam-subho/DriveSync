package com.iamsubho.drivesync.domain.model

enum class SyncDirection { TWO_WAY, UPLOAD_ONLY, DOWNLOAD_ONLY }

/** Behavior of a custom size limit relative to the configured threshold. */
enum class LimitMode {
    /** Files larger than the limit are skipped (the limit is a maximum). */
    SKIP_ABOVE,

    /** Only files larger than the limit are transferred (the limit is a minimum). */
    ONLY_ABOVE,
}

enum class FileTypeCategory { ALL, IMAGES, VIDEOS, DOCUMENTS, AUDIO, ARCHIVES, CUSTOM }

enum class SyncStatus { NEVER_SYNCED, UP_TO_DATE, SYNCING, PAUSED, ERROR }

enum class TransferResult { SUCCESS, FAILED, SKIPPED }

enum class TransferDirection { UPLOAD, DOWNLOAD }
