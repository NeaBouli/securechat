/*
 * SecureChat — Stealth Delete Wipe Manager
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.security

import android.content.Context
import com.stealthx.data.ChameleonDatabase
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WipeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ChameleonDatabase,
    private val appPreferences: AppPreferences
) {
    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        runCatching { database.close() }
        deleteDatabaseFiles()
        clearPreferences()
        deleteRecursively(File(context.filesDir, SECURE_VAULT_DIR))
        deleteRecursively(context.cacheDir)
        deleteRecursively(context.codeCacheDir)
    }

    private fun deleteDatabaseFiles() {
        context.deleteDatabase(DB_NAME)
        val dbFile = context.getDatabasePath(DB_NAME)
        listOf(dbFile, File("${dbFile.path}-wal"), File("${dbFile.path}-shm"), File("${dbFile.path}-journal"))
            .forEach { runCatching { it.delete() } }
    }

    private fun clearPreferences() {
        runCatching { appPreferences.clear() }
        runCatching { StealthXIdentity.clear(context) }
        runCatching {
            context.getSharedPreferences(DB_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    private fun deleteRecursively(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach(::deleteRecursively)
        }
        runCatching { file.delete() }
    }

    private companion object {
        const val DB_NAME = "chameleon_secure.db"
        const val DB_PREFS_NAME = "chameleon_secure"
        const val SECURE_VAULT_DIR = "secure_vault"
    }
}
