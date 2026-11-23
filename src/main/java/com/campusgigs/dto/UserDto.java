package com.campusgigs.dto;

import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;

/** A user as the screens see it. Never carries the password hash. */
public record UserDto(long userId, Role role, String fullName, String email, AccountStatus accountStatus) {
}
