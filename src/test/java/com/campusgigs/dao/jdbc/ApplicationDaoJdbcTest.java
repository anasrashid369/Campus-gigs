package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigApplication;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class ApplicationDaoJdbcTest {

    private ConnectionProvider provider;
    private ApplicationDaoJdbc applications;
    private long clientId;
    private long firstFreelancerId;
    private long secondFreelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        applications = new ApplicationDaoJdbc(provider);
        clientId = DbTestSupport.createUser(provider, "CLIENT");
        firstFreelancerId = DbTestSupport.createUser(provider, "FREELANCER");
        secondFreelancerId = DbTestSupport.createUser(provider, "FREELANCER");
        categoryId = DbTestSupport.createCategory(provider);
        gigId = DbTestSupport.createGig(provider, clientId, categoryId, 200);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteGig(provider, gigId);
            DbTestSupport.deleteCategory(provider, categoryId);
            DbTestSupport.deleteUser(provider, clientId);
            DbTestSupport.deleteUser(provider, firstFreelancerId);
            DbTestSupport.deleteUser(provider, secondFreelancerId);
        }
    }

    @Test
    void storesAPendingApplication() {
        long id = applications.insert(gigId, firstFreelancerId, "I can do this");

        GigApplication found = applications.findById(id).orElseThrow();
        assertEquals(gigId, found.gigId());
        assertEquals(firstFreelancerId, found.freelancerId());
        assertEquals("I can do this", found.proposalMessage());
        assertEquals(ApplicationStatus.PENDING, found.status());
        assertTrue(applications.findById(-1).isEmpty());
    }

    @Test
    void aFreelancerCanApplyToAGigOnlyOnce() {
        applications.insert(gigId, firstFreelancerId, "First");

        assertThrows(DataAccessException.class, () -> applications.insert(gigId, firstFreelancerId, "Second"));
    }

    @Test
    void findsByGigAndFreelancer() {
        long id = applications.insert(gigId, firstFreelancerId, "Mine");

        assertEquals(id, applications.findByGigAndFreelancer(gigId, firstFreelancerId).orElseThrow().applicationId());
        assertTrue(applications.findByGigAndFreelancer(gigId, secondFreelancerId).isEmpty());
    }

    @Test
    void listsByGigOldestFirstAndByFreelancerNewestFirst() throws Exception {
        long first = applications.insert(gigId, firstFreelancerId, "First");
        long second = applications.insert(gigId, secondFreelancerId, "Second");
        long otherGig = DbTestSupport.createGig(provider, clientId, categoryId, 50);
        try {
            long third = applications.insert(otherGig, firstFreelancerId, "Third");

            List<GigApplication> ofGig = applications.findByGigId(gigId);
            assertEquals(List.of(first, second), ofGig.stream().map(GigApplication::applicationId).toList());
            List<GigApplication> ofFreelancer = applications.findByFreelancerId(firstFreelancerId);
            assertEquals(List.of(third, first), ofFreelancer.stream().map(GigApplication::applicationId).toList());
        } finally {
            DbTestSupport.deleteGig(provider, otherGig);
        }
    }

    @Test
    void countsByStatus() {
        long id = applications.insert(gigId, firstFreelancerId, "First");
        assertEquals(1, applications.countByFreelancerAndStatus(firstFreelancerId, ApplicationStatus.PENDING));

        applications.updateStatus(id, ApplicationStatus.WITHDRAWN);

        assertEquals(0, applications.countByFreelancerAndStatus(firstFreelancerId, ApplicationStatus.PENDING));
        assertEquals(1, applications.countByFreelancerAndStatus(firstFreelancerId, ApplicationStatus.WITHDRAWN));
    }

    @Test
    void reopenTurnsAWithdrawnApplicationIntoANewPendingOne() {
        long id = applications.insert(gigId, firstFreelancerId, "Old message");
        applications.updateStatus(id, ApplicationStatus.WITHDRAWN);

        applications.reopen(id, "New message");

        GigApplication reopened = applications.findById(id).orElseThrow();
        assertEquals(ApplicationStatus.PENDING, reopened.status());
        assertEquals("New message", reopened.proposalMessage());
    }

    @Test
    void rejectPendingExceptLeavesTheAcceptedAndWithdrawnOnesAlone() {
        long accepted = applications.insert(gigId, firstFreelancerId, "Accepted");
        long other = applications.insert(gigId, secondFreelancerId, "Other");
        applications.updateStatus(accepted, ApplicationStatus.ACCEPTED);

        applications.rejectPendingExcept(gigId, accepted);

        assertEquals(ApplicationStatus.ACCEPTED, applications.findById(accepted).orElseThrow().status());
        assertEquals(ApplicationStatus.REJECTED, applications.findById(other).orElseThrow().status());
    }
}
