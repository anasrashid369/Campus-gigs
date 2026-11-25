package com.campusgigs.presentation.common;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Map;

/**
 * The application's look and feel: FlatLaf (a modern flat theme for Swing) in the
 * Campus Gigs colours. Call {@link #apply()} once on the Event Dispatch Thread before
 * the first window is created; screens need no changes.
 */
public final class Theme {

    private static final int CORNER_ARC = 10;
    private static final int TABLE_ROW_HEIGHT = 28;
    private static final int SCROLL_BAR_WIDTH = 10;

    private Theme() {
    }

    /** Installs the theme. If FlatLaf cannot be installed Swing keeps its default look. */
    public static void apply() {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", toHex(UiConstants.PRIMARY)));
        if (!FlatLightLaf.setup()) {
            return;
        }
        Font defaultFont = UIManager.getFont("defaultFont");
        UIManager.put("defaultFont", defaultFont.deriveFont((float) UiConstants.BODY_FONT.getSize()));
        UIManager.put("Button.arc", CORNER_ARC);
        UIManager.put("Component.arc", CORNER_ARC);
        UIManager.put("TextComponent.arc", CORNER_ARC);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Table.rowHeight", TABLE_ROW_HEIGHT);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TableHeader.font", UiConstants.SMALL_FONT.deriveFont(Font.BOLD));
        UIManager.put("ScrollBar.width", SCROLL_BAR_WIDTH);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.showTabSeparators", true);
    }

    /**
     * Makes a button look like a menu entry on the dark navigation column: no border, white
     * left-aligned text and a lighter background on hover. Without FlatLaf it is a plain button.
     */
    public static void styleNavigationButton(JButton button) {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.putClientProperty(FlatClientProperties.STYLE, "foreground: #FFFFFF;"
                + " hoverBackground: " + toHex(UiConstants.PRIMARY) + ";"
                + " pressedBackground: " + toHex(UiConstants.PRIMARY.darker()) + ";"
                + " margin: 6,12,6,12; font: +1");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusable(false);
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
