package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Skill;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class FreelancerProfileDaoJdbcTest {

    private ConnectionProvider provider;
    private FreelancerProfileDaoJdbc profiles;
    private SkillDaoJdbc skills;
    private long userId;
    private long skillA;
    private long skillB;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        profiles = new FreelancerProfileDaoJdbc(provider);
        skills = new SkillDaoJdbc(provider);
        userId = DbTestSupport.createUser(provider);
        skillA = skills.insert("A " + UUID.randomUUID());
        skillB = skills.insert("B " + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider == null) {
            return;
        }
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM FreelancerProfiles WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
        DbTestSupport.deleteUser(provider, userId);
        for (long id : new long[] {skillA, skillB}) {
            try (Connection c = provider.getConnection();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM Skills WHERE skill_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
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
