package com.campusgigs.service.impl;

import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.RegisterRequest;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.service.AuthService;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.UserService;
import com.campusgigs.service.WalletService;
import com.campusgigs.util.PasswordHasher;
import com.campusgigs.util.Validators;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Default {@link AuthService}. */
public class AuthServiceImpl implements AuthService {

    static final String INVALID_LOGIN = "Invalid email or password";

    /** Longest allowed email, matching Users.email. */
    private static final int MAX_EMAIL_LENGTH = 150;

    /** Checked when the email is unknown, so both failure paths take about the same time. */
    private static final String DUMMY_HASH = PasswordHasher.hash("no-such-user-password");

    private final UserDao userDao;
    private final WalletService walletService;
    private final SettingsService settingsService;
    private final TransactionManager transactions;

    public AuthServiceImpl(UserDao userDao, WalletService walletService,
                           SettingsService settingsService, TransactionManager transactions) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.walletService = Objects.requireNonNull(walletService, "walletService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public UserDto register(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Registration details are required");
        }
        String fullName = request.fullName() == null ? "" : request.fullName().trim();
        String email = normaliseEmail(request.email());
        validate(fullName, email, request.password(), request.role());

        String passwordHash = PasswordHasher.hash(request.password());
        long initialBalance = settingsService.getInitialTokenBalance();

        return transactions.call(() -> {
            if (userDao.findByEmail(email).isPresent()) {
                throw new ValidationException("This email is already registered");
            }
            long userId = userDao.insert(request.role(), fullName, email, passwordHash);
            walletService.createWallet(userId, initialBalance);
            return new UserDto(userId, request.role(), fullName, email, AccountStatus.ACTIVE);
        });
    }

    @Override
    public UserDto login(String email, String password) {
        if (Validators.isBlank(email) || password == null) {
            throw new ValidationException(INVALID_LOGIN);
        }
        Optional<User> found = userDao.findByEmail(normaliseEmail(email));
        String storedHash = found.map(User::passwordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = PasswordHasher.verify(password, storedHash);
        if (found.isEmpty() || !passwordMatches) {
            throw new ValidationException(INVALID_LOGIN);
        }
        User user = found.get();
        switch (user.accountStatus()) {
            case SUSPENDED -> throw new ValidationException(
                    "This account is suspended. Please contact the platform admin.");
            case DEACTIVATED -> throw new ValidationException("This account has been deactivated.");
            case ACTIVE -> {
                // allowed to log in
            }
        }
        return UserMapper.toDto(user);
    }

    private static void validate(String fullName, String email, String password, Role role) {
        if (fullName.isEmpty()) {
            throw new ValidationException("Full name is required");
        }
        if (fullName.length() > UserService.MAX_NAME_LENGTH) {
            throw new ValidationException("Full name can be at most " + UserService.MAX_NAME_LENGTH + " characters");
        }
        if (!Validators.isValidEmail(email) || email.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Please enter a valid email address");
        }
        if (!isStrongPassword(password)) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH
                    + " characters and contain a letter and a digit");
        }
        if (role != Role.CLIENT && role != Role.FREELANCER) {
            throw new ValidationException("Please choose Client or Freelancer");
        }
    }

    private static boolean isStrongPassword(String password) {
        return Validators.hasMinLength(password, MIN_PASSWORD_LENGTH)
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }

    /** Emails are stored trimmed and in lower case, so "Ali@Uni.edu" and "ali@uni.edu" are one account. */
    private static String normaliseEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
