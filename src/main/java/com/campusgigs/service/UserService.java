package com.campusgigs.service;

import com.campusgigs.dto.UserDto;

/** Reading and editing a user's own basic profile (F3). */
public interface UserService {

    /** Longest allowed full name, matching Users.full_name. */
    int MAX_NAME_LENGTH = 100;

    /**
     * Returns the user.
     *
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such user
     */
    UserDto getUser(long id);

    /**
     * Changes the user's full name and returns the updated user.
     *
     * @throws com.campusgigs.exception.ValidationException if the name is blank or too long
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such user
     */
    UserDto updateProfile(long userId, String fullName);
}
