/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import com.stealthx.data.dao.IfrTierCacheDao
import com.stealthx.data.entity.IfrTierCacheEntity
import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.security.KeystoreManager
import com.stealthx.shared.model.CachedTierResult
import com.stealthx.shared.model.IfrTier
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IFR Tier Cache with HMAC-SHA256 tamper protection.
 *
 * CRITICAL SECURITY:
 * - HMAC key from KeystoreManager (hardware-backed)
 * - HMAC computed over: walletAddress + lockedAmount + tier + verifiedAt + expiresAt
 * - On HMAC mismatch → return FREE, NEVER return a higher tier
 * - expiresAt = verifiedAt + 30 days
 */
@Singleton
class IfrTierRepositoryImpl @Inject constructor(
    private val dao: IfrTierCacheDao,
    private val keystoreManager: KeystoreManager
) : IfrTierRepository {

    companion object {
        private const val HMAC_KEY_ALIAS = "chameleon_ifr_tier_hmac"
        private const val EXPIRY_DAYS = 30L
        private const val SECONDS_PER_DAY = 86400L
    }

    override suspend fun getCachedTier(): IfrTier {
        val entity = dao.getCurrent() ?: return IfrTier.FREE

        if (!validateHmac(entity)) {
            // HMAC mismatch — tampered data. Return FREE, never higher.
            dao.deleteAll()
            return IfrTier.FREE
        }

        val now = Instant.now().epochSecond
        if (entity.expiresAt <= now) {
            // Cache expired — triggers re-verification
            return IfrTier.FREE
        }

        return try {
            IfrTier.valueOf(entity.tier)
        } catch (e: IllegalArgumentException) {
            IfrTier.FREE
        }
    }

    override suspend fun getCachedResult(): CachedTierResult? {
        val entity = dao.getCurrent() ?: return null

        if (!validateHmac(entity)) {
            dao.deleteAll()
            return null
        }

        return CachedTierResult(
            walletAddress = entity.walletAddress,
            lockedAmount = entity.lockedAmount,
            tier = try { IfrTier.valueOf(entity.tier) } catch (_: Exception) { IfrTier.FREE },
            verifiedAt = Instant.ofEpochSecond(entity.verifiedAt),
            expiresAt = Instant.ofEpochSecond(entity.expiresAt),
            hmac = entity.hmac
        )
    }

    override suspend fun saveTierResult(walletAddress: String, lockedAmount: Long, tier: IfrTier) {
        val verifiedAt = Instant.now().epochSecond
        val expiresAt = verifiedAt + (EXPIRY_DAYS * SECONDS_PER_DAY)

        val hmac = computeHmac(walletAddress, lockedAmount, tier.name, verifiedAt, expiresAt)

        val entity = IfrTierCacheEntity(
            walletAddress = walletAddress,
            lockedAmount = lockedAmount,
            tier = tier.name,
            verifiedAt = verifiedAt,
            expiresAt = expiresAt,
            hmac = hmac
        )

        dao.deleteAll()
        dao.upsert(entity)
    }

    override suspend fun invalidateCache() {
        dao.deleteAll()
    }

    override suspend fun isCacheValid(): Boolean {
        val entity = dao.getCurrent() ?: return false
        if (!validateHmac(entity)) return false
        return entity.expiresAt > Instant.now().epochSecond
    }

    private fun validateHmac(entity: IfrTierCacheEntity): Boolean {
        return try {
            val expected = computeHmac(
                entity.walletAddress,
                entity.lockedAmount,
                entity.tier,
                entity.verifiedAt,
                entity.expiresAt
            )
            expected.contentEquals(entity.hmac)
        } catch (e: Exception) {
            false
        }
    }

    private fun computeHmac(
        walletAddress: String,
        lockedAmount: Long,
        tier: String,
        verifiedAt: Long,
        expiresAt: Long
    ): ByteArray {
        val hmacKey = keystoreManager.getOrCreateHmacKey(HMAC_KEY_ALIAS)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)

        // Deterministic byte order: wallet + amount + tier + verifiedAt + expiresAt
        mac.update(walletAddress.toByteArray(StandardCharsets.UTF_8))
        mac.update(ByteBuffer.allocate(8).putLong(lockedAmount).array())
        mac.update(tier.toByteArray(StandardCharsets.UTF_8))
        mac.update(ByteBuffer.allocate(8).putLong(verifiedAt).array())
        mac.update(ByteBuffer.allocate(8).putLong(expiresAt).array())

        return mac.doFinal()
    }
}
