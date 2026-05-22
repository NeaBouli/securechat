/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.stealthx.data.entity.ContactKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactKeyDao {

    @Query("SELECT COUNT(*) FROM contact_keys")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(contact: ContactKeyEntity)

    /**
     * Atomic count-check + insert in one transaction.
     * Returns true if inserted, false if limit already reached.
     */
    @Transaction
    suspend fun insertIfUnderLimit(contact: ContactKeyEntity, limit: Int): Boolean {
        if (count() >= limit) return false
        insert(contact)
        return true
    }

    @Query("SELECT * FROM contact_keys ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ContactKeyEntity>>

    @Query("SELECT * FROM contact_keys WHERE id = :id")
    suspend fun getById(id: String): ContactKeyEntity?

    @Query("DELETE FROM contact_keys WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE contact_keys SET display_name = :displayName WHERE id = :id")
    suspend fun updateDisplayName(id: String, displayName: String)
}
