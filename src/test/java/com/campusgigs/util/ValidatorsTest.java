package com.campusgigs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorsTest {

    @Test
    void blankCoversNullEmptyAndWhitespace() {
        assertTrue(Validators.isBlank(null));
        assertTrue(Validators.isBlank(""));
        assertTrue(Validators.isBlank("   \t"));
        assertFalse(Validators.isBlank(" a "));
    }

    @Test
    void acceptsNormalEmails() {
        assertTrue(Validators.isValidEmail("ali@uni.edu.pk"));
        assertTrue(Validators.isValidEmail("sara.khan+gigs@gmail.com"));
        assertTrue(Validators.isValidEmail("admin@campusgigs.local"));
    }

    @Test
    void rejectsBadEmails() {
        assertFalse(Validators.isValidEmail(null));
        assertFalse(Validators.isValidEmail(""));
        assertFalse(Validators.isValidEmail("ali"));
        assertFalse(Validators.isValidEmail("ali@"));
        assertFalse(Validators.isValidEmail("@uni.edu"));
        assertFalse(Validators.isValidEmail("ali@uni"));
        assertFalse(Validators.isValidEmail("ali uni@edu.pk"));
        assertFalse(Validators.isValidEmail(" ali@uni.edu "));
        assertFalse(Validators.isValidEmail("ali@@uni.edu"));
    }

    @Test
    void minLengthCountsCharacters() {
        assertTrue(Validators.hasMinLength("password1", 8));
        assertTrue(Validators.hasMinLength("12345678", 8));
        assertFalse(Validators.hasMinLength("1234567", 8));
        assertFalse(Validators.hasMinLength(null, 1));
        assertTrue(Validators.hasMinLength("", 0));
    }

    @Test
    void positiveNumberAcceptsWholeNumbersAboveZero() {
        assertTrue(Validators.isPositiveNumber("1"));
        assertTrue(Validators.isPositiveNumber("250"));
        assertTrue(Validators.isPositiveNumber(" 500 "));
    }

    @Test
    void positiveNumberRejectsEverythingElse() {
        assertFalse(Validators.isPositiveNumber(null));
        assertFalse(Validators.isPositiveNumber(""));
        assertFalse(Validators.isPositiveNumber("0"));
        assertFalse(Validators.isPositiveNumber("-5"));
        assertFalse(Validators.isPositiveNumber("12.5"));
        assertFalse(Validators.isPositiveNumber("abc"));
        assertFalse(Validators.isPositiveNumber("99999999999999999999"));
    }
}
