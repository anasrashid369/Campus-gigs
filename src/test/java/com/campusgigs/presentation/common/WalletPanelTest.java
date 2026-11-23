package com.campusgigs.presentation.common;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.TransactionDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
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
class WalletPanelTest {

    private static final long USER_ID = 5;

    @Mock WalletService walletService;
    @Mock TransactionService transactionService;

    @BeforeEach
    void logIn() {
        SessionManager.getInstance().setCurrentUser(
                new UserDto(USER_ID, Role.CLIENT, "Test Client", "client@example.com", AccountStatus.ACTIVE));
        lenient().when(walletService.getWallet(USER_ID)).thenReturn(new WalletDto(1, USER_ID, 100, 0));
        lenient().when(transactionService.getHistory(USER_ID)).thenReturn(List.<TransactionDto>of());
    }

    @AfterEach
    void logOut() {
        SessionManager.getInstance().clear();
    }

    /** Builds the panel and waits until its first background load has finished, so stubbing is safe. */
    private WalletPanel openPanel() {
        WalletPanel panel = new WalletPanel(walletService, transactionService);
        verify(walletService, timeout(3000)).getWallet(USER_ID);
        verify(transactionService, timeout(3000)).getHistory(USER_ID);
        return panel;
    }

    @Test
    void loadsTheWalletAndHistoryOfTheLoggedInUser() {
        openPanel();
    }

    @Test
    void topUpCallsTheServiceWithTheTypedAmount() {
        WalletPanel panel = openPanel();
        when(walletService.topUp(USER_ID, 250)).thenReturn(new WalletDto(1, USER_ID, 350, 0));

        panel.submitTopUp(" 250 ");

        verify(walletService, timeout(3000)).topUp(USER_ID, 250);
    }

    @Test
    void anAmountThatIsNotANumberNeverReachesTheService() {
        WalletPanel panel = openPanel();

        panel.submitTopUp("abc");
        panel.submitTopUp("");
        panel.submitTopUp("12.5");

        verify(walletService, never()).topUp(anyLong(), anyLong());
    }

    @Test
    void aRejectedTopUpIsHandledWithoutCrashing() {
        WalletPanel panel = openPanel();
        when(walletService.topUp(USER_ID, 99_999)).thenThrow(new ValidationException("Top-up must be between 1 and 10000 tokens"));

        panel.submitTopUp("99999");

        verify(walletService, timeout(3000)).topUp(USER_ID, 99_999);
    }
}
