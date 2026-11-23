package com.campusgigs.service.impl;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.User;

/** Turns a {@link User} into a {@link UserDto}, leaving the password hash behind. */
final class UserMapper {

    private UserMapper() {
    }

    static UserDto toDto(User user) {
        return new UserDto(user.userId(), user.role(), user.fullName(), user.email(), user.accountStatus());
    }
}
