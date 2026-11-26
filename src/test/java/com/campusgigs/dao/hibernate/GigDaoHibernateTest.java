package com.campusgigs.dao.hibernate;

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
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class GigDaoHibernateTest {

    private SessionFactory db;
    private GigDaoHibernate gigs;
    private long clientId;
    private long freelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        gigs = new GigDaoHibernate(db);
        clientId = DbTestSupport.createUser(db, "CLIENT");
        freelancerId = DbTestSupport.createUser(db, "FREELANCER");
        categoryId = DbTestSupport.createCategory(db);
        gigId = DbTestSupport.createGig(db, clientId, categoryId, 300);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteGig(db, gigId);
            DbTestSupport.deleteCategory(db, categoryId);
            DbTestSupport.deleteUser(db, clientId);
            DbTestSupport.deleteUser(db, freelancerId);
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
            DbTestSupport.deleteGig(db, newId);
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
        long secondGig = DbTestSupport.createGig(db, clientId, categoryId, 50);
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
            DbTestSupport.deleteGig(db, secondGig);
        }
    }

    @Test
    void replaceSkillsKeepsOnlyTheNewSet() throws Exception {
        long skillA = DbTestSupport.createSkill(db);
        long skillB = DbTestSupport.createSkill(db);
        long skillC = DbTestSupport.createSkill(db);
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
            DbTestSupport.deleteSkill(db, skillA);
            DbTestSupport.deleteSkill(db, skillB);
            DbTestSupport.deleteSkill(db, skillC);
        }
    }

    @Test
    void searchOpenFiltersByKeywordCategoryAndBudget() throws Exception {
        long otherCategory = DbTestSupport.createCategory(db);
        long cheap = gigs.insert(clientId, categoryId, "Quick logo sketch", "Just a sketch", 80, LocalDate.of(2031, 1, 1));
        long other = gigs.insert(clientId, otherCategory, "Essay editing", "Proofread my essay", 500, LocalDate.of(2031, 1, 1));
        try {
            assertTrue(ids(gigs.searchOpen("logo", null, null, null)).contains(cheap));
            assertTrue(!ids(gigs.searchOpen("logo", null, null, null)).contains(other));
            assertTrue(ids(gigs.searchOpen("PROOFREAD", null, null, null)).contains(other));
            assertEquals(List.of(other), ids(gigs.searchOpen(null, otherCategory, null, null)));
            assertTrue(ids(gigs.searchOpen(null, categoryId, 100L, null)).contains(gigId));
            assertTrue(!ids(gigs.searchOpen(null, categoryId, 100L, null)).contains(cheap));
            assertTrue(ids(gigs.searchOpen(null, categoryId, null, 100L)).contains(cheap));
            assertTrue(!ids(gigs.searchOpen(null, categoryId, null, 100L)).contains(gigId));
        } finally {
            DbTestSupport.deleteGig(db, cheap);
            DbTestSupport.deleteGig(db, other);
            DbTestSupport.deleteCategory(db, otherCategory);
        }
    }

    @Test
    void searchOpenTreatsPercentAndUnderscoreAsPlainCharacters() throws Exception {
        long percent = gigs.insert(clientId, categoryId, "100% satisfaction", "Percent sign in the title", 10,
                LocalDate.of(2031, 1, 1));
        long plain = gigs.insert(clientId, categoryId, "1000 satisfaction", "No percent sign", 10, LocalDate.of(2031, 1, 1));
        long underscore = gigs.insert(clientId, categoryId, "snake_case helper", "An underscore", 10, LocalDate.of(2031, 1, 1));
        long letters = gigs.insert(clientId, categoryId, "snakeXcase helper", "No underscore", 10, LocalDate.of(2031, 1, 1));
        try {
            List<Long> percentHits = ids(gigs.searchOpen("100%", categoryId, null, null));
            assertTrue(percentHits.contains(percent));
            assertTrue(!percentHits.contains(plain));

            List<Long> underscoreHits = ids(gigs.searchOpen("snake_case", categoryId, null, null));
            assertTrue(underscoreHits.contains(underscore));
            assertTrue(!underscoreHits.contains(letters));
        } finally {
            for (long id : new long[] {percent, plain, underscore, letters}) {
                DbTestSupport.deleteGig(db, id);
            }
        }
    }

    @Test
    void searchOpenNeverReturnsGigsThatAreNotOpen() {
        gigs.assign(gigId, freelancerId, GigStatus.ASSIGNED);

        assertTrue(!ids(gigs.searchOpen(null, categoryId, null, null)).contains(gigId));
    }

    @Test
    void searchOpenIsNotFooledByQuotesInTheKeyword() {
        assertTrue(gigs.searchOpen("x' OR '1'='1", null, null, null).isEmpty());
    }

    @Test
    void findsTheSkillIdsOfSeveralGigsAtOnce() throws Exception {
        long skillA = DbTestSupport.createSkill(db);
        long skillB = DbTestSupport.createSkill(db);
        long second = DbTestSupport.createGig(db, clientId, categoryId, 10);
        try {
            gigs.replaceSkills(gigId, List.of(skillA, skillB));
            gigs.replaceSkills(second, List.of(skillB));

            Map<Long, Set<Long>> result = gigs.findSkillIdsByGigIds(List.of(gigId, second, -1L));

            assertEquals(Set.of(skillA, skillB), result.get(gigId));
            assertEquals(Set.of(skillB), result.get(second));
            assertTrue(!result.containsKey(-1L));
            assertTrue(gigs.findSkillIdsByGigIds(List.of()).isEmpty());
        } finally {
            DbTestSupport.deleteGig(db, second);
            DbTestSupport.deleteSkill(db, skillA);
            DbTestSupport.deleteSkill(db, skillB);
        }
    }

    @Test
    void findAllCanFilterByStatus() {
        gigs.assign(gigId, freelancerId, GigStatus.ASSIGNED);

        assertTrue(ids(gigs.findAll(null)).contains(gigId));
        assertTrue(ids(gigs.findAll(GigStatus.ASSIGNED)).contains(gigId));
        assertTrue(!ids(gigs.findAll(GigStatus.OPEN)).contains(gigId));
    }

    private static List<Long> ids(List<Gig> list) {
        return list.stream().map(Gig::gigId).toList();
    }
}
