/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stealthx.data.dao.AuditLogDao
import com.stealthx.data.dao.CryptoKeyDao
import com.stealthx.data.dao.IfrTierCacheDao
import com.stealthx.data.dao.SecureRuleDao
import com.stealthx.data.entity.AuditLogEntity
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.entity.CryptoKeyEntity
import com.stealthx.data.entity.IfrTierCacheEntity
import com.stealthx.data.entity.SecureRuleEntity
import net.sqlcipher.database.SupportFactory

/**
 * Chameleon Room Database — encrypted with SQLCipher.
 *
 * SECURITY:
 * - Database key from Android Keystore via KeystoreManager.getOrCreateAesKey()
 * - Key NEVER stored in SharedPreferences or plaintext
 * - exportSchema = true for migration support
 * - All tables encrypted at rest
 */
@Database(
    entities = [
        SecureRuleEntity::class,
        CryptoKeyEntity::class,
        ContactKeyEntity::class,
        AuditLogEntity::class,
        IfrTierCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(ChameleonTypeConverters::class)
abstract class ChameleonDatabase : RoomDatabase() {

    abstract fun secureRuleDao(): SecureRuleDao
    abstract fun cryptoKeyDao(): CryptoKeyDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun ifrTierCacheDao(): IfrTierCacheDao

    companion object {
        private const val DB_NAME = "chameleon_secure.db"

        /**
         * Build the encrypted database.
         *
         * @param context  Application context
         * @param passphrase  SQLCipher passphrase from Keystore (NEVER plaintext)
         */
        fun build(context: Context, passphrase: ByteArray): ChameleonDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                ChameleonDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
