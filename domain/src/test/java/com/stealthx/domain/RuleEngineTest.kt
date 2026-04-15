/*
 * Chameleon — Domain Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain

import com.stealthx.domain.rules.ActionType
import com.stealthx.domain.rules.RuleEngine
import com.stealthx.domain.rules.SecureRule
import com.stealthx.domain.rules.TriggerType
import com.stealthx.shared.model.SecurityLevel
import com.stealthx.shared.model.TriggerContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("RuleEngine")
class RuleEngineTest {

    private val engine = RuleEngine()

    @Test
    @DisplayName("Empty rules list defaults to PROTECTED (Fail Secure)")
    fun `empty rules returns protected`() {
        val result = engine.evaluate(emptyList(), TriggerContext())
        assertEquals(SecurityLevel.PROTECTED, result)
    }

    @Test
    @DisplayName("Highest security level wins conflict resolution")
    fun `highest level wins`() {
        val rules = listOf(
            createRule("1", TriggerType.APP, "com.whatsapp", SecurityLevel.PUBLIC),
            createRule("2", TriggerType.APP, "com.whatsapp", SecurityLevel.CAMOUFLAGE),
            createRule("3", TriggerType.APP, "com.whatsapp", SecurityLevel.PRIVATE)
        )
        val context = TriggerContext(packageName = "com.whatsapp")
        val result = engine.evaluate(rules, context)
        assertEquals(SecurityLevel.CAMOUFLAGE, result)
    }

    @Test
    @DisplayName("Disabled rules are ignored")
    fun `disabled rules skipped`() {
        val rules = listOf(
            createRule("1", TriggerType.APP, "com.whatsapp", SecurityLevel.CAMOUFLAGE, isEnabled = false),
            createRule("2", TriggerType.APP, "com.whatsapp", SecurityLevel.PUBLIC)
        )
        val context = TriggerContext(packageName = "com.whatsapp")
        val result = engine.evaluate(rules, context)
        assertEquals(SecurityLevel.PUBLIC, result)
    }

    @Test
    @DisplayName("APP trigger matches packageName")
    fun `app trigger matches`() {
        val rules = listOf(createRule("1", TriggerType.APP, "com.whatsapp", SecurityLevel.PRIVATE))
        val context = TriggerContext(packageName = "com.whatsapp")
        assertEquals(SecurityLevel.PRIVATE, engine.evaluate(rules, context))
    }

    @Test
    @DisplayName("APP trigger does not match different package")
    fun `app trigger no match`() {
        val rules = listOf(createRule("1", TriggerType.APP, "com.whatsapp", SecurityLevel.PRIVATE))
        val context = TriggerContext(packageName = "com.telegram")
        assertEquals(SecurityLevel.PROTECTED, engine.evaluate(rules, context))
    }

    @Test
    @DisplayName("WIFI trigger matches SSID")
    fun `wifi trigger matches`() {
        val rules = listOf(createRule("1", TriggerType.WIFI, "OfficeNet", SecurityLevel.CAMOUFLAGE))
        val context = TriggerContext(wifiSsid = "OfficeNet")
        assertEquals(SecurityLevel.CAMOUFLAGE, engine.evaluate(rules, context))
    }

    @Test
    @DisplayName("TIME trigger matches hour and day")
    fun `time trigger matches`() {
        val json = """{"startHour":9, "endHour":17, "days":[1,2,3,4,5]}"""
        val rules = listOf(createRule("1", TriggerType.TIME, json, SecurityLevel.PRIVATE))
        val context = TriggerContext(hourOfDay = 12, dayOfWeek = 3)
        assertEquals(SecurityLevel.PRIVATE, engine.evaluate(rules, context))
    }

    @Test
    @DisplayName("TIME trigger does not match weekend")
    fun `time trigger no match weekend`() {
        val json = """{"startHour":9, "endHour":17, "days":[1,2,3,4,5]}"""
        val rules = listOf(createRule("1", TriggerType.TIME, json, SecurityLevel.PRIVATE))
        val context = TriggerContext(hourOfDay = 12, dayOfWeek = 6)
        assertEquals(SecurityLevel.PROTECTED, engine.evaluate(rules, context))
    }

    @Test
    @DisplayName("Haversine distance calculation")
    fun `haversine distance correct`() {
        // Berlin to Munich ~ 504 km
        val dist = engine.haversineDistance(52.52, 13.405, 48.1351, 11.5820)
        assertTrue(dist > 500_000 && dist < 510_000, "Berlin-Munich should be ~504km, got ${dist/1000}km")
    }

    @Test
    @DisplayName("LOCATION trigger matches within radius")
    fun `location trigger within radius`() {
        val json = """{"lat":48.1351, "lng":11.5820, "radius":1000}"""
        val rules = listOf(createRule("1", TriggerType.LOCATION, json, SecurityLevel.CAMOUFLAGE))
        val context = TriggerContext(latitude = 48.1355, longitude = 11.5825) // ~50m away
        assertEquals(SecurityLevel.CAMOUFLAGE, engine.evaluate(rules, context))
    }

    @Test
    @DisplayName("LOCATION trigger outside radius")
    fun `location trigger outside radius`() {
        val json = """{"lat":48.1351, "lng":11.5820, "radius":100}"""
        val rules = listOf(createRule("1", TriggerType.LOCATION, json, SecurityLevel.CAMOUFLAGE))
        val context = TriggerContext(latitude = 52.52, longitude = 13.405) // Berlin, far away
        assertEquals(SecurityLevel.PROTECTED, engine.evaluate(rules, context))
    }

    private fun createRule(
        id: String,
        triggerType: TriggerType,
        triggerValue: String,
        securityLevel: SecurityLevel,
        isEnabled: Boolean = true
    ): SecureRule {
        return SecureRule(
            id = id,
            name = "Test Rule $id",
            triggerType = triggerType,
            triggerValue = triggerValue,
            actionType = ActionType.SET_LEVEL,
            securityLevel = securityLevel,
            priority = 0,
            isEnabled = isEnabled,
            createdAt = Instant.now().epochSecond,
            lastTriggered = null,
            triggerCount = 0
        )
    }
}
