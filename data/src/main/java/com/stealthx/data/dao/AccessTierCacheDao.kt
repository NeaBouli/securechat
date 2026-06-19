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
import com.stealthx.data.entity.AccessTierCacheEntity

@Dao
interface AccessTierCacheDao {

    @Query("SELECT * FROM access_tier_cache WHERE source_id = :sourceId")
    suspend fun getBySource(sourceId: String): AccessTierCacheEntity?

    @Query("SELECT * FROM access_tier_cache LIMIT 1")
    suspend fun getCurrent(): AccessTierCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: AccessTierCacheEntity)

    @Query("DELETE FROM access_tier_cache")
    suspend fun deleteAll()

    @Query("DELETE FROM access_tier_cache WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: String)
}
