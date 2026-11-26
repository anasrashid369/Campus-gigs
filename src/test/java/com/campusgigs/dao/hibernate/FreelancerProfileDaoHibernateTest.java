package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Skill;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class FreelancerProfileDaoHibernateTest {

    private SessionFactory db;
    private FreelancerProfileDaoHibernate profiles;
    private SkillDaoHibernate skills;
    private long userId;
    private long skillA;
    private long skillB;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        profiles = new FreelancerProfileDaoHibernate(db);
        skills = new SkillDaoHibernate(db);
        userId = DbTestSupport.createUser(db);
        skillA = skills.insert("A " + UUID.randomUUID());
        skillB = skills.insert("B " + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db == null) {
            return;
        }
        DbTestSupport.execute(db, "DELETE FROM FreelancerProfiles WHERE user_id = ?1", userId);
        DbTestSupport.deleteUser(db, userId);
        for (long id : new long[] {skillA, skillB}) {
            DbTestSupport.execute(db, "DELETE FROM Skills WHERE skill_id = ?1", id);
        }
    }

    @Test
    void insertsThenUpdatesProfile() {
        assertTrue(profiles.findByUserId(userId).isEmpty());

        profiles.insert(userId, "Bio", "");
        profiles.update(userId, "New bio", "github.com/test");

        FreelancerProfile profile = profiles.findByUserId(userId).orElseThrow();
        assertEquals("New bio", profile.biography());
        assertEquals("github.com/test", profile.portfolioLinks());
    }

    @Test
    void replaceSkillsKeepsOnlyTheNewList() {
        profiles.replaceSkills(userId, List.of(skillA, skillB));
        profiles.replaceSkills(userId, List.of(skillB));

        List<Skill> current = profiles.findSkills(userId);
        assertEquals(1, current.size());
        assertEquals(skillB, current.get(0).skillId());

        profiles.replaceSkills(userId, List.of());
        assertTrue(profiles.findSkills(userId).isEmpty());
    }
}
