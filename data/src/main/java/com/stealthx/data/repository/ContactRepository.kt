/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.entity.ContactKeyEntity
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierLimitException
import com.stealthx.shared.model.IfrTier
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactKeyDao: ContactKeyDao,
    private val tierGate: TierGate
) {
    companion object {
        const val FREE_CONTACT_LIMIT = 10
    }

    /**
     * Add a contact. Throws [TierLimitException] if FREE tier and limit reached.
     * Count + insert are atomic — no TOCTOU race condition.
     */
    suspend fun addContact(contact: ContactKeyEntity) {
        if (tierGate.getTier() < IfrTier.PRO) {
            val inserted = contactKeyDao.insertIfUnderLimit(contact, FREE_CONTACT_LIMIT)
            if (!inserted) {
                val count = contactKeyDao.count()
                throw TierLimitException(
                    "Contact limit reached ($count/$FREE_CONTACT_LIMIT). Upgrade to Pro for unlimited contacts."
                )
            }
        } else {
            contactKeyDao.insert(contact)
        }
    }

    fun observeAll(): Flow<List<ContactKeyEntity>> = contactKeyDao.observeAll()

    suspend fun getById(id: String): ContactKeyEntity? = contactKeyDao.getById(id)

    suspend fun deleteById(id: String) = contactKeyDao.deleteById(id)

    suspend fun count(): Int = contactKeyDao.count()
}
