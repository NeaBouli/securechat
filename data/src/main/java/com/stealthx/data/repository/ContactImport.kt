/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.data.repository

/**
 * Where a contact bundle entered the app. Set only by the trusted local entry point
 * (relay handler, deep-link router, camera scanner, NFC reader), never from a bundle field.
 */
enum class ImportProvenance {
    /** Relay CONTACT_EXCHANGE, deep link, pasted or edited text. */
    REMOTE,

    /** Camera QR scan or NFC tap performed by the user. */
    IN_PERSON
}

enum class ContactImportResult {
    /** New contact whose sxId is derived from its Ed25519 key. */
    ADDED,

    /** New legacy contact saved after explicit consent; not key-bound. */
    ADDED_LEGACY,

    /** Contact already stored with the same Ed25519 identity key; nothing written. */
    ALREADY_KNOWN
}

/** Remote import of an sxId that is not derived from the bundle's Ed25519 key. */
class UnboundIdentityException : IllegalArgumentException(
    "This SecureID is not bound to its key. Add it in person with a QR scan or NFC."
)

/** In-person import of an unbound legacy sxId without the user's explicit consent. */
class LegacyConsentRequiredException : IllegalStateException(
    "Legacy SecureID: explicit confirmation required before saving."
)

/** A known contact presented a different Ed25519 identity key. */
class IdentityKeyChangedException : SecurityException(
    "Identity key changed for a known contact. Import rejected."
)
