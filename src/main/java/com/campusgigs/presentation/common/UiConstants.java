package com.campusgigs.presentation.common;

import java.net.URL;

/**
 * Shared sizes and the stylesheet of every screen. Colours and fonts live in
 * campus-gigs.css; use its style classes instead of literal values so all screens look the same.
 */
public final class UiConstants {

    public static final String APP_TITLE = "Campus Gigs";

    /** Classpath location of the stylesheet. */
    public static final String STYLESHEET = "/com/campusgigs/presentation/campus-gigs.css";

    public static final double SPACING_SMALL = 4;
    public static final double SPACING_MEDIUM = 8;
    public static final double SPACING_LARGE = 16;
    public static final double SPACING_XLARGE = 24;

    public static final double FRAME_WIDTH = 1180;
    public static final double FRAME_HEIGHT = 760;
    public static final double FRAME_MIN_WIDTH = 900;
    public static final double FRAME_MIN_HEIGHT = 600;
    public static final double FORM_WIDTH = 440;
    public static final double WIDE_FORM_WIDTH = 640;

    private UiConstants() {
    }

    /** The stylesheet as a URL string for {@code getStylesheets().add(...)}. */
    public static String stylesheet() {
        URL url = UiConstants.class.getResource(STYLESHEET);
        if (url == null) {
            throw new IllegalStateException("Stylesheet missing from the classpath: " + STYLESHEET);
        }
        return url.toExternalForm();
    }
}
