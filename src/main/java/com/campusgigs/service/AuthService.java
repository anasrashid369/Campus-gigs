package com.campusgigs.service;

import com.campusgigs.dto.RegisterRequest;
import com.campusgigs.dto.UserDto;

/** Registration and login (F1). */
public interface AuthService {

    /** Minimum password length. A password also needs at least one letter and one digit. */
    int MIN_PASSWORD_LENGTH = 8;

    /**
     * Creates a CLIENT or FREELANCER account together with its wallet, in one transaction.
     * The wallet starts with {@link SettingsService#getInitialTokenBalance()} tokens.
     *
     * @throws com.campusgigs.exception.ValidationException if a field is invalid, the role is
     *         ADMIN, or the email is already registered (case is ignored)
     */
    UserDto register(RegisterRequest request);

    /**
     * Checks the email and password.
     *
     * @throws com.campusgigs.exception.ValidationException with "Invalid email or password" for
     *         an unknown email or a wrong password, or a clear message for a suspended or
     *         deactivated account
     */
    UserDto login(String email, String password);
}
