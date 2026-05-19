/*
 * StealthX Shared — sx_ ID Validator
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.shared

object SxIdValidator {

    // sx_ + exactly 9 Base58 chars (no 0, O, I, l) = 12 total
    val REGEX = Regex("^sx_[1-9A-HJ-NP-Za-km-z]{9}$")

    fun isValid(id: String): Boolean = REGEX.matches(id)

    fun requireValid(id: String): String {
        require(isValid(id)) { "Invalid sx_ ID: '$id' — expected format sx_[Base58]{9}" }
        return id
    }
}
