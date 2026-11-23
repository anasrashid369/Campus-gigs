package com.campusgigs.presentation.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.GigService;
import com.campusgigs.service.ProfileService;
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
class ClientGigsPanelTest {

    @Mock GigService gigService;
    @Mock ProfileService profileService;
    @Mock ApplicationService applicationService;
    @Mock GigLifecycleService lifecycleService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    private static GigDto gig(GigStatus status, Long freelancerId) {
        return new GigDto(10, 1, 7, freelancerId, "Logo design", "Description", 1500, LocalDate.of(2026, 12, 31),
                status, LocalDateTime.now());
    }

    @Test
    void showsAnOpenGigWithoutAFreelancer() {
        assertArrayEquals(new Object[] {"Logo design", "1,500", LocalDate.of(2026, 12, 31), "Open", "-"},
                ClientGigsPanel.toCells(gig(GigStatus.OPEN, null)));
    }

    @Test
    void showsTheStatusWordsAndTheHiredFreelancer() {
        assertArrayEquals(new Object[] {"Logo design", "1,500", LocalDate.of(2026, 12, 31), "In progress", "#4"},
                ClientGigsPanel.toCells(gig(GigStatus.ASSIGNED, 4L)));
    }

    @Test
    void loadsTheGigsOfTheLoggedInClient() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(1, Role.CLIENT, "Client", "c@example.com", AccountStatus.ACTIVE));
        lenient().when(gigService.listClientGigs(1)).thenReturn(List.of(gig(GigStatus.OPEN, null)));

        new ClientGigsPanel(gigService, profileService, applicationService, lifecycleService);

        verify(gigService, timeout(3000)).listClientGigs(1);
    }
}
