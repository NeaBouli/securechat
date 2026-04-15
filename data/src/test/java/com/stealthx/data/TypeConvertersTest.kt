/*
 * Chameleon — Data Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data

import com.stealthx.domain.rules.ActionType
import com.stealthx.domain.rules.TriggerType
import com.stealthx.shared.model.IfrTier
import com.stealthx.shared.model.SecurityLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("ChameleonTypeConverters")
class TypeConvertersTest {

    private val converters = ChameleonTypeConverters()

    @Test
    @DisplayName("Instant roundtrip preserves epoch second")
    fun `instant roundtrip`() {
        val now = Instant.now()
        val epoch = converters.fromInstant(now)
        val restored = converters.toInstant(epoch)
        assertEquals(now.epochSecond, restored?.epochSecond)
    }

    @Test
    @DisplayName("Null instant converts to null")
    fun `null instant`() {
        assertNull(converters.fromInstant(null))
        assertNull(converters.toInstant(null))
    }

    @Test
    @DisplayName("SecurityLevel roundtrip")
    fun `security level roundtrip`() {
        SecurityLevel.entries.forEach { level ->
            val name = converters.fromSecurityLevel(level)
            val restored = converters.toSecurityLevel(name)
            assertEquals(level, restored)
        }
    }

    @Test
    @DisplayName("IfrTier roundtrip")
    fun `ifr tier roundtrip`() {
        IfrTier.entries.forEach { tier ->
            val name = converters.fromIfrTier(tier)
            val restored = converters.toIfrTier(name)
            assertEquals(tier, restored)
        }
    }

    @Test
    @DisplayName("TriggerType roundtrip")
    fun `trigger type roundtrip`() {
        TriggerType.entries.forEach { type ->
            val name = converters.fromTriggerType(type)
            val restored = converters.toTriggerType(name)
            assertEquals(type, restored)
        }
    }

    @Test
    @DisplayName("ActionType roundtrip")
    fun `action type roundtrip`() {
        ActionType.entries.forEach { type ->
            val name = converters.fromActionType(type)
            val restored = converters.toActionType(name)
            assertEquals(type, restored)
        }
    }

    @Test
    @DisplayName("IfrTier thresholds are correct")
    fun `ifr tier thresholds`() {
        assertEquals(0L, IfrTier.FREE.minLockAmount)
        assertEquals(2_000_000_000_000L, IfrTier.PRO.minLockAmount)
        assertEquals(6_000_000_000_000L, IfrTier.ELITE.minLockAmount)
    }

    @Test
    @DisplayName("SecurityLevel ordering")
    fun `security level order`() {
        assertTrue(SecurityLevel.PUBLIC.ordinal < SecurityLevel.PROTECTED.ordinal)
        assertTrue(SecurityLevel.PROTECTED.ordinal < SecurityLevel.PRIVATE.ordinal)
        assertTrue(SecurityLevel.PRIVATE.ordinal < SecurityLevel.CAMOUFLAGE.ordinal)
    }
}
