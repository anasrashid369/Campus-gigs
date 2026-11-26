package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Needs a local MySQL with db/01_schema.sql loaded and config/config.properties
 * filled in. Skipped when the config file is absent.
 */
class DatabaseConnectionTest {

    @Test
    void connectsAndSeesSchema() {
        long tables = DbTestSupport.queryNumber(DbTestSupport.sessionFactory(),
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'campus_gigs'");

        assertEquals(14, tables);
    }
}
