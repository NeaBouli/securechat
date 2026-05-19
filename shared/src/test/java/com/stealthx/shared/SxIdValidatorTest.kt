package com.stealthx.shared

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SxIdValidatorTest {

    @Test
    fun validId_passes() {
        assertTrue(SxIdValidator.isValid("sx_a7Kx9mPq2"))
        assertTrue(SxIdValidator.isValid("sx_111111111"))
        assertTrue(SxIdValidator.isValid("sx_ZZZZZZZZZ"))
        assertTrue(SxIdValidator.isValid("sx_abcdefghj"))
    }

    @Test
    fun wrongPrefix_fails() {
        assertFalse(SxIdValidator.isValid("SX_a7Kx9mPq2"))
        assertFalse(SxIdValidator.isValid("stealthx_a7Kx9mPq2"))
        assertFalse(SxIdValidator.isValid("a7Kx9mPq2"))
    }

    @Test
    fun tooShort_fails() {
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mP"))   // 8 body chars
        assertFalse(SxIdValidator.isValid("sx_"))
        assertFalse(SxIdValidator.isValid("sx_a"))
    }

    @Test
    fun tooLong_fails() {
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mPq2n"))  // 10 body chars
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mPq2nRt"))
    }

    @Test
    fun invalidBase58Chars_fail() {
        // Base58 excludes: 0 (zero), O (capital-O), I (capital-I), l (lowercase-L)
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mPq0")) // '0'
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mPqO")) // 'O'
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mPqI")) // 'I'
        assertFalse(SxIdValidator.isValid("sx_a7Kx9mPql")) // 'l'
    }

    @Test
    fun emptyOrBlank_fails() {
        assertFalse(SxIdValidator.isValid(""))
        assertFalse(SxIdValidator.isValid("   "))
    }

    @Test
    fun requireValid_throwsOnInvalid() {
        val ex = assertThrows<IllegalArgumentException> {
            SxIdValidator.requireValid("bad_id")
        }
        assertTrue(ex.message!!.contains("bad_id"))
    }

    @Test
    fun requireValid_returnsIdOnValid() {
        val id = "sx_a7Kx9mPq2"
        val result = SxIdValidator.requireValid(id)
        assertTrue(result == id)
    }
}
