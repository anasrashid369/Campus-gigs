package com.campusgigs.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.EarningsDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.admin.AdminSummaryPanel;
import com.campusgigs.presentation.client.ClientSummaryPanel;
import com.campusgigs.presentation.freelancer.FreelancerSummaryPanel;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.DisputeService;
import com.campusgigs.service.EarningsService;
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
class SummaryPanelsTest {

    @Mock GigService gigService;
    @Mock ApplicationService applicationService;
    @Mock EarningsService earningsService;
    @Mock DisputeService disputeService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    private static GigDto gig(long id, GigStatus status) {
        return new GigDto(id, 1, 1, id, "Gig " + id, "Description", 100, LocalDate.of(2030, 1, 1), status,
                LocalDateTime.now());
    }

    private static void logIn(Role role) {
        SessionManager.getInstance().setCurrentUser(new UserDto(5, role, "User", "u@example.com", AccountStatus.ACTIVE));
    }

    @Test
    void clientSummaryCountsGigsByStatus() {
        List<GigDto> gigs = List.of(gig(1, GigStatus.OPEN), gig(2, GigStatus.OPEN), gig(3, GigStatus.ASSIGNED),
                gig(4, GigStatus.SUBMITTED), gig(5, GigStatus.CANCELLED));

        assertEquals(2, ClientSummaryPanel.count(gigs, GigStatus.OPEN));
        assertEquals(1, ClientSummaryPanel.count(gigs, GigStatus.ASSIGNED));
        assertEquals(1, ClientSummaryPanel.count(gigs, GigStatus.SUBMITTED));
        assertEquals(0, ClientSummaryPanel.count(gigs, GigStatus.APPROVED));
        assertEquals(0, ClientSummaryPanel.count(List.of(), GigStatus.OPEN));
    }

    @Test
    void freelancerSummaryCountsPendingApplicationsAndWorkInProgress() {
        ApplicationDto pending = new ApplicationDto(1, 1, "Gig", GigStatus.OPEN, 5, "User", "Proposal",
                ApplicationStatus.PENDING, LocalDateTime.now());
        ApplicationDto accepted = new ApplicationDto(2, 2, "Gig", GigStatus.ASSIGNED, 5, "User", "Proposal",
                ApplicationStatus.ACCEPTED, LocalDateTime.now());

        assertEquals(1, FreelancerSummaryPanel.pending(List.of(pending, accepted)));
        assertEquals(2, FreelancerSummaryPanel.inProgress(List.of(gig(1, GigStatus.ASSIGNED),
                gig(2, GigStatus.SUBMITTED), gig(3, GigStatus.APPROVED), gig(4, GigStatus.DISPUTED))));
    }

    @Test
    void clientSummaryLoadsTheGigsOfTheLoggedInClient() {
        logIn(Role.CLIENT);
        lenient().when(gigService.listClientGigs(5)).thenReturn(List.of());

        new ClientSummaryPanel(gigService);

        verify(gigService, timeout(3000)).listClientGigs(5);
    }

    @Test
    void freelancerSummaryLoadsApplicationsGigsAndEarnings() {
        logIn(Role.FREELANCER);
        ApplicationDto pending = new ApplicationDto(1, 1, "Gig", GigStatus.OPEN, 5, "User", "Proposal",
                ApplicationStatus.PENDING, LocalDateTime.now());
        lenient().when(applicationService.listForFreelancer(5)).thenReturn(List.of(pending));
        lenient().when(gigService.listFreelancerGigs(5)).thenReturn(List.of(gig(1, GigStatus.ASSIGNED)));
        lenient().when(earningsService.getEarnings(5)).thenReturn(new EarningsDto(120, 0, 0, 0, 0));

        new FreelancerSummaryPanel(applicationService, gigService, earningsService);

        verify(applicationService, timeout(3000)).listForFreelancer(5);
        verify(gigService, timeout(3000)).listFreelancerGigs(5);
        verify(earningsService, timeout(3000)).getEarnings(5);
    }

    @Test
    void adminSummaryAsksForTheOpenDisputes() {
        logIn(Role.ADMIN);
        lenient().when(disputeService.listDisputes(5, DisputeStatus.OPEN)).thenReturn(List.of());

        new AdminSummaryPanel(disputeService);

        verify(disputeService, timeout(3000)).listDisputes(5, DisputeStatus.OPEN);
    }
}
