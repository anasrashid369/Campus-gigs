package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.EarningsDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.User;
import com.campusgigs.model.Wallet;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EarningsServiceImplTest {

    @Mock UserDao userDao;
    @Mock WalletDao walletDao;
    @Mock TransactionDao transactionDao;
    @Mock GigDao gigDao;
    @InjectMocks EarningsServiceImpl service;

    private static User user(long id, Role role) {
        return new User(id, role, "User", "u@example.com", "hash", AccountStatus.ACTIVE, LocalDateTime.now());
    }

    @Test
    void summarisesEarningsAfterFees() {
        when(userDao.findById(2)).thenReturn(Optional.of(user(2, Role.FREELANCER)));
        when(walletDao.findByUserId(2)).thenReturn(Optional.of(new Wallet(20, 2, 470, 0, 0)));
        when(transactionDao.sumByType(20, TransactionType.ESCROW_RELEASE)).thenReturn(1000L);
        when(transactionDao.sumByType(20, TransactionType.PLATFORM_FEE)).thenReturn(50L);
        when(transactionDao.sumByType(20, TransactionType.WITHDRAWAL)).thenReturn(480L);
        when(gigDao.sumBudgetByFreelancerAndStatuses(2,
                EnumSet.of(GigStatus.ASSIGNED, GigStatus.SUBMITTED, GigStatus.DISPUTED))).thenReturn(300L);

        EarningsDto result = service.getEarnings(2);

        assertEquals(new EarningsDto(470, 950, 50, 480, 300), result);
    }

    @Test
    void aNewFreelancerHasZeroEverything() {
        when(userDao.findById(2)).thenReturn(Optional.of(user(2, Role.FREELANCER)));
        when(walletDao.findByUserId(2)).thenReturn(Optional.of(new Wallet(20, 2, 0, 0, 0)));
        when(transactionDao.sumByType(20, TransactionType.ESCROW_RELEASE)).thenReturn(0L);
        when(transactionDao.sumByType(20, TransactionType.PLATFORM_FEE)).thenReturn(0L);
        when(transactionDao.sumByType(20, TransactionType.WITHDRAWAL)).thenReturn(0L);
        when(gigDao.sumBudgetByFreelancerAndStatuses(any(Long.class), any())).thenReturn(0L);

        assertEquals(new EarningsDto(0, 0, 0, 0, 0), service.getEarnings(2));
    }

    @Test
    void onlyFreelancersHaveEarnings() {
        when(userDao.findById(1)).thenReturn(Optional.of(user(1, Role.CLIENT)));

        assertThrows(ValidationException.class, () -> service.getEarnings(1));

        verifyNoInteractions(walletDao, transactionDao, gigDao);
    }

    @Test
    void unknownUserOrMissingWalletIsReported() {
        when(userDao.findById(9)).thenReturn(Optional.empty());
        when(userDao.findById(2)).thenReturn(Optional.of(user(2, Role.FREELANCER)));
        when(walletDao.findByUserId(2)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEarnings(9));
        assertThrows(ResourceNotFoundException.class, () -> service.getEarnings(2));
    }
}
