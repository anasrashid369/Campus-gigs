package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigApplication;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class ApplicationDaoHibernateTest {

    private SessionFactory db;
    private ApplicationDaoHibernate applications;
    private long clientId;
    private long firstFreelancerId;
    private long secondFreelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        applications = new ApplicationDaoHibernate(db);
        clientId = DbTestSupport.createUser(db, "CLIENT");
        firstFreelancerId = DbTestSupport.createUser(db, "FREELANCER");
        secondFreelancerId = DbTestSupport.createUser(db, "FREELANCER");
        categoryId = DbTestSupport.createCategory(db);
        gigId = DbTestSupport.createGig(db, clientId, categoryId, 200);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteGig(db, gigId);
            DbTestSupport.deleteCategory(db, categoryId);
            DbTestSupport.deleteUser(db, clientId);
            DbTestSupport.deleteUser(db, firstFreelancerId);
            DbTestSupport.deleteUser(db, secondFreelancerId);
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
        long otherGig = DbTestSupport.createGig(db, clientId, categoryId, 50);
        try {
            long third = applications.insert(otherGig, firstFreelancerId, "Third");

            List<GigApplication> ofGig = applications.findByGigId(gigId);
            assertEquals(List.of(first, second), ofGig.stream().map(GigApplication::applicationId).toList());
            List<GigApplication> ofFreelancer = applications.findByFreelancerId(firstFreelancerId);
            assertEquals(List.of(third, first), ofFreelancer.stream().map(GigApplication::applicationId).toList());
        } finally {
            DbTestSupport.deleteGig(db, otherGig);
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

    @Test
    void findAllCanFilterByStatus() {
        long pending = applications.insert(gigId, firstFreelancerId, "Pending one");
        long rejected = applications.insert(gigId, secondFreelancerId, "Rejected one");
        applications.updateStatus(rejected, ApplicationStatus.REJECTED);

        assertTrue(applications.findAll(null).stream().anyMatch(a -> a.applicationId() == pending));
        assertTrue(applications.findAll(ApplicationStatus.PENDING).stream().anyMatch(a -> a.applicationId() == pending));
        assertTrue(applications.findAll(ApplicationStatus.PENDING).stream().noneMatch(a -> a.applicationId() == rejected));
        assertTrue(applications.findAll(ApplicationStatus.REJECTED).stream().anyMatch(a -> a.applicationId() == rejected));
    }
}
