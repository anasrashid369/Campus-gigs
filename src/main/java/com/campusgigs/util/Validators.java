package com.campusgigs.util;

import java.util.regex.Pattern;

/**
 * Small input checks shared by screens (for quick feedback) and services
 * (for safety). All methods accept {@code null} and never throw.
 */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    private Validators() {
    }

    /** True when the value is {@code null}, empty or only whitespace. */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** True for a normal address such as {@code ali@uni.edu.pk}; surrounding spaces are not allowed. */
    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value).matches();
    }

    /** True when the value has at least {@code minLength} characters. */
    public static boolean hasMinLength(String value, int minLength) {
        return value != null && value.length() >= minLength;
    }

    /** True when the value is a whole number greater than zero, such as a token amount ("250"). */
    public static boolean isPositiveNumber(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            return Long.parseLong(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
