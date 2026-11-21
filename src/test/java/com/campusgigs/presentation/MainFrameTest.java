package com.campusgigs.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MainFrameTest {

    private MainFrame frame;

    @BeforeEach
    void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        frame = new MainFrame();
    }

    @Test
    void showsTheRequestedCard() {
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        frame.addCard("first", first);
        frame.addCard("second", second);

        frame.showCard("second");

        assertTrue(second.isVisible());
        assertFalse(first.isVisible());
    }

    @Test
    void rejectsDuplicateCardNames() {
        frame.addCard("wallet", new JPanel());

        assertThrows(IllegalArgumentException.class, () -> frame.addCard("wallet", new JPanel()));
    }

    @Test
    void rejectsUnknownCardName() {
        assertThrows(IllegalArgumentException.class, () -> frame.showCard("missing"));
        frame.addCard("home", new JPanel());
        assertDoesNotThrow(() -> frame.showCard("home"));
    }
}
