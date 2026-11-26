package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.hibernate.entity.TransactionEntity;
import com.campusgigs.model.LedgerEntry;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Hibernate implementation of {@link TransactionDao}. */
public class TransactionDaoHibernate extends BaseHibernateDao implements TransactionDao {

    public TransactionDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Transaction insert(long walletId, Long gigId, TransactionType type, long amount, String reference) {
        return withSession(session -> {
            TransactionEntity entry = new TransactionEntity(walletId, gigId, type, amount, reference);
            session.insert(entry);
            return new Transaction(entry.getTransactionId(), walletId, gigId, type, amount, reference,
                    LocalDateTime.now());
        });
    }

    @Override
    public List<Transaction> findByWalletId(long walletId) {
        return withSession(session -> session
                .createSelectionQuery("from TransactionEntity where walletId = :walletId "
                        + "order by createdAt desc, transactionId desc", TransactionEntity.class)
                .setParameter("walletId", walletId)
                .getResultList().stream().map(TransactionEntity::toModel).toList());
    }

    @Override
    public long sumByType(long walletId, TransactionType type) {
        return withSession(session -> session
                .createSelectionQuery("select coalesce(sum(t.amount), 0) from TransactionEntity t "
                        + "where t.walletId = :walletId and t.type = :type", Long.class)
                .setParameter("walletId", walletId)
                .setParameter("type", type)
                .getSingleResult());
    }

    @Override
    public List<LedgerEntry> findLedger(TransactionType typeOrNull, LocalDate from, LocalDate to) {
        StringBuilder hql = new StringBuilder("select new com.campusgigs.model.LedgerEntry("
                + "t.transactionId, u.userId, u.fullName, t.type, t.amount, t.gigId, t.reference, t.createdAt) "
                + "from TransactionEntity t "
                + "join WalletEntity w on w.walletId = t.walletId "
                + "join UserEntity u on u.userId = w.userId where 1 = 1");
        if (typeOrNull != null) {
            hql.append(" and t.type = :type");
        }
        if (from != null) {
            hql.append(" and t.createdAt >= :from");
        }
        if (to != null) {
            hql.append(" and t.createdAt < :to");
        }
        hql.append(" order by t.createdAt desc, t.transactionId desc");
        return withSession(session -> {
            SelectionQuery<LedgerEntry> query = session.createSelectionQuery(hql.toString(), LedgerEntry.class);
            if (typeOrNull != null) {
                query.setParameter("type", typeOrNull);
            }
            if (from != null) {
                query.setParameter("from", from.atStartOfDay());
            }
            if (to != null) {
                query.setParameter("to", to.plusDays(1).atStartOfDay());
            }
            return query.setMaxResults(MAX_LEDGER_ROWS).getResultList();
        });
    }
}
