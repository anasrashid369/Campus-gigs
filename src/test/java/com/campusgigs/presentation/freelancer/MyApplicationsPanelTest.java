package com.campusgigs.presentation.freelancer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.util.SessionManager;
import com.campusgigs.presentation.FxTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyApplicationsPanelTest {

    @Mock ApplicationService applicationService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    @Test
    void showsTheApplicationTheGigStateAndTheSendTime() {
        ApplicationDto application = new ApplicationDto(20, 10, "Logo design", GigStatus.ASSIGNED, 3, "Free",
                "Proposal text", ApplicationStatus.REJECTED, LocalDateTime.of(2026, 11, 24, 9, 5));

        assertArrayEquals(new Object[] {"Logo design", "Not chosen", "In progress", "2026-11-24 09:05"},
                MyApplicationsPanel.toCells(application));
    }

    @Test
    void loadsTheApplicationsOfTheLoggedInFreelancer() {
        FxTestSupport.startToolkit();
        SessionManager.getInstance().setCurrentUser(
                new UserDto(3, Role.FREELANCER, "Free", "f@example.com", AccountStatus.ACTIVE));
        lenient().when(applicationService.listForFreelancer(3)).thenReturn(List.of());

        new MyApplicationsPanel(applicationService);

        verify(applicationService, timeout(3000)).listForFreelancer(3);
    }
}
