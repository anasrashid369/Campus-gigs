package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campusgigs.dto.RatingSummaryDto;
import org.junit.jupiter.api.Test;

class RatingFormatTest {

    @Test
    void showsARatingAsFiveStars() {
        assertEquals("★★★★☆", RatingFormat.stars(4));
        assertEquals("★☆☆☆☆", RatingFormat.stars(1));
        assertEquals("★★★★★", RatingFormat.stars(5));
    }

    @Test
    void showsTheAverageWithOneDecimalAndTheCount() {
        assertEquals("4.5 ★ (2 reviews)", RatingFormat.summary(new RatingSummaryDto(1, 4.5, 2)));
        assertEquals("3.7 ★ (3 reviews)", RatingFormat.summary(new RatingSummaryDto(1, 11 / 3.0, 3)));
        assertEquals("5.0 ★ (1 review)", RatingFormat.summary(new RatingSummaryDto(1, 5, 1)));
    }

    @Test
    void aFreelancerWithoutReviewsSaysSo() {
        assertEquals("No reviews yet", RatingFormat.summary(RatingSummaryDto.none(1)));
    }
}
