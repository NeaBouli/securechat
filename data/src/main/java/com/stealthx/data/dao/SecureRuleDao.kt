/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stealthx.data.entity.SecureRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureRuleDao {

    @Query("SELECT * FROM secure_rules WHERE is_enabled = 1 ORDER BY priority DESC")
    fun observeEnabled(): Flow<List<SecureRuleEntity>>

    @Query("SELECT * FROM secure_rules ORDER BY priority DESC")
    fun observeAll(): Flow<List<SecureRuleEntity>>

    @Query("SELECT * FROM secure_rules WHERE id = :id")
    suspend fun getById(id: String): SecureRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SecureRuleEntity)

    @Update
    suspend fun update(rule: SecureRuleEntity)

    @Delete
    suspend fun delete(rule: SecureRuleEntity)

    @Query("DELETE FROM secure_rules")
    suspend fun deleteAll()

    @Query("UPDATE secure_rules SET trigger_count = trigger_count + 1, last_triggered = :timestamp WHERE id = :id")
    suspend fun recordTrigger(id: String, timestamp: Long)
}
