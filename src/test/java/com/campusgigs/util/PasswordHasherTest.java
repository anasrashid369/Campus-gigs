package com.campusgigs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void samePasswordVerifies() {
        String stored = PasswordHasher.hash("Secret123");

        assertTrue(PasswordHasher.verify("Secret123", stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordHasher.hash("Secret123");

        assertFalse(PasswordHasher.verify("secret123", stored));
        assertFalse(PasswordHasher.verify("", stored));
    }

    @Test
    void twoHashesOfSamePasswordDiffer() {
        assertNotEquals(PasswordHasher.hash("Secret123"), PasswordHasher.hash("Secret123"));
    }

    @Test
    void usesTheAgreedFormat() {
        String[] parts = PasswordHasher.hash("Secret123").split("\\$");

        assertEquals(4, parts.length);
        assertEquals("pbkdf2", parts[0]);
        assertEquals("120000", parts[1]);
        assertEquals(24, parts[2].length());
        assertEquals(44, parts[3].length());
    }

    @Test
    void malformedStoredValueReturnsFalse() {
        assertFalse(PasswordHasher.verify("Secret123", ""));
        assertFalse(PasswordHasher.verify("Secret123", "plain-text-password"));
        assertFalse(PasswordHasher.verify("Secret123", "pbkdf2$120000$onlythree"));
        assertFalse(PasswordHasher.verify("Secret123", "md5$120000$c2FsdA==$aGFzaA=="));
        assertFalse(PasswordHasher.verify("Secret123", "pbkdf2$abc$c2FsdA==$aGFzaA=="));
        assertFalse(PasswordHasher.verify("Secret123", "pbkdf2$-1$c2FsdA==$aGFzaA=="));
        assertFalse(PasswordHasher.verify("Secret123", "pbkdf2$120000$not*base64$aGFzaA=="));
        assertFalse(PasswordHasher.verify("Secret123", "pbkdf2$120000$c2FsdA==$aGFzaA=="));
        assertFalse(PasswordHasher.verify("Secret123", "pbkdf2$120000$c2FsdA==$aGFzaA==$extra"));
    }

    @Test
    void nullInputReturnsFalse() {
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("Secret123")));
        assertFalse(PasswordHasher.verify("Secret123", null));
    }

    @Test
    void hashRejectsNullPassword() {
        assertThrows(NullPointerException.class, () -> PasswordHasher.hash(null));
    }
}
