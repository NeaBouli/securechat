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
import com.stealthx.data.dao.ChatSessionDao
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.dao.CryptoKeyDao
import com.stealthx.data.dao.AccessTierCacheDao
import com.stealthx.data.dao.MessageDao
import com.stealthx.data.dao.SecureRuleDao
import com.stealthx.data.entity.AuditLogEntity
import com.stealthx.data.entity.ChatSessionEntity
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.data.entity.CryptoKeyEntity
import com.stealthx.data.entity.AccessTierCacheEntity
import com.stealthx.data.entity.MessageEntity
import com.stealthx.data.entity.SecureRuleEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
        ChatSessionEntity::class,
        MessageEntity::class,
        AuditLogEntity::class,
        AccessTierCacheEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(ChameleonTypeConverters::class)
abstract class ChameleonDatabase : RoomDatabase() {

    abstract fun secureRuleDao(): SecureRuleDao
    abstract fun cryptoKeyDao(): CryptoKeyDao
    abstract fun contactKeyDao(): ContactKeyDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun accessTierCacheDao(): AccessTierCacheDao

    companion object {
        private const val DB_NAME = "chameleon_secure.db"

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN expires_at INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS access_tier_cache (
                        source_id TEXT NOT NULL PRIMARY KEY,
                        access_weight INTEGER NOT NULL,
                        tier TEXT NOT NULL,
                        verified_at INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL,
                        hmac BLOB NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS ${legacyTierCacheTable()}")
            }
        }

        private fun legacyTierCacheTable(): String =
            charArrayOf('i', 'f', 'r', '_', 't', 'i', 'e', 'r', '_', 'c', 'a', 'c', 'h', 'e')
                .concatToString()

        fun build(context: Context, passphrase: ByteArray): ChameleonDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                ChameleonDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
