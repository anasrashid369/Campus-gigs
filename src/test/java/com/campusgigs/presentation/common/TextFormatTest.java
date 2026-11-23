package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextFormatTest {

    @Test
    void shortTextsAreUnchanged() {
        assertEquals("Short text", TextFormat.oneLine("Short text", 60));
        assertEquals("exactly10!", TextFormat.oneLine("exactly10!", 10));
    }

    @Test
    void lineBreaksAndRepeatedSpacesBecomeSingleSpaces() {
        assertEquals("Two lines now one", TextFormat.oneLine("Two\n lines   now\r\none", 60));
    }

    @Test
    void longTextsAreCutWithAnEllipsisAtExactlyTheLimit() {
        String cut = TextFormat.oneLine("word ".repeat(40), 60);

        assertEquals(60, cut.length());
        assertEquals('…', cut.charAt(59));
    }
}
