package com.campusgigs.presentation.common;

/** Helpers for showing longer texts in table cells. */
public final class TextFormat {

    private TextFormat() {
    }

    /** Puts a text on one line and cuts it to {@code maxChars} characters, ending with an ellipsis if it was cut. */
    public static String oneLine(String text, int maxChars) {
        String line = text.replaceAll("\\s+", " ").trim();
        return line.length() <= maxChars ? line : line.substring(0, maxChars - 1) + "…";
    }
}
