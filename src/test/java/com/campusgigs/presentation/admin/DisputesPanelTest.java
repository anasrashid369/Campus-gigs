package com.campusgigs.presentation.admin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.DisputeService;
import com.campusgigs.util.SessionManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisputesPanelTest {

    @Mock DisputeService disputeService;

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    private static DisputeDto open(long raisedBy, String reason) {
        return new DisputeDto(5, 10, "Logo design", 1, 2, 1500, raisedBy, reason, DisputeStatus.OPEN, null, null, null);
    }

    @Test
    void showsAnOpenDisputeRaisedByTheClient() {
        assertArrayEquals(new Object[] {"#5", "Logo design", "1,500", "Client", DisputeStatus.OPEN, "-", "Late"},
                DisputesPanel.toCells(open(1, "Late")));
    }

    @Test
    void showsWhoRaisedItAndTheDecisionOnceResolved() {
        DisputeDto resolved = new DisputeDto(5, 10, "Logo design", 1, 2, 1500, 2, "Unpaid", DisputeStatus.RESOLVED,
                DisputeDecision.RELEASE, 99L, LocalDateTime.now());

        Object[] cells = DisputesPanel.toCells(resolved);

        assertEquals("Freelancer", cells[3]);
        assertEquals(DisputeDecision.RELEASE, cells[5]);
    }

    @Test
    void longReasonsAreCutToOneShortLine() {
        String shortened = DisputesPanel.shorten("word ".repeat(40));

        assertEquals(60, shortened.length());
        assertTrue(shortened.endsWith("…"));
        assertEquals("Two lines now one", DisputesPanel.shorten("Two\n lines   now\none"));
    }

    @Test
    void loadsTheOpenDisputesOfTheLoggedInAdmin() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(99, Role.ADMIN, "Admin", "admin@example.com", AccountStatus.ACTIVE));
        when(disputeService.listDisputes(99, DisputeStatus.OPEN)).thenReturn(List.of(open(1, "Late")));

        new DisputesPanel(disputeService);

        verify(disputeService, timeout(3000)).listDisputes(99, DisputeStatus.OPEN);
    }
}
