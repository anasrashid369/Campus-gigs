package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.model.Gig;
import com.campusgigs.model.Skill;
import com.campusgigs.model.GigStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class GigDaoJdbcTest {

    private ConnectionProvider provider;
    private GigDaoJdbc gigs;
    private long clientId;
    private long freelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        gigs = new GigDaoJdbc(provider);
        clientId = DbTestSupport.createUser(provider, "CLIENT");
        freelancerId = DbTestSupport.createUser(provider, "FREELANCER");
        categoryId = DbTestSupport.createCategory(provider);
        gigId = DbTestSupport.createGig(provider, clientId, categoryId, 300);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteGig(provider, gigId);
            DbTestSupport.deleteCategory(provider, categoryId);
            DbTestSupport.deleteUser(provider, clientId);
            DbTestSupport.deleteUser(provider, freelancerId);
        }
    }

    @Test
    void findsAnOpenGigWithoutAFreelancer() {
        Gig gig = gigs.findById(gigId).orElseThrow();

        assertEquals(clientId, gig.clientId());
        assertEquals(300, gig.budget());
        assertEquals(GigStatus.OPEN, gig.status());
        assertNull(gig.assignedFreelancerId());
    }

    @Test
    void unknownGigIsEmpty() {
        assertTrue(gigs.findById(-1).isEmpty());
        assertTrue(gigs.findByIdForUpdate(-1).isEmpty());
    }

    @Test
    void assignSetsFreelancerAndStatusTogether() {
        gigs.assign(gigId, freelancerId, GigStatus.ASSIGNED);

        Gig gig = gigs.findByIdForUpdate(gigId).orElseThrow();
        assertEquals(freelancerId, gig.assignedFreelancerId());
        assertEquals(GigStatus.ASSIGNED, gig.status());
    }

    @Test
    void updateStatusChangesOnlyTheStatus() {
        gigs.assign(gigId, freelancerId, GigStatus.ASSIGNED);

        gigs.updateStatus(gigId, GigStatus.SUBMITTED);

        Gig gig = gigs.findById(gigId).orElseThrow();
        assertEquals(GigStatus.SUBMITTED, gig.status());
        assertEquals(freelancerId, gig.assignedFreelancerId());
    }

    @Test
    void databaseRefusesAnAssignedStatusWithoutAFreelancer() {
        assertThrows(DataAccessException.class, () -> gigs.updateStatus(gigId, GigStatus.ASSIGNED));
    }

    @Test
    void sumsTheBudgetsOfAFreelancersGigsByStatus() {
        assertEquals(0, gigs.sumBudgetByFreelancerAndStatuses(freelancerId, java.util.EnumSet.allOf(GigStatus.class)));
        assertEquals(0, gigs.sumBudgetByFreelancerAndStatuses(freelancerId, java.util.Set.of()));
        gigs.assign(gigId, freelancerId, GigStatus.ASSIGNED);

        assertEquals(300, gigs.sumBudgetByFreelancerAndStatuses(freelancerId,
                java.util.EnumSet.of(GigStatus.ASSIGNED, GigStatus.SUBMITTED)));
        assertEquals(0, gigs.sumBudgetByFreelancerAndStatuses(freelancerId, java.util.EnumSet.of(GigStatus.APPROVED)));
        assertEquals(0, gigs.sumBudgetByFreelancerAndStatuses(clientId, java.util.EnumSet.of(GigStatus.ASSIGNED)));
    }

    @Test
    void insertStoresAnOpenGigWithoutAFreelancer() throws Exception {
        long newId = gigs.insert(clientId, categoryId, "Poster design", "Design an A3 poster", 450,
                LocalDate.of(2031, 5, 20));
        try {
            Gig gig = gigs.findById(newId).orElseThrow();

            assertEquals("Poster design", gig.title());
            assertEquals("Design an A3 poster", gig.description());
            assertEquals(450, gig.budget());
            assertEquals(LocalDate.of(2031, 5, 20), gig.deadline());
            assertEquals(GigStatus.OPEN, gig.status());
            assertNull(gig.assignedFreelancerId());
        } finally {
            DbTestSupport.deleteGig(provider, newId);
        }
    }

    @Test
    void updateChangesTheEditableFieldsOnly() {
        gigs.update(gigId, categoryId, "New title", "A new description", 700, LocalDate.of(2032, 1, 2));

        Gig gig = gigs.findById(gigId).orElseThrow();
        assertEquals("New title", gig.title());
        assertEquals("A new description", gig.description());
        assertEquals(700, gig.budget());
        assertEquals(LocalDate.of(2032, 1, 2), gig.deadline());
        assertEquals(clientId, gig.clientId());
        assertEquals(GigStatus.OPEN, gig.status());
    }

    @Test
    void listsGigsByClientFreelancerAndStatus() throws Exception {
        long secondGig = DbTestSupport.createGig(provider, clientId, categoryId, 50);
        try {
            gigs.assign(secondGig, freelancerId, GigStatus.ASSIGNED);

            List<Gig> ofClient = gigs.findByClientId(clientId);
            assertEquals(2, ofClient.size());
            assertEquals(secondGig, ofClient.get(0).gigId());
            assertEquals(1, gigs.findByFreelancerId(freelancerId).size());
            assertTrue(gigs.findByStatus(GigStatus.OPEN).stream().anyMatch(g -> g.gigId() == gigId));
            assertTrue(gigs.findByStatus(GigStatus.OPEN).stream().noneMatch(g -> g.gigId() == secondGig));
            assertTrue(gigs.findByClientId(freelancerId).isEmpty());
        } finally {
            DbTestSupport.deleteGig(provider, secondGig);
        }
    }

    @Test
    void replaceSkillsKeepsOnlyTheNewSet() throws Exception {
        long skillA = DbTestSupport.createSkill(provider);
        long skillB = DbTestSupport.createSkill(provider);
        long skillC = DbTestSupport.createSkill(provider);
        try {
            gigs.replaceSkills(gigId, List.of(skillA, skillB));
            assertEquals(2, gigs.findSkills(gigId).size());

            gigs.replaceSkills(gigId, List.of(skillC));
            List<Skill> skills = gigs.findSkills(gigId);
            assertEquals(1, skills.size());
            assertEquals(skillC, skills.get(0).skillId());

            gigs.replaceSkills(gigId, List.of());
            assertTrue(gigs.findSkills(gigId).isEmpty());
        } finally {
            DbTestSupport.deleteSkill(provider, skillA);
            DbTestSupport.deleteSkill(provider, skillB);
            DbTestSupport.deleteSkill(provider, skillC);
        }
    }
}
