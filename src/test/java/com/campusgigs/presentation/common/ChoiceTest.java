package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ChoiceTest {

    @Test
    void showsItsLabelInAComboBoxAndKeepsItsValue() {
        Choice<Integer> five = new Choice<>("Five", 5);

        assertEquals("Five", five.toString());
        assertEquals(5, five.value());
    }

    @Test
    void aNullValueStandsForAll() {
        assertNull(new Choice<String>("All", null).value());
    }
}
