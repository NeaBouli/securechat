/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

import com.stealthx.data.dao.AccessTierCacheDao
import com.stealthx.data.entity.AccessTierCacheEntity
import com.stealthx.domain.repository.AccessTierRepository
import com.stealthx.security.KeystoreManager
import com.stealthx.shared.DevTierOverride
import com.stealthx.shared.model.CachedTierResult
import com.stealthx.shared.model.AccessTier
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App tier cache with HMAC-SHA256 tamper protection.
 *
 * CRITICAL SECURITY:
 * - HMAC key from KeystoreManager (hardware-backed)
 * - HMAC computed over: sourceId + accessWeight + tier + verifiedAt + expiresAt
 * - On HMAC mismatch → return FREE, NEVER return a higher tier
 * - expiresAt = verifiedAt + 30 days
 */
@Singleton
class AccessTierRepositoryImpl @Inject constructor(
    private val dao: AccessTierCacheDao,
    private val keystoreManager: KeystoreManager
) : AccessTierRepository {

    companion object {
        private const val HMAC_KEY_ALIAS = "securechat_access_tier_hmac"
        private const val EXPIRY_DAYS = 30L
        private const val SECONDS_PER_DAY = 86400L
    }

    override suspend fun getCachedTier(): AccessTier {
        DevTierOverride.forcedTier?.let { return it }
        if (DevTierOverride.forceElite) return AccessTier.ELITE
        val entity = dao.getCurrent() ?: return AccessTier.FREE

        if (!validateHmac(entity)) {
            // HMAC mismatch — tampered data. Return FREE, never higher.
            dao.deleteAll()
            return AccessTier.FREE
        }

        val now = Instant.now().epochSecond
        if (entity.expiresAt <= now) {
            // Cache expired — triggers re-verification
            return AccessTier.FREE
        }

        return try {
            AccessTier.valueOf(entity.tier)
        } catch (e: IllegalArgumentException) {
            AccessTier.FREE
        }
    }

    override suspend fun getCachedResult(): CachedTierResult? {
        val entity = dao.getCurrent() ?: return null

        if (!validateHmac(entity)) {
            dao.deleteAll()
            return null
        }

        return CachedTierResult(
            sourceId = entity.sourceId,
            accessWeight = entity.accessWeight,
            tier = try { AccessTier.valueOf(entity.tier) } catch (_: Exception) { AccessTier.FREE },
            verifiedAt = Instant.ofEpochSecond(entity.verifiedAt),
            expiresAt = Instant.ofEpochSecond(entity.expiresAt),
            hmac = entity.hmac
        )
    }

    override suspend fun saveTierResult(
        sourceId: String,
        accessWeight: Long,
        tier: AccessTier,
        expiresAtEpochSeconds: Long?
    ) {
        val verifiedAt = Instant.now().epochSecond
        val maximumExpiry = verifiedAt + (EXPIRY_DAYS * SECONDS_PER_DAY)
        val expiresAt = expiresAtEpochSeconds?.coerceAtMost(maximumExpiry) ?: maximumExpiry
        require(expiresAt > verifiedAt) { "Tier result is already expired" }

        val hmac = computeHmac(sourceId, accessWeight, tier.name, verifiedAt, expiresAt)

        val entity = AccessTierCacheEntity(
            sourceId = sourceId,
            accessWeight = accessWeight,
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

    private fun validateHmac(entity: AccessTierCacheEntity): Boolean {
        return try {
            val expected = computeHmac(
                entity.sourceId,
                entity.accessWeight,
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
        sourceId: String,
        accessWeight: Long,
        tier: String,
        verifiedAt: Long,
        expiresAt: Long
    ): ByteArray {
        val hmacKey = keystoreManager.getOrCreateHmacKey(HMAC_KEY_ALIAS)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)

        // Deterministic byte order: source + access weight + tier + verifiedAt + expiresAt
        mac.update(sourceId.toByteArray(StandardCharsets.UTF_8))
        mac.update(ByteBuffer.allocate(8).putLong(accessWeight).array())
        mac.update(tier.toByteArray(StandardCharsets.UTF_8))
        mac.update(ByteBuffer.allocate(8).putLong(verifiedAt).array())
        mac.update(ByteBuffer.allocate(8).putLong(expiresAt).array())

        return mac.doFinal()
    }
}
