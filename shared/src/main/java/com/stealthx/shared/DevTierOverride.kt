/*
 * SecureChat — Dev-only tier override
 * NEVER referenced in release builds — guarded by BuildConfig.DEBUG && BuildConfig.FORCE_ELITE
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.shared

import com.stealthx.shared.model.AccessTier

/**
 * Debug-only flag that bypasses HMAC cache and on-chain checks.
 * Set to true in Application.onCreate() when DEBUG && FORCE_ELITE.
 * Has zero effect in release builds (FORCE_ELITE = false).
 */
object DevTierOverride {
    var forceElite: Boolean = false
    var forcedTier: AccessTier? = null
}
