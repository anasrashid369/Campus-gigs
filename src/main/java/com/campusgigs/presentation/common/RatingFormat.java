package com.campusgigs.presentation.common;

import com.campusgigs.dto.RatingSummaryDto;
import java.util.Locale;

/** Turns ratings into the text shown on screen. */
public final class RatingFormat {

    private static final String FULL_STAR = "★";
    private static final String EMPTY_STAR = "☆";
    private static final int STARS = 5;

    private RatingFormat() {
    }

    /** A 1 to 5 rating as stars, for example {@code ★★★★☆} for 4. */
    public static String stars(int rating) {
        int full = Math.max(0, Math.min(STARS, rating));
        return FULL_STAR.repeat(full) + EMPTY_STAR.repeat(STARS - full);
    }

    /** An average with its count, for example {@code 4.5 ★ (2 reviews)}, or "No reviews yet". */
    public static String summary(RatingSummaryDto summary) {
        if (!summary.hasReviews()) {
            return "No reviews yet";
        }
        return String.format(Locale.ROOT, "%.1f %s (%d %s)", summary.averageRating(), FULL_STAR,
                summary.reviewCount(), summary.reviewCount() == 1 ? "review" : "reviews");
    }
}
