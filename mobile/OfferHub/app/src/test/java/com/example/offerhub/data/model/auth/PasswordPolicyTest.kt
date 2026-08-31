package com.example.offerhub.data.model.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {
    @Test
    fun `valid password satisfies every requirement`() {
        assertTrue(PasswordPolicy.isValid("NewPassword1!"))
    }

    @Test
    fun `password shorter than eight characters is rejected`() {
        assertFalse(PasswordPolicy.isValid("Short1!"))
    }

    @Test
    fun `password without uppercase letter is rejected`() {
        assertFalse(PasswordPolicy.isValid("password1!"))
    }

    @Test
    fun `password without number is rejected`() {
        assertFalse(PasswordPolicy.isValid("Password!"))
    }

    @Test
    fun `password without special character is rejected`() {
        assertFalse(PasswordPolicy.isValid("Password1"))
    }
}
