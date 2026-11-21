package com.campusgigs.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void toStringNeverContainsPasswordHash() {
        User user = new User(1, Role.CLIENT, "Test User", "t@example.com",
                "SECRET_HASH", AccountStatus.ACTIVE, LocalDateTime.now());

        assertFalse(user.toString().contains("SECRET_HASH"));
        assertTrue(user.toString().contains("t@example.com"));
    }
}
