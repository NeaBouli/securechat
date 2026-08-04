package com.stealthx.securechat.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListenerRecoveryDecisionTest {

    @Test
    fun `boot completed starts the listener when enabled`() {
        assertTrue(
            ListenerRecoveryDecision.shouldStart(
                "android.intent.action.BOOT_COMPLETED",
                enabled = true
            )
        )
    }

    @Test
    fun `package replaced starts the listener when enabled`() {
        assertTrue(
            ListenerRecoveryDecision.shouldStart(
                "android.intent.action.MY_PACKAGE_REPLACED",
                enabled = true
            )
        )
    }

    @Test
    fun `boot completed does not start the listener when disabled`() {
        assertFalse(
            ListenerRecoveryDecision.shouldStart(
                "android.intent.action.BOOT_COMPLETED",
                enabled = false
            )
        )
    }

    @Test
    fun `package replaced does not start the listener when disabled`() {
        assertFalse(
            ListenerRecoveryDecision.shouldStart(
                "android.intent.action.MY_PACKAGE_REPLACED",
                enabled = false
            )
        )
    }

    @Test
    fun `unknown action never starts the listener when enabled`() {
        assertFalse(
            ListenerRecoveryDecision.shouldStart(
                "android.intent.action.TIMEZONE_CHANGED",
                enabled = true
            )
        )
    }

    @Test
    fun `unknown action never starts the listener when disabled`() {
        assertFalse(
            ListenerRecoveryDecision.shouldStart(
                "android.intent.action.TIMEZONE_CHANGED",
                enabled = false
            )
        )
    }

    @Test
    fun `null action never starts the listener when enabled`() {
        assertFalse(ListenerRecoveryDecision.shouldStart(null, enabled = true))
    }

    @Test
    fun `null action never starts the listener when disabled`() {
        assertFalse(ListenerRecoveryDecision.shouldStart(null, enabled = false))
    }
}
