/*
 * Chameleon — Data Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data

import com.stealthx.data.entity.AuditLogEntity
import com.stealthx.data.entity.CryptoKeyEntity
import com.stealthx.data.entity.SecureRuleEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("Room Entities")
class EntityTest {

    @Test
    @DisplayName("SecureRuleEntity stores all fields")
    fun `secure rule entity complete`() {
        val entity = SecureRuleEntity(
            id = "uuid-1",
            name = "Work WiFi Rule",
            triggerType = "WIFI",
            triggerValue = "OfficeNetwork",
            actionType = "SET_LEVEL",
            securityLevel = "PRIVATE",
            priority = 10,
            isEnabled = true,
            createdAt = Instant.now().epochSecond,
            lastTriggered = null,
            triggerCount = 0
        )
        assertEquals("uuid-1", entity.id)
        assertTrue(entity.isEnabled)
        assertEquals(0, entity.triggerCount)
    }

    @Test
    @DisplayName("CryptoKeyEntity marks revocation")
    fun `crypto key revocation`() {
        val key = CryptoKeyEntity(
            id = "key-1",
            keyType = "SYMMETRIC",
            algorithm = "XCHACHA20_POLY1305",
            publicKey = null,
            keystoreAlias = "ks_alias_1",
            createdAt = Instant.now().epochSecond,
            expiresAt = null,
            isRevoked = false
        )
        assertFalse(key.isRevoked)
        val revoked = key.copy(isRevoked = true)
        assertTrue(revoked.isRevoked)
    }

    @Test
    @DisplayName("AuditLogEntity autoGenerates id")
    fun `audit log auto id`() {
        val entry = AuditLogEntity(
            timestamp = Instant.now().epochSecond,
            action = "ENCRYPT",
            packageName = "com.whatsapp",
            securityLevel = "PRIVATE",
            details = "encrypted 42 chars"
        )
        assertEquals(0L, entry.id)
    }
}
