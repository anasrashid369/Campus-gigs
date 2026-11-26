package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.NotificationDao;
import com.campusgigs.dao.hibernate.entity.NotificationEntity;
import com.campusgigs.model.Notification;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link NotificationDao}. */
public class NotificationDaoHibernate extends BaseHibernateDao implements NotificationDao {

    private static final String NEWEST_FIRST = " order by createdAt desc, notificationId desc";

    public NotificationDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public long insert(long userId, String message) {
        return withSession(session -> {
            NotificationEntity notification = new NotificationEntity(userId, message);
            session.insert(notification);
            return notification.getNotificationId();
        });
    }

    @Override
    public Optional<Notification> findById(long notificationId) {
        return withSession(session -> Optional.ofNullable(session.get(NotificationEntity.class, notificationId))
                .map(NotificationEntity::toModel));
    }

    @Override
    public List<Notification> findByUserId(long userId) {
        return findList("from NotificationEntity where userId = :userId" + NEWEST_FIRST, userId);
    }

    @Override
    public List<Notification> findUnreadByUserId(long userId) {
        return findList("from NotificationEntity where userId = :userId and read = false" + NEWEST_FIRST, userId);
    }

    @Override
    public int countUnread(long userId) {
        return withSession(session -> session
                .createSelectionQuery("select count(*) from NotificationEntity where userId = :userId "
                        + "and read = false", Long.class)
                .setParameter("userId", userId)
                .getSingleResult()
                .intValue());
    }

    @Override
    public void markAsRead(long notificationId) {
        inSession(session -> session
                .createMutationQuery("update NotificationEntity set read = true where notificationId = :id")
                .setParameter("id", notificationId)
                .executeUpdate());
    }

    @Override
    public void markAllAsRead(long userId) {
        inSession(session -> session
                .createMutationQuery("update NotificationEntity set read = true where userId = :userId "
                        + "and read = false")
                .setParameter("userId", userId)
                .executeUpdate());
    }

    private List<Notification> findList(String hql, long userId) {
        return withSession(session -> session.createSelectionQuery(hql, NotificationEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream().map(NotificationEntity::toModel).toList());
    }
}
