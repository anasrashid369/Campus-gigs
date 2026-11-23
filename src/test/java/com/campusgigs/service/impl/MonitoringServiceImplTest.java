package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.LedgerEntryDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigApplication;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.LedgerEntry;
import com.campusgigs.model.Role;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceImplTest {

    private static final long ADMIN = 9;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);

    @Mock GigDao gigDao;
    @Mock ApplicationDao applicationDao;
    @Mock TransactionDao transactionDao;
    @Mock UserDao userDao;
    @InjectMocks MonitoringServiceImpl service;

    private static User user(long id, Role role, AccountStatus status) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", status, NOW);
    }

    @BeforeEach
    void setUp() {
        lenient().when(userDao.findById(ADMIN)).thenReturn(Optional.of(user(ADMIN, Role.ADMIN, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(1)).thenReturn(Optional.of(user(1, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(2)).thenReturn(Optional.of(user(2, Role.FREELANCER, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(8)).thenReturn(Optional.of(user(8, Role.ADMIN, AccountStatus.SUSPENDED)));
    }

    @Test
    void listsGigsWithTheChosenStatus() {
        when(gigDao.findAll(GigStatus.OPEN)).thenReturn(List.of(new Gig(10, 1, 1, null, "Logo", "Description", 300,
                LocalDate.of(2030, 1, 1), GigStatus.OPEN, NOW)));

        List<GigDto> result = service.listGigs(ADMIN, GigStatus.OPEN);

        assertEquals(1, result.size());
        assertEquals("Logo", result.get(0).title());
    }

    @Test
    void listsApplicationsWithGigTitleAndFreelancerName() {
        when(applicationDao.findAll(null)).thenReturn(List.of(
                new GigApplication(20, 10, 2, "I can do it", ApplicationStatus.PENDING, NOW)));
        when(gigDao.findById(10)).thenReturn(Optional.of(new Gig(10, 1, 1, null, "Logo", "Description", 300,
                LocalDate.of(2030, 1, 1), GigStatus.OPEN, NOW)));

        List<ApplicationDto> result = service.listApplications(ADMIN, null);

        assertEquals("Logo", result.get(0).gigTitle());
        assertEquals("User 2", result.get(0).freelancerName());
    }

    @Test
    void listsTheLedgerWithTheGivenFilters() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        when(transactionDao.findLedger(TransactionType.TOPUP, from, to)).thenReturn(List.of(
                new LedgerEntry(1, 2, "User 2", TransactionType.TOPUP, 50, null, "Top-up", NOW)));

        List<LedgerEntryDto> result = service.listLedger(ADMIN, TransactionType.TOPUP, from, to);

        assertEquals(1, result.size());
        assertEquals("User 2", result.get(0).userName());
        assertEquals(50, result.get(0).amount());
    }

    @Test
    void theFirstDayCannotBeAfterTheLastDay() {
        assertThrows(ValidationException.class, () -> service.listLedger(ADMIN, null,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)));

        verifyNoInteractions(transactionDao);
    }

    @Test
    void sameDayAndOpenEndedRangesAreFine() {
        LocalDate day = LocalDate.of(2026, 11, 24);
        when(transactionDao.findLedger(null, day, day)).thenReturn(List.of());
        when(transactionDao.findLedger(null, day, null)).thenReturn(List.of());

        service.listLedger(ADMIN, null, day, day);
        service.listLedger(ADMIN, null, day, null);

        verify(transactionDao).findLedger(null, day, null);
    }

    @Test
    void onlyAnActiveAdministratorCanMonitor() {
        for (long caller : new long[] {1, 2, 8}) {
            assertThrows(ValidationException.class, () -> service.listGigs(caller, null));
            assertThrows(ValidationException.class, () -> service.listApplications(caller, null));
            assertThrows(ValidationException.class, () -> service.listLedger(caller, null, null, null));
        }
        when(userDao.findById(77)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.listGigs(77, null));
        verifyNoInteractions(gigDao, applicationDao, transactionDao);
    }
}
