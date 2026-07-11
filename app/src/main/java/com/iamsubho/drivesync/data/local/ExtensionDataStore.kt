package com.iamsubho.drivesync.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.extensionsDataStore by preferencesDataStore(name = "extensions")

/** Persists editable extension lists. Absent key = category defaults. */
@Singleton
class ExtensionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun keyFor(category: FileTypeCategory): Preferences.Key<String> =
        stringPreferencesKey("extensions_${category.name.lowercase()}")

    fun observe(category: FileTypeCategory): Flow<List<String>> =
        context.extensionsDataStore.data.map { prefs ->
            decode(prefs[keyFor(category)]) ?: ExtensionDefaults.forCategory(category)
        }

    suspend fun snapshot(category: FileTypeCategory): List<String> = observe(category).first()

    suspend fun save(category: FileTypeCategory, extensions: List<String>) {
        context.extensionsDataStore.edit { prefs ->
            prefs[keyFor(category)] = extensions.joinToString(",")
        }
    }

    suspend fun restoreDefaults(category: FileTypeCategory) {
        context.extensionsDataStore.edit { prefs ->
            prefs.remove(keyFor(category))
        }
    }

    private fun decode(raw: String?): List<String>? =
        raw?.split(',')?.filter { it.isNotBlank() }
}
