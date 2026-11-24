package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TextPromptTest {

    @Test
    void acceptsTextWithinTheLimits() {
        assertNull(TextPrompt.problem("1234567890", 10, 1000));
        assertNull(TextPrompt.problem("x".repeat(1000), 10, 1000));
        assertNull(TextPrompt.problem("  padded text here  ", 10, 1000));
    }

    @Test
    void rejectsTextThatIsTooShortOrTooLong() {
        assertEquals("Please write between 10 and 1000 characters.", TextPrompt.problem("123456789", 10, 1000));
        assertEquals("Please write between 10 and 1000 characters.", TextPrompt.problem("x".repeat(1001), 10, 1000));
        assertEquals("Please write between 10 and 1000 characters.", TextPrompt.problem(null, 10, 1000));
    }

    @Test
    void blankTextCountsAsEmpty() {
        assertEquals("Please write between 1 and 1000 characters.", TextPrompt.problem("     ", 1, 1000));
        assertNull(TextPrompt.problem(" a ", 1, 1000));
    }
}
