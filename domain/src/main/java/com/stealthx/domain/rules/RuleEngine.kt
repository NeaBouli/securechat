/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.domain.rules

import com.stealthx.shared.model.SecurityLevel
import com.stealthx.shared.model.TriggerContext
/**
 * Rule Engine — evaluates active rules against current context.
 *
 * Conflict Resolution (Fail Secure):
 *   When multiple rules match, the HIGHEST security level ALWAYS wins.
 *   There is no priority inversion. A user can temporarily override
 *   downward (e.g. "relax for 30 minutes") but never bypass upward.
 *
 * Default: If no rules match → PROTECTED (never PUBLIC by default).
 */
class RuleEngine {

    /**
     * Evaluate a list of active rules against the current context.
     * Returns the highest matching SecurityLevel.
     *
     * @param rules    Active rules from RuleRepository
     * @param context  Current trigger context (app, wifi, location, time)
     * @return         Resolved SecurityLevel (always the maximum)
     */
    fun evaluate(rules: List<SecureRule>, context: TriggerContext): SecurityLevel {
        val matchingRules = rules
            .filter { it.isEnabled }
            .filter { matches(it, context) }

        return resolveConflicts(matchingRules)
    }

    /**
     * Resolve conflicts: highest security level wins.
     * Default: PROTECTED (Fail Secure — never PUBLIC without explicit rule).
     */
    fun resolveConflicts(rules: List<SecureRule>): SecurityLevel {
        return rules
            .maxByOrNull { it.securityLevel.ordinal }
            ?.securityLevel
            ?: SecurityLevel.PROTECTED  // Fail Secure default
    }

    /**
     * Check if a rule matches the current context.
     */
    private fun matches(rule: SecureRule, context: TriggerContext): Boolean {
        return when (rule.triggerType) {
            TriggerType.APP ->
                context.packageName != null &&
                context.packageName == rule.triggerValue

            TriggerType.WIFI ->
                context.wifiSsid != null &&
                context.wifiSsid == rule.triggerValue

            TriggerType.LOCATION -> {
                val lat = context.latitude
                val lng = context.longitude
                lat != null && lng != null &&
                isWithinGeofence(lat, lng, rule.triggerValue)
            }

            TriggerType.TIME ->
                isWithinTimeWindow(context.hourOfDay, context.dayOfWeek, rule.triggerValue)

            TriggerType.BLUETOOTH ->
                context.bluetoothId != null &&
                context.bluetoothId == rule.triggerValue
        }
    }

    /**
     * Check if coordinates are within a geofence radius.
     * Uses Haversine formula for great-circle distance.
     *
     * @param lat      Current latitude
     * @param lng      Current longitude
     * @param geoJson  JSON: {"lat":X, "lng":Y, "radius":Z} (radius in meters)
     */
    private fun isWithinGeofence(lat: Double, lng: Double, geoJson: String): Boolean {
        return try {
            val parts = geoJson.replace("{", "").replace("}", "").replace("\"", "")
            val map = parts.split(",").associate {
                val (k, v) = it.trim().split(":")
                k.trim() to v.trim()
            }
            val targetLat = map["lat"]?.toDouble() ?: return false
            val targetLng = map["lng"]?.toDouble() ?: return false
            val radius = map["radius"]?.toDouble() ?: return false

            haversineDistance(lat, lng, targetLat, targetLng) <= radius
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Haversine distance between two points in meters.
     */
    internal fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Check if current time is within a time window.
     *
     * @param hour     Current hour (0-23)
     * @param day      Current day of week (1=Mon, 7=Sun)
     * @param timeJson JSON: {"startHour":X, "endHour":Y, "days":[1,2,3,4,5]}
     */
    private fun isWithinTimeWindow(hour: Int, day: Int, timeJson: String): Boolean {
        return try {
            val clean = timeJson.replace("\"", "").replace(" ", "")

            // Extract days array
            val daysMatch = Regex("days:\\[([^]]*)]").find(clean)
            val days = daysMatch?.groupValues?.get(1)
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()

            // Extract startHour and endHour
            val startMatch = Regex("startHour:(\\d+)").find(clean)
            val endMatch = Regex("endHour:(\\d+)").find(clean)
            val startHour = startMatch?.groupValues?.get(1)?.toIntOrNull() ?: return false
            val endHour = endMatch?.groupValues?.get(1)?.toIntOrNull() ?: return false

            val hourInRange = if (startHour <= endHour) {
                hour in startHour..endHour
            } else {
                hour >= startHour || hour <= endHour
            }

            hourInRange && (days.isEmpty() || day in days)
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Domain model for a secure rule.
 * Stored in Room via :data layer.
 */
data class SecureRule(
    val id:            String,          // UUID as String
    val name:          String,
    val triggerType:   TriggerType,
    val triggerValue:  String,          // JSON-encoded trigger parameters
    val actionType:    ActionType,
    val securityLevel: SecurityLevel,
    val priority:      Int,             // Higher = more important (for future use)
    val isEnabled:     Boolean,
    val createdAt:     Long,            // epoch seconds
    val lastTriggered: Long?,
    val triggerCount:  Int
)

enum class TriggerType { APP, LOCATION, TIME, WIFI, BLUETOOTH }
enum class ActionType  { SET_LEVEL, AUTO_ENCRYPT, SHOW_DECOY }
