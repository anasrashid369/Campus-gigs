package com.campusgigs.presentation.admin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.LedgerEntryDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.TransactionType;
import com.campusgigs.service.MonitoringService;
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
class MonitoringTabsTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 24, 8, 15);

    @Mock MonitoringService monitoringService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    @Test
    void gigRowsShowClientAndFreelancer() {
        GigDto open = new GigDto(10, 1, 7, null, "Logo", "Description", 300, LocalDate.of(2026, 12, 1), GigStatus.OPEN, WHEN);
        GigDto taken = new GigDto(11, 1, 7, 4L, "Poster", "Description", 1200, LocalDate.of(2026, 12, 2), GigStatus.ASSIGNED, WHEN);

        assertArrayEquals(new Object[] {"Logo", "300", LocalDate.of(2026, 12, 1), "Open", "#1", "-"},
                GigsMonitorTab.toCells(open));
        assertArrayEquals(new Object[] {"Poster", "1,200", LocalDate.of(2026, 12, 2), "In progress", "#1", "#4"},
                GigsMonitorTab.toCells(taken));
    }

    @Test
    void applicationRowsShowBothStatuses() {
        ApplicationDto application = new ApplicationDto(20, 10, "Logo", GigStatus.ASSIGNED, 4, "Free", "Proposal",
                ApplicationStatus.ACCEPTED, WHEN);

        assertArrayEquals(new Object[] {"Logo", "Free", "Accepted", "In progress", "2026-11-24 08:15"},
                ApplicationsMonitorTab.toCells(application));
    }

    @Test
    void ledgerRowsShowAReadableTypeAndDashesForMissingValues() {
        LedgerEntryDto withGig = new LedgerEntryDto(1, "Client", TransactionType.ESCROW_HOLD, 600, 10L, "Held", WHEN);
        LedgerEntryDto plain = new LedgerEntryDto(2, "Freelancer", TransactionType.WITHDRAWAL, 50, null, null, WHEN);

        assertArrayEquals(new Object[] {"2026-11-24 08:15", "Client", "Escrow hold", "600", "10", "Held"},
                LedgerMonitorTab.toCells(withGig));
        assertArrayEquals(new Object[] {"2026-11-24 08:15", "Freelancer", "Withdrawal", "50", "-", "-"},
                LedgerMonitorTab.toCells(plain));
    }

    @Test
    void daysAreReadAsIsoDatesAndBlankMeansNoLimit() {
        assertEquals(LocalDate.of(2026, 11, 24), LedgerMonitorTab.parseDay(" 2026-11-24 "));
        assertNull(LedgerMonitorTab.parseDay(""));
        assertNull(LedgerMonitorTab.parseDay("   "));
        assertNull(LedgerMonitorTab.parseDay(null));
    }

    @Test
    void anUnreadableDayGivesAClearMessage() {
        for (String bad : new String[] {"24/11/2026", "2026-13-01", "today"}) {
            ValidationException error = assertThrows(ValidationException.class, () -> LedgerMonitorTab.parseDay(bad));
            assertEquals("Enter days as yyyy-MM-dd, for example 2026-11-24.", error.getMessage());
        }
    }

    @Test
    void theMonitoringPanelLoadsAllThreeListsForTheAdmin() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(9, Role.ADMIN, "Admin", "a@example.com", AccountStatus.ACTIVE));
        lenient().when(monitoringService.listGigs(9, null)).thenReturn(List.of());
        lenient().when(monitoringService.listApplications(9, null)).thenReturn(List.of());
        lenient().when(monitoringService.listLedger(9, null, null, null)).thenReturn(List.of());

        new MonitoringPanel(monitoringService);

        verify(monitoringService, timeout(3000)).listGigs(9, null);
        verify(monitoringService, timeout(3000)).listApplications(9, null);
        verify(monitoringService, timeout(3000)).listLedger(9, null, null, null);
    }
}
