package com.campusgigs.util;

import com.campusgigs.dto.UserDto;

import java.util.Objects;

/**
 * Remembers who is logged in for the running app. In memory only, no database.
 * Screens read the current user from here and pass its id to the services.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private volatile UserDto currentUser;

    private SessionManager() {
    }

    /** Returns the one shared session. */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Stores the user who just logged in (or the updated copy after a profile change). */
    public void setCurrentUser(UserDto user) {
        currentUser = Objects.requireNonNull(user, "user");
    }

    /**
     * Returns the logged-in user.
     *
     * @throws IllegalStateException if nobody is logged in
     */
    public UserDto getCurrentUser() {
        UserDto user = currentUser;
        if (user == null) {
            throw new IllegalStateException("No user is logged in");
        }
        return user;
    }

    /** Forgets the current user (logout). */
    public void clear() {
        currentUser = null;
    }

    /** True when someone is logged in. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
