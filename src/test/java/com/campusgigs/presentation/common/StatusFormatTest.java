package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.dto.GigDto;
import com.campusgigs.model.GigStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StatusFormatTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    private static GigDto gig(GigStatus status, LocalDate deadline) {
        return new GigDto(1, 2, 3, status == GigStatus.OPEN ? null : 4L, "Logo", "Description", 100, deadline,
                status, LocalDateTime.now());
    }

    @Test
    void everyStatusHasAReadableName() {
        for (GigStatus status : GigStatus.values()) {
            assertFalse(StatusFormat.gigStatus(status).isBlank());
        }
        assertEquals("Open", StatusFormat.gigStatus(GigStatus.OPEN));
        assertEquals("In progress", StatusFormat.gigStatus(GigStatus.ASSIGNED));
        assertEquals("Cancelled", StatusFormat.gigStatus(GigStatus.CANCELLED));
    }

    @Test
    void aGigInProgressIsOverdueOnlyAfterItsDeadline() {
        assertTrue(StatusFormat.isOverdue(gig(GigStatus.ASSIGNED, TODAY.minusDays(1)), TODAY));
        assertFalse(StatusFormat.isOverdue(gig(GigStatus.ASSIGNED, TODAY), TODAY));
        assertFalse(StatusFormat.isOverdue(gig(GigStatus.ASSIGNED, TODAY.plusDays(3)), TODAY));
    }

    @Test
    void otherStatusesAreNeverOverdue() {
        for (GigStatus status : GigStatus.values()) {
            if (status != GigStatus.ASSIGNED) {
                assertFalse(StatusFormat.isOverdue(gig(status, TODAY.minusDays(30)), TODAY), status.name());
            }
        }
    }
}
