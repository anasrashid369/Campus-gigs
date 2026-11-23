package com.campusgigs.presentation.common;

import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JPanel;

/**
 * One button in a dashboard's navigation area.
 *
 * @param label  the button text, for example "My Wallet"
 * @param screen creates the screen the first time the button is clicked after login
 */
public record NavigationItem(String label, Supplier<JPanel> screen) {

    public NavigationItem {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(screen, "screen");
    }
}
