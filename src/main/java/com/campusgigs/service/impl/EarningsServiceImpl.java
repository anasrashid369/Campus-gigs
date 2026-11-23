package com.campusgigs.service.impl;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.EarningsDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import com.campusgigs.service.EarningsService;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Default {@link EarningsService}. */
public class EarningsServiceImpl implements EarningsService {

    /** Statuses in which the freelancer has been hired but not yet paid. */
    private static final Set<GigStatus> UNPAID = EnumSet.of(GigStatus.ASSIGNED, GigStatus.SUBMITTED, GigStatus.DISPUTED);

    private final UserDao userDao;
    private final WalletDao walletDao;
    private final TransactionDao transactionDao;
    private final GigDao gigDao;

    public EarningsServiceImpl(UserDao userDao, WalletDao walletDao, TransactionDao transactionDao, GigDao gigDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.walletDao = Objects.requireNonNull(walletDao, "walletDao");
        this.transactionDao = Objects.requireNonNull(transactionDao, "transactionDao");
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
    }

    @Override
    public EarningsDto getEarnings(long userId) {
        Role role = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .role();
        if (role != Role.FREELANCER) {
            throw new ValidationException("Only freelancers have earnings");
        }
        Wallet wallet = walletDao.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        long received = transactionDao.sumByType(wallet.walletId(), TransactionType.ESCROW_RELEASE);
        long fees = transactionDao.sumByType(wallet.walletId(), TransactionType.PLATFORM_FEE);
        long withdrawn = transactionDao.sumByType(wallet.walletId(), TransactionType.WITHDRAWAL);
        long pending = gigDao.sumBudgetByFreelancerAndStatuses(userId, UNPAID);
        return new EarningsDto(wallet.availableBalance(), received - fees, fees, withdrawn, pending);
    }
}
