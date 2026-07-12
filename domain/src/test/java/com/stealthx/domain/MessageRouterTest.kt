package com.stealthx.domain

import com.stealthx.domain.transport.MessageRouter
import com.stealthx.shared.model.EncryptedPayload
import com.stealthx.shared.model.RatchetMessage
import com.stealthx.transport.OnionRelayTransport
import com.stealthx.transport.RelayTransport
import com.stealthx.transport.TorRelayTransport
import com.stealthx.transport.TransportResult
import com.stealthx.transport.TransportType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class MessageRouterTest {

    private val message = RatchetMessage(
        dhPublicKey = ByteArray(32),
        counter = 1,
        prevCounter = 0,
        payload = EncryptedPayload(
            ciphertext = byteArrayOf(1),
            nonce = ByteArray(24),
            paddedLength = 1,
            aad = ByteArray(0),
            algorithm = "XChaCha20-Poly1305",
            version = 1
        )
    )

    @Test
    fun `central signaling relay is selected without being labeled Tor`() = runTest {
        val signaling = FakeTransport(TransportType.SIGNALING_RELAY, available = true)
        val local = FakeTransport(TransportType.LOCAL, available = true)
        val router = MessageRouter(mapOf(signaling.type to signaling, local.type to local))

        val result = router.send("sx_recipient", message)

        assertEquals(TransportType.SIGNALING_RELAY, router.getActiveTransportType())
        assertEquals(TransportType.SIGNALING_RELAY, (result as TransportResult.Delivered).transportType)
    }

    @Test
    fun `real privacy relays retain priority over signaling`() = runTest {
        val onion = FakeTransport(TransportType.ONION_RELAY, available = true)
        val tor = FakeTransport(TransportType.TOR_RELAY, available = true)
        val signaling = FakeTransport(TransportType.SIGNALING_RELAY, available = true)
        val router = MessageRouter(listOf(onion, tor, signaling).associateBy { it.type })

        assertEquals(TransportType.ONION_RELAY, router.getActiveTransportType())
    }

    @Test
    fun `relay-only delivery can use the central signaling relay`() = runTest {
        val signaling = FakeTransport(TransportType.SIGNALING_RELAY, available = true)
        val router = MessageRouter(mapOf(signaling.type to signaling))

        val result = router.sendRelayOnly("sx_recipient", message)

        assertInstanceOf(TransportResult.Delivered::class.java, result)
    }

    @Test
    fun `planned transports fail closed instead of throwing`() = runTest {
        val tor = TorRelayTransport()
        val onion = OnionRelayTransport()

        assertFalse(tor.connect())
        assertFalse(onion.connect())
        assertInstanceOf(TransportResult.Failed::class.java, tor.send("sx_recipient", message))
        assertInstanceOf(TransportResult.Failed::class.java, onion.send("sx_recipient", message))
        tor.disconnect()
        onion.disconnect()
    }

    private class FakeTransport(
        override val type: TransportType,
        private val available: Boolean
    ) : RelayTransport {
        override val isAvailable: Boolean get() = available

        override suspend fun send(recipientSxId: String, message: RatchetMessage): TransportResult =
            TransportResult.Delivered("test-message", type)

        override suspend fun connect(): Boolean = available
        override suspend fun disconnect() = Unit
    }
}
