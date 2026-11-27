package com.campusgigs.presentation.common;

import java.util.Objects;
import java.util.function.Supplier;
import javafx.scene.Node;

/**
 * One button in a dashboard's navigation area.
 *
 * @param label  the button text, for example "My Wallet"
 * @param screen creates the screen the first time the button is clicked after login
 */
public record NavigationItem(String label, Supplier<? extends Node> screen) {

    public NavigationItem {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(screen, "screen");
    }
}
