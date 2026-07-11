package com.iamsubho.drivesync.data.remote

/** The account's Drive authorization is no longer valid; user must re-consent. */
class DriveAuthException(cause: Throwable? = null) :
    Exception("Account needs re-authorization", cause)

/** The Drive storage quota is exhausted. */
class DriveQuotaException(cause: Throwable? = null) :
    Exception("Drive storage full", cause)

/** The job's Drive folder no longer exists. */
class DriveFolderMissingException(cause: Throwable? = null) :
    Exception("Drive folder missing", cause)
