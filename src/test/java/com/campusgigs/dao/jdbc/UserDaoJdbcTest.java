package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class UserDaoJdbcTest {

    private ConnectionProvider provider;
    private UserDaoJdbc users;
    private String email;
    private long userId;

    @BeforeEach
    void setUp() {
        provider = DbTestSupport.provider();
        users = new UserDaoJdbc(provider);
        email = "dao-" + UUID.randomUUID() + "@example.com";
        userId = users.insert(Role.FREELANCER, "Dao Test", email, "pbkdf2$test");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteUser(provider, userId);
        }
    }

    @Test
    void insertedUserIsFoundByIdAndEmail() {
        User byId = users.findById(userId).orElseThrow();

        assertEquals(Role.FREELANCER, byId.role());
        assertEquals("Dao Test", byId.fullName());
        assertEquals(AccountStatus.ACTIVE, byId.accountStatus());
        assertEquals(userId, users.findByEmail(email).orElseThrow().userId());
    }

    @Test
    void updatesNameAndStatus() {
        users.updateName(userId, "Renamed");
        users.updateAccountStatus(userId, AccountStatus.SUSPENDED);

        User user = users.findById(userId).orElseThrow();
        assertEquals("Renamed", user.fullName());
        assertEquals(AccountStatus.SUSPENDED, user.accountStatus());
    }

    @Test
    void findAllContainsTheUser() {
        assertTrue(users.findAll().stream().anyMatch(u -> u.userId() == userId));
    }
}
