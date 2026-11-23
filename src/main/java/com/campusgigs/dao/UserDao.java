package com.campusgigs.dao;

import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;

import java.util.List;
import java.util.Optional;

/** Data access for the Users table. Contains SQL and mapping only. */
public interface UserDao {

    /** Finds a user by id. */
    Optional<User> findById(long id);

    /** Finds a user by email. Callers pass the email trimmed and in lower case. */
    Optional<User> findByEmail(String email);

    /**
     * Inserts an ACTIVE user.
     *
     * @return the new user's id
     */
    long insert(Role role, String fullName, String email, String passwordHash);

    /** Returns every user, oldest first. */
    List<User> findAll();

    /** Changes the user's account status. */
    void updateAccountStatus(long userId, AccountStatus status);

    /** Changes the user's full name. */
    void updateName(long userId, String fullName);
}
