package com.campusgigs.service;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.LedgerEntryDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.TransactionType;

import java.time.LocalDate;
import java.util.List;

/**
 * Administrator monitoring of gigs, applications and the token ledger (F9).
 * Only an active ADMIN may use it; the check is made in every method.
 */
public interface MonitoringService {

    /** Lists all gigs, or those in one status, newest first. */
    List<GigDto> listGigs(long adminId, GigStatus statusOrNull);

    /** Lists all applications, or those in one status, newest first. */
    List<ApplicationDto> listApplications(long adminId, ApplicationStatus statusOrNull);

    /**
     * Lists ledger entries of all wallets, newest first, at most
     * {@link com.campusgigs.dao.TransactionDao#MAX_LEDGER_ROWS} rows. A null condition is ignored.
     *
     * @throws com.campusgigs.exception.ValidationException if the caller is not an admin or {@code from} is after {@code to}
     */
    List<LedgerEntryDto> listLedger(long adminId, TransactionType typeOrNull, LocalDate from, LocalDate to);
}
