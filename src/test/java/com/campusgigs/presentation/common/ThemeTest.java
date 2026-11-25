package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThemeTest {

    private LookAndFeel original;

    @BeforeEach
    void rememberLookAndFeel() {
        original = UIManager.getLookAndFeel();
    }

    @AfterEach
    void restoreLookAndFeel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(original);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void installsFlatLafWithRoundedCornersAndTallerRows() throws Exception {
        SwingUtilities.invokeAndWait(Theme::apply);

        assertInstanceOf(FlatLightLaf.class, UIManager.getLookAndFeel());
        assertEquals(10, UIManager.getInt("Button.arc"));
        assertEquals(28, UIManager.getInt("Table.rowHeight"));
    }
}
