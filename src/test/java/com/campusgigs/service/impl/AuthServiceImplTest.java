package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.RegisterRequest;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.WalletService;
import com.campusgigs.util.PasswordHasher;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PASSWORD = "Secret123";
    private static final String STORED_HASH = PasswordHasher.hash(PASSWORD);

    @Mock UserDao userDao;
    @Mock WalletService walletService;
    @Mock SettingsService settingsService;

    private int transactionsStarted;
    private AuthServiceImpl service;

    /** Runs the work directly and counts how often a transaction was opened. */
    private final TransactionManager countingTransactions = new TransactionManager() {
        @Override
        public <T> T call(Supplier<T> work) {
            transactionsStarted++;
            return work.get();
        }

        @Override
        public void run(Runnable work) {
            transactionsStarted++;
            work.run();
        }
    };

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userDao, walletService, settingsService, countingTransactions);
    }

    private static User user(AccountStatus status) {
        return new User(7, Role.FREELANCER, "Sara Khan", "sara@uni.edu.pk", STORED_HASH, status,
                LocalDateTime.of(2026, 11, 1, 9, 0));
    }

    private static RegisterRequest request(String name, String email, String password, Role role) {
        return new RegisterRequest(name, email, password, role);
    }

    // ---- register ----

    @Test
    void registerCreatesUserAndWalletWithSettingsBalanceInOneTransaction() {
        when(settingsService.getInitialTokenBalance()).thenReturn(1500L);
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.empty());
        when(userDao.insert(eq(Role.FREELANCER), eq("Sara Khan"), eq("sara@uni.edu.pk"), anyString()))
                .thenReturn(7L);

        UserDto created = service.register(request("  Sara Khan ", " Sara@Uni.edu.pk ", PASSWORD, Role.FREELANCER));

        assertEquals(new UserDto(7, Role.FREELANCER, "Sara Khan", "sara@uni.edu.pk", AccountStatus.ACTIVE), created);
        verify(walletService).createWallet(7L, 1500L);
        assertEquals(1, transactionsStarted);
    }

    @Test
    void registerStoresAHashNotThePassword() {
        when(userDao.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userDao.insert(any(), anyString(), anyString(), anyString())).thenReturn(1L);

        service.register(request("Ali", "ali@uni.edu.pk", PASSWORD, Role.CLIENT));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userDao).insert(eq(Role.CLIENT), eq("Ali"), eq("ali@uni.edu.pk"), hash.capture());
        assertTrue(hash.getValue().startsWith("pbkdf2$"));
        assertTrue(PasswordHasher.verify(PASSWORD, hash.getValue()));
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCase() {
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.of(user(AccountStatus.ACTIVE)));

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register(request("Sara", "SARA@uni.edu.pk", PASSWORD, Role.CLIENT)));

        assertEquals("This email is already registered", e.getMessage());
        verify(userDao, never()).insert(any(), anyString(), anyString(), anyString());
        verify(walletService, never()).createWallet(anyLong(), anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "onlyletters", "12345678", ""})
    void registerRejectsWeakPassword(String password) {
        assertThrows(ValidationException.class,
                () -> service.register(request("Ali", "ali@uni.edu.pk", password, Role.CLIENT)));
        verifyNoInteractions(userDao, walletService);
    }

    @Test
    void adminCannotSelfRegister() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register(request("Boss", "boss@uni.edu.pk", PASSWORD, Role.ADMIN)));

        assertEquals("Please choose Client or Freelancer", e.getMessage());
        verifyNoInteractions(userDao, walletService);
    }

    @Test
    void registerRejectsMissingFields() {
        assertThrows(ValidationException.class, () -> service.register(null));
        assertThrows(ValidationException.class,
                () -> service.register(request("  ", "ali@uni.edu.pk", PASSWORD, Role.CLIENT)));
        assertThrows(ValidationException.class,
                () -> service.register(request("Ali", "not-an-email", PASSWORD, Role.CLIENT)));
        assertThrows(ValidationException.class,
                () -> service.register(request("Ali", "ali@uni.edu.pk", PASSWORD, null)));
        assertThrows(ValidationException.class,
                () -> service.register(request("A".repeat(101), "ali@uni.edu.pk", PASSWORD, Role.CLIENT)));
        verifyNoInteractions(userDao, walletService);
    }

    // ---- login ----

    @Test
    void loginReturnsUserWithoutHash() {
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.of(user(AccountStatus.ACTIVE)));

        UserDto dto = service.login(" Sara@uni.edu.pk ", PASSWORD);

        assertEquals(new UserDto(7, Role.FREELANCER, "Sara Khan", "sara@uni.edu.pk", AccountStatus.ACTIVE), dto);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.of(user(AccountStatus.ACTIVE)));
        when(userDao.findByEmail("nobody@uni.edu.pk")).thenReturn(Optional.empty());

        ValidationException wrong = assertThrows(ValidationException.class,
                () -> service.login("sara@uni.edu.pk", "Wrong1234"));
        ValidationException unknown = assertThrows(ValidationException.class,
                () -> service.login("nobody@uni.edu.pk", PASSWORD));

        assertEquals(AuthServiceImpl.INVALID_LOGIN, wrong.getMessage());
        assertEquals(AuthServiceImpl.INVALID_LOGIN, unknown.getMessage());
    }

    @Test
    void suspendedAccountCannotLogIn() {
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.of(user(AccountStatus.SUSPENDED)));

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.login("sara@uni.edu.pk", PASSWORD));

        assertTrue(e.getMessage().contains("suspended"));
    }

    @Test
    void deactivatedAccountCannotLogIn() {
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.of(user(AccountStatus.DEACTIVATED)));

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.login("sara@uni.edu.pk", PASSWORD));

        assertTrue(e.getMessage().contains("deactivated"));
    }

    @Test
    void suspendedStatusIsNotRevealedForAWrongPassword() {
        when(userDao.findByEmail("sara@uni.edu.pk")).thenReturn(Optional.of(user(AccountStatus.SUSPENDED)));

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.login("sara@uni.edu.pk", "Wrong1234"));

        assertEquals(AuthServiceImpl.INVALID_LOGIN, e.getMessage());
    }

    @Test
    void blankLoginInputIsRejectedWithoutLookup() {
        assertThrows(ValidationException.class, () -> service.login(" ", PASSWORD));
        assertThrows(ValidationException.class, () -> service.login(null, PASSWORD));
        assertThrows(ValidationException.class, () -> service.login("sara@uni.edu.pk", null));
        verifyNoInteractions(userDao);
    }
}
