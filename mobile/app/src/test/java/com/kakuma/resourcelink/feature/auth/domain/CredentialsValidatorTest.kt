package com.kakuma.resourcelink.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialsValidatorTest {

    @Test fun `email is required`() {
        assertEquals(FieldError.REQUIRED, CredentialsValidator.email(""))
        assertEquals(FieldError.REQUIRED, CredentialsValidator.email("   "))
    }

    @Test fun `malformed emails are rejected`() {
        listOf("amina", "amina@", "@example.com", "amina@example", "a b@example.com").forEach {
            assertEquals(it, FieldError.INVALID_EMAIL, CredentialsValidator.email(it))
        }
    }

    @Test fun `valid email passes and surrounding spaces are tolerated`() {
        assertNull(CredentialsValidator.email("amina.hassan+test@demo.kakuma.test"))
        assertNull(CredentialsValidator.email("  amina@example.com "))
    }

    @Test fun `login password only needs to be present`() {
        assertEquals(FieldError.REQUIRED, CredentialsValidator.loginPassword(""))
        assertNull(CredentialsValidator.loginPassword("x"))
    }

    @Test fun `new password enforces minimum length`() {
        assertEquals(FieldError.REQUIRED, CredentialsValidator.newPassword(""))
        assertEquals(FieldError.PASSWORD_TOO_SHORT, CredentialsValidator.newPassword("a".repeat(CredentialsValidator.MIN_PASSWORD - 1)))
        assertNull(CredentialsValidator.newPassword("a".repeat(CredentialsValidator.MIN_PASSWORD)))
    }

    @Test fun `full name needs at least two characters`() {
        assertEquals(FieldError.REQUIRED, CredentialsValidator.fullName(" "))
        assertEquals(FieldError.NAME_TOO_SHORT, CredentialsValidator.fullName(" A "))
        assertNull(CredentialsValidator.fullName("Al"))
    }
}
