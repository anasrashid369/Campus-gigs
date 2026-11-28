package com.campusgigs.presentation.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.RatingSummaryDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.FxTestSupport;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.ReviewService;
import com.campusgigs.util.SessionManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GigApplicationsPanelTest {

    @Mock ApplicationService applicationService;
    @Mock ReviewService reviewService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    private static ApplicationDto application(long freelancerId) {
        return new ApplicationDto(20, 10, "Logo design", GigStatus.OPEN, freelancerId, "Free Lancer",
                "I have designed many logos\nfor clubs and societies and I would love to help with yours",
                ApplicationStatus.PENDING, LocalDateTime.of(2026, 11, 24, 14, 30));
    }

    @Test
    void showsTheApplicantTheirRatingStatusAndAShortenedProposal() {
        Object[] cells = GigApplicationsPanel.toCells(
                new GigApplicationsPanel.Row(application(3), new RatingSummaryDto(3, 4.5, 2)));

        assertEquals(60, ((String) cells[4]).length());
        assertArrayEquals(new Object[] {"Free Lancer", "4.5 ★ (2 reviews)", "Pending", "2026-11-24 14:30",
                cells[4]}, cells);
    }

    @Test
    void aNewFreelancerShowsNoReviewsYet() {
        Object[] cells = GigApplicationsPanel.toCells(
                new GigApplicationsPanel.Row(application(3), RatingSummaryDto.none(3)));

        assertEquals("No reviews yet", cells[1]);
    }

    @Test
    void loadsTheApplicationsAndTheRatingsOfTheirFreelancers() {
        FxTestSupport.startToolkit();
        SessionManager.getInstance().setCurrentUser(
                new UserDto(1, Role.CLIENT, "Client", "c@example.com", AccountStatus.ACTIVE));
        GigDto gig = new GigDto(10, 1, 7, null, "Logo design", "Description", 1500, LocalDate.of(2026, 12, 31),
                GigStatus.OPEN, LocalDateTime.now());
        lenient().when(applicationService.listForGig(1, 10)).thenReturn(List.of(application(3)));
        lenient().when(reviewService.getRatingSummaries(List.of(3L)))
                .thenReturn(Map.of(3L, RatingSummaryDto.none(3)));

        new GigApplicationsPanel(applicationService, reviewService, gig);

        verify(applicationService, timeout(3000)).listForGig(1, 10);
        verify(reviewService, timeout(3000)).getRatingSummaries(List.of(3L));
    }
}
