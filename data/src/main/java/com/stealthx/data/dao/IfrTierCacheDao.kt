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
import com.stealthx.data.entity.IfrTierCacheEntity

@Dao
interface IfrTierCacheDao {

    @Query("SELECT * FROM ifr_tier_cache WHERE wallet_address = :walletAddress")
    suspend fun getByWallet(walletAddress: String): IfrTierCacheEntity?

    @Query("SELECT * FROM ifr_tier_cache LIMIT 1")
    suspend fun getCurrent(): IfrTierCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: IfrTierCacheEntity)

    @Query("DELETE FROM ifr_tier_cache")
    suspend fun deleteAll()

    @Query("DELETE FROM ifr_tier_cache WHERE wallet_address = :walletAddress")
    suspend fun deleteByWallet(walletAddress: String)
}
