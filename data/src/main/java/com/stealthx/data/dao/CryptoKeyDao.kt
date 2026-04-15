/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stealthx.data.entity.CryptoKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoKeyDao {

    @Query("SELECT * FROM crypto_keys WHERE is_revoked = 0 ORDER BY created_at DESC")
    fun observeActive(): Flow<List<CryptoKeyEntity>>

    @Query("SELECT * FROM crypto_keys WHERE id = :id")
    suspend fun getById(id: String): CryptoKeyEntity?

    @Query("SELECT * FROM crypto_keys WHERE keystore_alias = :alias AND is_revoked = 0")
    suspend fun getByAlias(alias: String): CryptoKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(key: CryptoKeyEntity)

    @Query("UPDATE crypto_keys SET is_revoked = 1 WHERE id = :id")
    suspend fun revoke(id: String)

    @Query("DELETE FROM crypto_keys WHERE is_revoked = 1")
    suspend fun purgeRevoked()
}
