package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.hibernate.entity.ApplicationEntity;
import com.campusgigs.dao.hibernate.entity.CategoryEntity;
import com.campusgigs.dao.hibernate.entity.DisputeEntity;
import com.campusgigs.dao.hibernate.entity.FreelancerProfileEntity;
import com.campusgigs.dao.hibernate.entity.GigEntity;
import com.campusgigs.dao.hibernate.entity.GigSkillEntity;
import com.campusgigs.dao.hibernate.entity.NotificationEntity;
import com.campusgigs.dao.hibernate.entity.SettingEntity;
import com.campusgigs.dao.hibernate.entity.SkillEntity;
import com.campusgigs.dao.hibernate.entity.TransactionEntity;
import com.campusgigs.dao.hibernate.entity.UserEntity;
import com.campusgigs.dao.hibernate.entity.UserSkillEntity;
import com.campusgigs.dao.hibernate.entity.WalletEntity;
import com.campusgigs.util.ConfigLoader;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Builds the one Hibernate {@link SessionFactory} of the application from config.properties
 * (db.url, db.user, db.password) and the entity classes. The tables come from
 * db/01_schema.sql; Hibernate never creates or changes them.
 */
public final class HibernateSessionFactory {

    /** Every mapped table; add the entity class here when a new table gets a DAO. */
    private static final Class<?>[] ENTITIES = {
        UserEntity.class, WalletEntity.class, TransactionEntity.class, SettingEntity.class,
        CategoryEntity.class, SkillEntity.class, UserSkillEntity.class, FreelancerProfileEntity.class,
        GigEntity.class, GigSkillEntity.class, ApplicationEntity.class, DisputeEntity.class,
        NotificationEntity.class,
    };

    private static SessionFactory instance;

    private HibernateSessionFactory() {
    }

    /** The shared session factory, built on first use. */
    public static synchronized SessionFactory getInstance() {
        if (instance == null) {
            ConfigLoader config = ConfigLoader.getInstance();
            instance = build(config.getRequired("db.url"), config.getRequired("db.user"),
                    config.getRequired("db.password"));
        }
        return instance;
    }

    /** Builds a session factory for the given database; tests use the shared one. */
    static SessionFactory build(String url, String user, String password) {
        Configuration configuration = new Configuration()
                .setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver")
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", user)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.connection.pool_size", "5")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                // Do not connect while starting up (empty pool, no metadata lookup): the login
                // screen opens even if MySQL is down, and the first database call reports it.
                .setProperty("hibernate.connection.initial_pool_size", "0")
                .setProperty("hibernate.connection.min_pool_size", "0")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .setProperty("hibernate.show_sql", "false");
        for (Class<?> entity : ENTITIES) {
            configuration.addAnnotatedClass(entity);
        }
        return configuration.buildSessionFactory();
    }
}
