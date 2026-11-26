package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

class HibernateSessionFactoryTest {

    @Test
    void addsTheSessionTimeZoneOptionToTheUrl() {
        assertEquals("jdbc:mysql://h/db?useSSL=false&forceConnectionTimeZoneToSession=true",
                HibernateSessionFactory.withSessionTimeZone("jdbc:mysql://h/db?useSSL=false"));
        assertEquals("jdbc:mysql://h/db?forceConnectionTimeZoneToSession=true",
                HibernateSessionFactory.withSessionTimeZone("jdbc:mysql://h/db"));
    }

    @Test
    void keepsAnOptionTheUrlAlreadySets() {
        String url = "jdbc:mysql://h/db?forceConnectionTimeZoneToSession=false";

        assertEquals(url, HibernateSessionFactory.withSessionTimeZone(url));
    }

    @Test
    void startsWithoutConnectingSoTheAppOpensWhenMySqlIsDown() {
        SessionFactory factory = assertDoesNotThrow(() -> HibernateSessionFactory.build(
                "jdbc:mysql://127.0.0.1:1/nothing?connectTimeout=500", "nobody", "x"));
        factory.close();
    }
}
