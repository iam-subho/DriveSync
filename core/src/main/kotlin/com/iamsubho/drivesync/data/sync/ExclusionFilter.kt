package com.iamsubho.drivesync.data.sync

/**
 * Excluded-subfolder matching. Paths are relative to the job root with '/' separators;
 * excluded entries are folder paths without trailing slash (e.g. "Sent", "Private/Temp").
 */
object ExclusionFilter {

    /** True when a FILE at [relativePath] lies inside any excluded folder. */
    fun isExcluded(relativePath: String, excludedFolders: Collection<String>): Boolean =
        excludedFolders.any { folder -> relativePath.startsWith("$folder/") }

    /** True when a DIRECTORY at [relativeDir] is an excluded folder or inside one. */
    fun isExcludedDir(relativeDir: String, excludedFolders: Collection<String>): Boolean =
        excludedFolders.any { folder ->
            relativeDir == folder || relativeDir.startsWith("$folder/")
        }
}
