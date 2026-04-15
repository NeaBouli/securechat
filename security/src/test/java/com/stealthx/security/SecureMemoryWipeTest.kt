/*
 * Chameleon — Security Unit Tests
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

@DisplayName("SecureMemoryWipe")
class SecureMemoryWipeTest {

    @Test
    @DisplayName("wipeByteArray zeroes all bytes")
    fun `wipeByteArray clears all data`() {
        val data = byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45)
        SecureMemoryWipe.wipeByteArray(data)
        assertTrue(data.all { it == 0.toByte() })
    }

    @Test
    @DisplayName("wipeByteArray handles empty array")
    fun `wipeByteArray on empty array does not throw`() {
        assertDoesNotThrow { SecureMemoryWipe.wipeByteArray(ByteArray(0)) }
    }

    @Test
    @DisplayName("wipeCharArray zeroes all chars")
    fun `wipeCharArray clears all data`() {
        val data = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        SecureMemoryWipe.wipeCharArray(data)
        assertTrue(data.all { it == '\u0000' })
    }

    @Test
    @DisplayName("wipeCharArray handles empty array")
    fun `wipeCharArray on empty array does not throw`() {
        assertDoesNotThrow { SecureMemoryWipe.wipeCharArray(CharArray(0)) }
    }

    @Test
    @DisplayName("wipeIntArray zeroes all ints")
    fun `wipeIntArray clears all data`() {
        val data = intArrayOf(1, 2, 3, 42, 99)
        SecureMemoryWipe.wipeIntArray(data)
        assertTrue(data.all { it == 0 })
    }

    @Test
    @DisplayName("wipeByteArray handles large array (1MB)")
    fun `wipeByteArray handles large array`() {
        val data = ByteArray(1024 * 1024) { 0xFF.toByte() }
        SecureMemoryWipe.wipeByteArray(data)
        assertTrue(data.all { it == 0.toByte() })
    }

    @Test
    @DisplayName("secureDelete removes file after 3-pass wipe")
    fun `secureDelete wipes and deletes file`() {
        val tempFile = File.createTempFile("chameleon_test_", ".bin")
        tempFile.writeBytes(ByteArray(4096) { 0x42 })
        assertTrue(tempFile.exists())

        val result = SecureMemoryWipe.secureDelete(tempFile)

        assertTrue(result)
        assertFalse(tempFile.exists())
    }

    @Test
    @DisplayName("secureDelete on non-existent file returns true")
    fun `secureDelete on missing file returns true`() {
        val missing = File("/tmp/chameleon_nonexistent_${System.nanoTime()}")
        assertTrue(SecureMemoryWipe.secureDelete(missing))
    }

    @Test
    @DisplayName("secureDelete handles small file (1 byte)")
    fun `secureDelete handles tiny file`() {
        val tempFile = File.createTempFile("chameleon_tiny_", ".bin")
        tempFile.writeBytes(byteArrayOf(0x01))

        val result = SecureMemoryWipe.secureDelete(tempFile)
        assertTrue(result)
        assertFalse(tempFile.exists())
    }

    @Test
    @DisplayName("secureDeleteDirectory removes all files recursively")
    fun `secureDeleteDirectory cleans nested structure`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "chameleon_test_dir_${System.nanoTime()}")
        tempDir.mkdirs()
        val subDir = File(tempDir, "sub")
        subDir.mkdirs()

        File(tempDir, "a.txt").writeText("secret A")
        File(subDir, "b.txt").writeText("secret B")
        File(subDir, "c.dat").writeBytes(ByteArray(8192) { 0xAA.toByte() })

        val result = SecureMemoryWipe.secureDeleteDirectory(tempDir)

        assertTrue(result)
        assertFalse(File(tempDir, "a.txt").exists())
        assertFalse(File(subDir, "b.txt").exists())
        assertFalse(File(subDir, "c.dat").exists())
    }
}
