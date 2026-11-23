package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
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
}
