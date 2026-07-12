package com.stealthx.data.exchange

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PendingFrameBufferTest {

    @Test
    fun `offer rejects frames beyond capacity`() {
        val buffer = PendingFrameBuffer(maxSize = 2)

        assertTrue(buffer.offer("first"))
        assertTrue(buffer.offer("second"))
        assertFalse(buffer.offer("overflow"))
        assertEquals(listOf("first", "second"), buffer.snapshot())
    }

    @Test
    fun `failed drain restores the head and preserves order`() {
        val buffer = PendingFrameBuffer(maxSize = 4)
        buffer.offer("first")
        buffer.offer("second")
        buffer.offer("third")
        val attempts = AtomicInteger(0)

        val sent = buffer.drain { attempts.incrementAndGet() == 1 }

        assertEquals(1, sent)
        assertEquals(listOf("second", "third"), buffer.snapshot())
    }

    @Test
    fun `clear removes every pending frame`() {
        val buffer = PendingFrameBuffer(maxSize = 2)
        buffer.offer("first")
        buffer.offer("second")

        buffer.clear()

        assertEquals(emptyList<String>(), buffer.snapshot())
    }

    @Test
    fun `stop listening shares the send queue monitor`() {
        val method = ContactExchangeManager::class.java.getDeclaredMethod("stopListening")

        assertTrue(Modifier.isSynchronized(method.modifiers))
    }

    @Test
    fun `concurrent offers never exceed capacity`() {
        val capacity = 64
        val buffer = PendingFrameBuffer(maxSize = capacity)
        val accepted = AtomicInteger(0)
        val ready = CountDownLatch(8)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        repeat(8) { worker ->
            executor.submit {
                ready.countDown()
                start.await()
                repeat(32) { frame ->
                    if (buffer.offer("$worker:$frame")) accepted.incrementAndGet()
                }
            }
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS))
        start.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        assertEquals(capacity, accepted.get())
        assertEquals(capacity, buffer.snapshot().size)
    }
}
