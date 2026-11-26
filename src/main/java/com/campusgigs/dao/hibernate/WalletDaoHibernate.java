package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.WalletDao;
import com.campusgigs.dao.hibernate.entity.WalletEntity;
import com.campusgigs.model.Wallet;
import jakarta.persistence.LockModeType;
import org.hibernate.SessionFactory;

import java.util.Optional;

/** Hibernate implementation of {@link WalletDao}. */
public class WalletDaoHibernate extends BaseHibernateDao implements WalletDao {

    private static final String BY_USER = "from WalletEntity where userId = :userId";

    public WalletDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<Wallet> findByUserId(long userId) {
        return withSession(session -> session.createSelectionQuery(BY_USER, WalletEntity.class)
                .setParameter("userId", userId)
                .uniqueResultOptional()
                .map(WalletEntity::toModel));
    }

    @Override
    public Optional<Wallet> findByUserIdForUpdate(long userId) {
        // PESSIMISTIC_WRITE makes Hibernate add FOR UPDATE: the row stays locked until the transaction ends.
        return withSession(session -> session.createSelectionQuery(BY_USER, WalletEntity.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .uniqueResultOptional()
                .map(WalletEntity::toModel));
    }

    @Override
    public Wallet create(long userId, long availableBalance) {
        return withSession(session -> {
            WalletEntity wallet = new WalletEntity(userId, availableBalance);
            session.insert(wallet);
            return wallet.toModel();
        });
    }

    @Override
    public void updateBalances(long walletId, long availableBalance, long escrowBalance) {
        inSession(session -> session
                .createMutationQuery("update WalletEntity set availableBalance = :available, "
                        + "escrowBalance = :escrow, rowVersion = rowVersion + 1 where walletId = :id")
                .setParameter("available", availableBalance)
                .setParameter("escrow", escrowBalance)
                .setParameter("id", walletId)
                .executeUpdate());
    }
}
