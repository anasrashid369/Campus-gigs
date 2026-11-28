package com.campusgigs.presentation.freelancer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.dto.ReviewDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.FxTestSupport;
import com.campusgigs.service.ReviewService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewsPanelTest {

    @Mock ReviewService reviewService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    @Test
    void showsTheGigClientStarsAndComment() {
        ReviewDto review = new ReviewDto(1, 10, "Logo design", "Ayesha Khan", 3, 4, "Quick and\nfriendly");

        assertArrayEquals(new Object[] {"Logo design", "Ayesha Khan", "★★★★☆",
                "Quick and friendly"}, ReviewsPanel.toCells(review));
    }

    @Test
    void aReviewWithoutCommentShowsADash() {
        ReviewDto review = new ReviewDto(1, 10, "Logo design", "Ayesha Khan", 3, 5, null);

        assertEquals("-", ReviewsPanel.toCells(review)[3]);
    }

    @Test
    void theHeadingShowsTheAverage() {
        assertEquals("My reviews: 4.5 ★ (2 reviews)", ReviewsPanel.title(new RatingSummaryDto(3, 4.5, 2)));
        assertEquals("My reviews: No reviews yet", ReviewsPanel.title(RatingSummaryDto.none(3)));
    }

    @Test
    void loadsTheReviewsAndAverageOfTheLoggedInFreelancer() {
        FxTestSupport.startToolkit();
        SessionManager.getInstance().setCurrentUser(
                new UserDto(3, Role.FREELANCER, "Free", "f@example.com", AccountStatus.ACTIVE));
        lenient().when(reviewService.getReviewsForFreelancer(3)).thenReturn(List.of());
        lenient().when(reviewService.getRatingSummary(3)).thenReturn(RatingSummaryDto.none(3));

        new ReviewsPanel(reviewService);

        verify(reviewService, timeout(3000)).getReviewsForFreelancer(3);
        verify(reviewService, timeout(3000)).getRatingSummary(3);
    }
}
