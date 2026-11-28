package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.hibernate.entity.CategoryEntity;
import com.campusgigs.dao.hibernate.entity.GigEntity;
import com.campusgigs.dao.hibernate.entity.SkillEntity;
import com.campusgigs.dao.hibernate.entity.UserEntity;
import com.campusgigs.model.Role;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Assumptions;

/** Helpers for tests that need the local MySQL database; they skip when it is not configured. */
final class DbTestSupport {

    private DbTestSupport() {
    }

    static SessionFactory sessionFactory() {
        Assumptions.assumeTrue(Files.isRegularFile(Paths.get("config/config.properties")),
                "config/config.properties not found");
        SessionFactory db = HibernateSessionFactory.getInstance();
        if (reachable == null) {
            reachable = canConnect(db);
        }
        Assumptions.assumeTrue(reachable, "MySQL is not running or not reachable with config/config.properties");
        return db;
    }

    /** Checked once per test run, so a stopped MySQL costs one connection timeout instead of one per test. */
    private static Boolean reachable;

    private static boolean canConnect(SessionFactory db) {
        try {
            queryNumber(db, "SELECT COUNT(*) FROM information_schema.schemata");
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Inserts a throwaway client user and returns its id. */
    static long createUser(SessionFactory db) {
        return createUser(db, "CLIENT");
    }

    /** Inserts a throwaway user with the given role (ADMIN, CLIENT or FREELANCER) and returns its id. */
    static long createUser(SessionFactory db, String role) {
        UserEntity user = new UserEntity(Role.valueOf(role), "Test", "test-" + UUID.randomUUID() + "@example.com",
                "x");
        insert(db, user);
        return user.getUserId();
    }

    /** Inserts a throwaway gig category and returns its id. */
    static long createCategory(SessionFactory db) {
        CategoryEntity category = new CategoryEntity("test-" + UUID.randomUUID());
        insert(db, category);
        return category.getCategoryId();
    }

    /** Inserts a throwaway skill and returns its id. */
    static long createSkill(SessionFactory db) {
        SkillEntity skill = new SkillEntity("test-" + UUID.randomUUID());
        insert(db, skill);
        return skill.getSkillId();
    }

    /** Removes a test skill (rows in GigSkills and UserSkills that use it go with it). */
    static void deleteSkill(SessionFactory db, long skillId) {
        execute(db, "DELETE FROM Skills WHERE skill_id = ?1", skillId);
    }

    /** Inserts an OPEN gig of the client with the given budget and returns its id. */
    static long createGig(SessionFactory db, long clientId, long categoryId, long budget) {
        GigEntity gig = new GigEntity(clientId, categoryId, "Test gig", "Test description", budget,
                LocalDate.of(2030, 1, 1));
        insert(db, gig);
        return gig.getGigId();
    }

    /** Removes a test gig together with its review, disputes and ledger rows. */
    static void deleteGig(SessionFactory db, long gigId) {
        execute(db, "DELETE FROM Reviews WHERE gig_id = ?1", gigId);
        execute(db, "DELETE FROM Disputes WHERE gig_id = ?1", gigId);
        execute(db, "DELETE FROM Transactions WHERE gig_id = ?1", gigId);
        execute(db, "DELETE FROM Gigs WHERE gig_id = ?1", gigId);
    }

    /** Removes a test category. */
    static void deleteCategory(SessionFactory db, long categoryId) {
        execute(db, "DELETE FROM GigCategories WHERE category_id = ?1", categoryId);
    }

    /** Removes the throwaway user together with its wallet and ledger rows. */
    static void deleteUser(SessionFactory db, long userId) {
        execute(db, "DELETE FROM Transactions WHERE wallet_id IN (SELECT wallet_id FROM Wallets WHERE user_id = ?1)",
                userId);
        execute(db, "DELETE FROM Wallets WHERE user_id = ?1", userId);
        execute(db, "DELETE FROM Users WHERE user_id = ?1", userId);
    }

    /** Runs one SQL statement for test set-up or clean-up; parameters are ?1, ?2, ... */
    static int execute(SessionFactory db, String sql, Object... parameters) {
        return inTransaction(db, session -> {
            var statement = session.createNativeMutationQuery(sql);
            for (int i = 0; i < parameters.length; i++) {
                statement.setParameter(i + 1, parameters[i]);
            }
            return statement.executeUpdate();
        });
    }

    /** Runs a query that returns one number, for example a COUNT; parameters are ?1, ?2, ... */
    static long queryNumber(SessionFactory db, String sql, Object... parameters) {
        return inTransaction(db, session -> {
            var query = session.createNativeQuery(sql, Long.class);
            for (int i = 0; i < parameters.length; i++) {
                query.setParameter(i + 1, parameters[i]);
            }
            return query.getSingleResult();
        });
    }

    private static <T> T inTransaction(SessionFactory db, Function<StatelessSession, T> work) {
        try (StatelessSession session = db.openStatelessSession()) {
            Transaction transaction = session.beginTransaction();
            T result = work.apply(session);
            transaction.commit();
            return result;
        }
    }

    private static void insert(SessionFactory db, Object entity) {
        inTransaction(db, session -> session.insert(entity));
    }
}
