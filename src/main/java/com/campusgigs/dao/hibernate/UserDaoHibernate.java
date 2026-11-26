package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.UserDao;
import com.campusgigs.dao.hibernate.entity.UserEntity;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link UserDao}. */
public class UserDaoHibernate extends BaseHibernateDao implements UserDao {

    public UserDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<User> findById(long id) {
        return withSession(session -> Optional.ofNullable(session.get(UserEntity.class, id))
                .map(UserEntity::toModel));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return withSession(session -> session
                .createSelectionQuery("from UserEntity where email = :email", UserEntity.class)
                .setParameter("email", email)
                .uniqueResultOptional()
                .map(UserEntity::toModel));
    }

    @Override
    public long insert(Role role, String fullName, String email, String passwordHash) {
        return withSession(session -> {
            UserEntity user = new UserEntity(role, fullName, email, passwordHash);
            session.insert(user);
            return user.getUserId();
        });
    }

    @Override
    public List<User> findAll() {
        return withSession(session -> session
                .createSelectionQuery("from UserEntity order by userId", UserEntity.class)
                .getResultList().stream().map(UserEntity::toModel).toList());
    }

    @Override
    public void updateAccountStatus(long userId, AccountStatus status) {
        inSession(session -> session
                .createMutationQuery("update UserEntity set accountStatus = :status where userId = :id")
                .setParameter("status", status)
                .setParameter("id", userId)
                .executeUpdate());
    }

    @Override
    public void updateName(long userId, String fullName) {
        inSession(session -> session
                .createMutationQuery("update UserEntity set fullName = :name where userId = :id")
                .setParameter("name", fullName)
                .setParameter("id", userId)
                .executeUpdate());
    }
}
