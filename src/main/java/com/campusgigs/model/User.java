package com.campusgigs.model;

import java.time.LocalDateTime;

/** A platform user (row of Users). */
public record User(long userId, Role role, String fullName, String email,
                   String passwordHash, AccountStatus accountStatus, LocalDateTime createdAt) {

    /** Omits the password hash so it can never leak into logs. */
    @Override
    public String toString() {
        return "User[userId=" + userId + ", role=" + role + ", fullName=" + fullName
                + ", email=" + email + ", accountStatus=" + accountStatus + "]";
    }
}
