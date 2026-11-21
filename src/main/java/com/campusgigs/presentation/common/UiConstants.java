package com.campusgigs.presentation.common;

import java.awt.Color;
import java.awt.Font;

/**
 * Shared look of every screen: fonts, colours, spacing and standard sizes.
 * Use these instead of literal values so all panels look the same.
 */
public final class UiConstants {

    public static final String APP_TITLE = "Campus Gigs";

    public static final String FONT_FAMILY = Font.SANS_SERIF;
    public static final Font TITLE_FONT = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font HEADING_FONT = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font BODY_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font(FONT_FAMILY, Font.PLAIN, 12);

    public static final Color PRIMARY = new Color(0x1E, 0x5A, 0xA8);
    public static final Color PRIMARY_DARK = new Color(0x15, 0x40, 0x7A);
    public static final Color BACKGROUND = new Color(0xF5, 0xF6, 0xF8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(0x22, 0x22, 0x22);
    public static final Color MUTED_TEXT = new Color(0x6B, 0x72, 0x80);
    public static final Color ERROR = new Color(0xC0, 0x39, 0x2B);
    public static final Color SUCCESS = new Color(0x1E, 0x84, 0x49);

    public static final int SPACING_SMALL = 4;
    public static final int SPACING_MEDIUM = 8;
    public static final int SPACING_LARGE = 16;
    public static final int SPACING_XLARGE = 24;

    public static final int FRAME_WIDTH = 1024;
    public static final int FRAME_HEIGHT = 700;
    public static final int LOGIN_WIDTH = 440;
    public static final int LOGIN_HEIGHT = 360;
    public static final int TEXT_FIELD_COLUMNS = 24;

    private UiConstants() {
    }
}
