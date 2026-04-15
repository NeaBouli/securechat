/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.security

import java.io.File
import java.io.RandomAccessFile
import java.util.Arrays

/**
 * Secure memory and file wiping utilities.
 *
 * Prevents sensitive data from lingering in RAM or storage
 * after it is no longer needed.
 *
 * File wipe: DoD 5220.22-M (3 passes)
 *   Pass 1: 0x00 (zeros)
 *   Pass 2: 0xFF (ones)
 *   Pass 3: random bytes
 */
object SecureMemoryWipe {

    /**
     * Overwrite byte array with zeros.
     * Call on all key material, passwords, and plaintext after use.
     */
    fun wipeByteArray(data: ByteArray) {
        Arrays.fill(data, 0.toByte())
    }

    /**
     * Overwrite char array with null characters.
     * Call on all password char arrays after use.
     */
    fun wipeCharArray(data: CharArray) {
        Arrays.fill(data, '\u0000')
    }

    /**
     * Overwrite an int array with zeros.
     */
    fun wipeIntArray(data: IntArray) {
        Arrays.fill(data, 0)
    }

    /**
     * Securely delete a file using DoD 5220.22-M 3-pass wipe.
     *
     * @param file  File to securely delete
     * @return      true if wiped and deleted successfully
     */
    fun secureDelete(file: File): Boolean {
        if (!file.exists()) return true

        return try {
            val length = file.length()
            RandomAccessFile(file, "rws").use { raf ->
                // Pass 1: zeros
                raf.seek(0)
                val zeros = ByteArray(minOf(length, 4096L).toInt()) { 0x00 }
                writePass(raf, length, zeros)

                // Pass 2: ones
                raf.seek(0)
                val ones = ByteArray(minOf(length, 4096L).toInt()) { 0xFF.toByte() }
                writePass(raf, length, ones)

                // Pass 3: random
                raf.seek(0)
                val random = ByteArray(minOf(length, 4096L).toInt())
                java.security.SecureRandom().nextBytes(random)
                writePass(raf, length, random)

                wipeByteArray(zeros)
                wipeByteArray(ones)
                wipeByteArray(random)
            }
            file.delete()
        } catch (e: Exception) {
            file.delete() // best-effort fallback
            false
        }
    }

    private fun writePass(raf: RandomAccessFile, fileLength: Long, pattern: ByteArray) {
        var remaining = fileLength
        while (remaining > 0) {
            val chunk = minOf(remaining, pattern.size.toLong()).toInt()
            raf.write(pattern, 0, chunk)
            remaining -= chunk
        }
        raf.fd.sync()
    }

    /**
     * Wipe a directory and all its contents securely.
     * Recurses into subdirectories.
     */
    fun secureDeleteDirectory(dir: File): Boolean {
        if (!dir.isDirectory) return secureDelete(dir)
        var success = true
        dir.walkBottomUp().forEach { file ->
            if (file.isFile) {
                if (!secureDelete(file)) success = false
            } else {
                file.delete()
            }
        }
        return success
    }
}
