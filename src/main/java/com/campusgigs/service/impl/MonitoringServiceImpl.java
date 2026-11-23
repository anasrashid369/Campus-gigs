package com.campusgigs.service.impl;

import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.LedgerEntryDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigApplication;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.User;
import com.campusgigs.service.MonitoringService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Default {@link MonitoringService}. */
public class MonitoringServiceImpl implements MonitoringService {

    private final GigDao gigDao;
    private final ApplicationDao applicationDao;
    private final TransactionDao transactionDao;
    private final UserDao userDao;

    public MonitoringServiceImpl(GigDao gigDao, ApplicationDao applicationDao, TransactionDao transactionDao,
                                 UserDao userDao) {
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.applicationDao = Objects.requireNonNull(applicationDao, "applicationDao");
        this.transactionDao = Objects.requireNonNull(transactionDao, "transactionDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
    }

    @Override
    public List<GigDto> listGigs(long adminId, GigStatus statusOrNull) {
        requireAdmin(adminId);
        return gigDao.findAll(statusOrNull).stream().map(GigMapper::toDto).toList();
    }

    @Override
    public List<ApplicationDto> listApplications(long adminId, ApplicationStatus statusOrNull) {
        requireAdmin(adminId);
        return applicationDao.findAll(statusOrNull).stream().map(this::toDto).toList();
    }

    @Override
    public List<LedgerEntryDto> listLedger(long adminId, TransactionType typeOrNull, LocalDate from, LocalDate to) {
        requireAdmin(adminId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("The first day cannot be after the last day.");
        }
        return transactionDao.findLedger(typeOrNull, from, to).stream()
                .map(entry -> new LedgerEntryDto(entry.transactionId(), entry.userName(), entry.type(),
                        entry.amount(), entry.gigId(), entry.reference(), entry.createdAt()))
                .toList();
    }

    private ApplicationDto toDto(GigApplication application) {
        Gig gig = gigDao.findById(application.gigId())
                .orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
        User freelancer = userDao.findById(application.freelancerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new ApplicationDto(application.applicationId(), gig.gigId(), gig.title(), gig.status(),
                freelancer.userId(), freelancer.fullName(), application.proposalMessage(), application.status(),
                application.submittedAt());
    }

    private void requireAdmin(long userId) {
        User user = userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.role() != Role.ADMIN || user.accountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Only an administrator can monitor the platform.");
        }
    }
}
