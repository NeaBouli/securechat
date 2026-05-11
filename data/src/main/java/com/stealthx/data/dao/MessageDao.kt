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
import com.stealthx.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE contact_id = :contactId ORDER BY sent_at ASC")
    fun observeForContact(contactId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE sent_at IN (
            SELECT MAX(sent_at) FROM messages GROUP BY contact_id
        )
        ORDER BY sent_at DESC
        """
    )
    fun observeLatestPerContact(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE contact_id = :contactId AND direction = 'INCOMING' AND delivery_status = 'UNREAD'")
    fun observeUnreadCount(contactId: String): Flow<Int>

    @Query("UPDATE messages SET delivery_status = 'READ' WHERE contact_id = :contactId AND direction = 'INCOMING'")
    suspend fun markRead(contactId: String)
}
