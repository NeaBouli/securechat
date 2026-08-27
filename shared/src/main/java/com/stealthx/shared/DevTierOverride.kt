/*
 * SecureChat — Dev-only tier override
 * Enabled only by debug and screenshot builds.
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.shared

import com.stealthx.shared.model.AccessTier

/**
 * Debug-only flag that bypasses the server-signed entitlement cache.
 * Every signable release build keeps ALLOW_TIER_OVERRIDE disabled.
 */
object DevTierOverride {
    var forceElite: Boolean = false
    var forcedTier: AccessTier? = null
}
