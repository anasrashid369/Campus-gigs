package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class DisputeDaoJdbcTest {

    private ConnectionProvider provider;
    private DisputeDaoJdbc disputes;
    private long clientId;
    private long adminId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        disputes = new DisputeDaoJdbc(provider);
        clientId = DbTestSupport.createUser(provider, "CLIENT");
        adminId = DbTestSupport.createUser(provider, "ADMIN");
        categoryId = DbTestSupport.createCategory(provider);
        gigId = DbTestSupport.createGig(provider, clientId, categoryId, 100);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteGig(provider, gigId);
            DbTestSupport.deleteCategory(provider, categoryId);
            DbTestSupport.deleteUser(provider, clientId);
            DbTestSupport.deleteUser(provider, adminId);
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
