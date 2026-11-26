package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class DisputeDaoHibernateTest {

    private SessionFactory db;
    private DisputeDaoHibernate disputes;
    private long clientId;
    private long adminId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        disputes = new DisputeDaoHibernate(db);
        clientId = DbTestSupport.createUser(db, "CLIENT");
        adminId = DbTestSupport.createUser(db, "ADMIN");
        categoryId = DbTestSupport.createCategory(db);
        gigId = DbTestSupport.createGig(db, clientId, categoryId, 100);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteGig(db, gigId);
            DbTestSupport.deleteCategory(db, categoryId);
            DbTestSupport.deleteUser(db, clientId);
            DbTestSupport.deleteUser(db, adminId);
        }
    }

    @Test
    void storesAnOpenDisputeWithTheReason() {
        Dispute created = disputes.insert(gigId, clientId, "Work is incomplete");

        Dispute found = disputes.findById(created.disputeId()).orElseThrow();
        assertEquals("Work is incomplete", found.reason());
        assertEquals(DisputeStatus.OPEN, found.status());
        assertNull(found.decision());
        assertNull(found.resolvedBy());
        assertNull(found.resolvedAt());
    }

    @Test
    void findsTheOpenDisputeOfAGig() {
        assertTrue(disputes.findOpenByGigId(gigId).isEmpty());
        Dispute created = disputes.insert(gigId, clientId, "Late");

        assertEquals(created.disputeId(), disputes.findOpenByGigId(gigId).orElseThrow().disputeId());
        assertTrue(disputes.findByIdForUpdate(created.disputeId()).isPresent());
    }

    @Test
    void resolveRecordsTheDecisionOnce() {
        Dispute created = disputes.insert(gigId, clientId, "Wrong files");

        assertTrue(disputes.resolve(created.disputeId(), adminId, DisputeDecision.REFUND));
        assertFalse(disputes.resolve(created.disputeId(), adminId, DisputeDecision.RELEASE));

        Dispute resolved = disputes.findById(created.disputeId()).orElseThrow();
        assertEquals(DisputeStatus.RESOLVED, resolved.status());
        assertEquals(DisputeDecision.REFUND, resolved.decision());
        assertEquals(adminId, resolved.resolvedBy());
        assertTrue(disputes.findOpenByGigId(gigId).isEmpty());
    }

    @Test
    void listsDisputesByStatus() {
        Dispute created = disputes.insert(gigId, clientId, "Reason");

        assertTrue(disputes.findByStatus(DisputeStatus.OPEN).stream()
                .anyMatch(d -> d.disputeId() == created.disputeId()));
        assertFalse(disputes.findByStatus(DisputeStatus.RESOLVED).stream()
                .anyMatch(d -> d.disputeId() == created.disputeId()));
    }
}
