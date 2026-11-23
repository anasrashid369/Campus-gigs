package com.campusgigs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SessionManagerTest {

    private final SessionManager session = SessionManager.getInstance();

    @AfterEach
    void logout() {
        session.clear();
    }

    @Test
    void isASingleton() {
        assertSame(session, SessionManager.getInstance());
    }

    @Test
    void remembersAndForgetsTheUser() {
        UserDto ali = new UserDto(3, Role.CLIENT, "Ali", "ali@uni.edu.pk", AccountStatus.ACTIVE);

        session.setCurrentUser(ali);
        assertTrue(session.isLoggedIn());
        assertEquals(ali, session.getCurrentUser());

        session.clear();
        assertFalse(session.isLoggedIn());
    }

    @Test
    void readingWithoutLoginFails() {
        assertThrows(IllegalStateException.class, session::getCurrentUser);
        assertThrows(NullPointerException.class, () -> session.setCurrentUser(null));
    }
}
