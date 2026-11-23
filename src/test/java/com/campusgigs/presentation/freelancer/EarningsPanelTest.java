package com.campusgigs.presentation.freelancer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.EarningsDto;
import com.campusgigs.dto.TransactionDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.service.EarningsService;
import com.campusgigs.service.TransactionService;
import com.campusgigs.service.WalletService;
import com.campusgigs.util.SessionManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EarningsPanelTest {

    private static final long USER_ID = 8;

    @Mock EarningsService earningsService;
    @Mock WalletService walletService;
    @Mock TransactionService transactionService;

    @BeforeEach
    void logIn() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(USER_ID, Role.FREELANCER, "Test Freelancer", "f@example.com", AccountStatus.ACTIVE));
        lenient().when(earningsService.getEarnings(USER_ID)).thenReturn(new EarningsDto(500, 950, 50, 100, 300));
        lenient().when(transactionService.getHistory(USER_ID)).thenReturn(List.<TransactionDto>of());
    }

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    /** Builds the panel and waits until its first background load has finished, so stubbing is safe. */
    private EarningsPanel openPanel() {
        EarningsPanel panel = new EarningsPanel(earningsService, walletService, transactionService);
        verify(earningsService, timeout(3000)).getEarnings(USER_ID);
        verify(transactionService, timeout(3000)).getHistory(USER_ID);
        return panel;
    }

    @Test
    void loadsTheEarningsAndHistoryOfTheLoggedInFreelancer() {
        openPanel();
    }

    @Test
    void withdrawalCallsTheServiceWithTheTypedAmount() {
        EarningsPanel panel = openPanel();
        when(walletService.withdraw(USER_ID, 120)).thenReturn(new WalletDto(1, USER_ID, 380, 0));

        panel.submitWithdrawal(" 120 ");

        verify(walletService, timeout(3000)).withdraw(USER_ID, 120);
    }

    @Test
    void anAmountThatIsNotANumberNeverReachesTheService() {
        EarningsPanel panel = openPanel();

        panel.submitWithdrawal("abc");
        panel.submitWithdrawal("");
        panel.submitWithdrawal("1.5");

        verify(walletService, never()).withdraw(anyLong(), anyLong());
    }

    @Test
    void aRejectedWithdrawalIsHandledWithoutCrashing() {
        EarningsPanel panel = openPanel();
        when(walletService.withdraw(USER_ID, 9_999)).thenThrow(new ValidationException("Not enough tokens"));

        panel.submitWithdrawal("9999");

        verify(walletService, timeout(3000)).withdraw(USER_ID, 9_999);
    }
}
