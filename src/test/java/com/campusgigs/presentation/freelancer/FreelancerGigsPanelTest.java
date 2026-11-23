package com.campusgigs.presentation.freelancer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreelancerGigsPanelTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    @Mock GigService gigService;
    @Mock GigLifecycleService lifecycleService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    private static GigDto gig(GigStatus status, LocalDate deadline) {
        return new GigDto(10, 1, 7, 3L, "Logo design", "Description", 1500, deadline, status, LocalDateTime.now());
    }

    @Test
    void marksGigsInProgressThatAreLate() {
        assertArrayEquals(new Object[] {"Logo design", "1,500", TODAY.minusDays(2), "In progress", "Overdue"},
                FreelancerGigsPanel.toCells(gig(GigStatus.ASSIGNED, TODAY.minusDays(2)), TODAY));
    }

    @Test
    void doesNotMarkGigsThatAreOnTimeOrNotInProgress() {
        assertArrayEquals(new Object[] {"Logo design", "1,500", TODAY, "In progress", ""},
                FreelancerGigsPanel.toCells(gig(GigStatus.ASSIGNED, TODAY), TODAY));
        assertArrayEquals(new Object[] {"Logo design", "1,500", TODAY.minusDays(9), "Submitted", ""},
                FreelancerGigsPanel.toCells(gig(GigStatus.SUBMITTED, TODAY.minusDays(9)), TODAY));
    }

    @Test
    void loadsTheGigsOfTheLoggedInFreelancer() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(3, Role.FREELANCER, "Free", "f@example.com", AccountStatus.ACTIVE));
        lenient().when(gigService.listFreelancerGigs(3)).thenReturn(List.of());

        new FreelancerGigsPanel(gigService, lifecycleService);

        verify(gigService, timeout(3000)).listFreelancerGigs(3);
    }
}
