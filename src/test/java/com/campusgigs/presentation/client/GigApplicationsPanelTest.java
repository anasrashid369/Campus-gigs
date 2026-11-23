package com.campusgigs.presentation.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.ApplicationService;
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
class GigApplicationsPanelTest {

    @Mock ApplicationService applicationService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    @Test
    void showsTheApplicantTheirStatusAndAShortenedProposal() {
        ApplicationDto application = new ApplicationDto(20, 10, "Logo design", GigStatus.OPEN, 3, "Free Lancer",
                "I have designed many logos\nfor clubs and societies and I would love to help with yours",
                ApplicationStatus.PENDING, LocalDateTime.of(2026, 11, 24, 14, 30));

        Object[] cells = GigApplicationsPanel.toCells(application);

        assertEquals("Free Lancer", cells[0]);
        assertEquals("Pending", cells[1]);
        assertEquals("2026-11-24 14:30", cells[2]);
        assertEquals(60, ((String) cells[3]).length());
        assertArrayEquals(new Object[] {"Free Lancer", "Pending", "2026-11-24 14:30", cells[3]}, cells);
    }

    @Test
    void loadsTheApplicationsOfTheGigForTheLoggedInClient() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(1, Role.CLIENT, "Client", "c@example.com", AccountStatus.ACTIVE));
        GigDto gig = new GigDto(10, 1, 7, null, "Logo design", "Description", 1500, LocalDate.of(2026, 12, 31),
                GigStatus.OPEN, LocalDateTime.now());
        lenient().when(applicationService.listForGig(1, 10)).thenReturn(List.of());

        new GigApplicationsPanel(applicationService, gig);

        verify(applicationService, timeout(3000)).listForGig(1, 10);
    }
}
