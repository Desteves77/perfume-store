package com.perfumestore.backend.notification;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.perfumestore.backend.shared.api.PageResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationRepository {
    private final JdbcTemplate jdbc;

    public NotificationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean insertIfAbsent(NotificationRecord n) {
        return jdbc.update("""
                insert into perfumestore.notifications
                    (id, event_type, event_key, order_id, user_id, recipient_email, template_name, template_version,
                     subject, body_text, body_html, status, attempt_count, next_attempt_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (event_key) do nothing
                """, n.id(), n.eventType().name(), n.eventKey(), n.orderId(), n.userId(), n.recipientEmail(),
                n.templateName(), n.templateVersion(), n.subject(), n.bodyText(), n.bodyHtml(), n.status().name(), n.attemptCount(),
                Timestamp.from(n.nextAttemptAt()), Timestamp.from(n.createdAt()), Timestamp.from(n.updatedAt())) == 1;
    }

    @Transactional
    public Optional<NotificationRecord> claimDue(Instant now, Instant leaseUntil) {
        List<NotificationRecord> claimed = jdbc.query("""
                with candidate as (
                    select id
                    from perfumestore.notifications
                    where (status in ('PENDING','RETRY_PENDING') and next_attempt_at <= ?)
                       or (status = 'PROCESSING' and locked_until < ?)
                    order by created_at, id
                    for update skip locked
                    limit 1
                )
                update perfumestore.notifications n
                   set status = 'PROCESSING', attempt_count = n.attempt_count + 1,
                       last_attempt_at = ?, locked_until = ?, updated_at = ?
                  from candidate c
                 where n.id = c.id
                returning n.*
                """, this::map, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now),
                Timestamp.from(leaseUntil), Timestamp.from(now));
        if (claimed.isEmpty()) return Optional.empty();
        NotificationRecord result = claimed.get(0);
        jdbc.update("""
                insert into perfumestore.notification_attempts
                    (id, notification_id, attempt_number, started_at)
                values (?, ?, ?, ?)
                """, UUID.randomUUID(), result.id(), result.attemptCount(), Timestamp.from(now));
        return Optional.of(result);
    }

    @Transactional
    public void markSent(UUID id, int attempt, Instant now) {
        jdbc.update("""
                update perfumestore.notifications
                   set status = 'SENT', sent_at = ?, locked_until = null, last_error_code = null, updated_at = ?
                 where id = ? and status = 'PROCESSING' and attempt_count = ?
                """, Timestamp.from(now), Timestamp.from(now), id, attempt);
        jdbc.update("""
                update perfumestore.notification_attempts
                   set finished_at = ?, successful = true
                 where notification_id = ? and attempt_number = ?
                """, Timestamp.from(now), id, attempt);
    }

    @Transactional
    public void markFailure(UUID id, int attempt, String errorCode, Instant nextAttempt, boolean terminal, Instant now) {
        jdbc.update("""
                update perfumestore.notifications
                   set status = ?, next_attempt_at = ?, locked_until = null, last_error_code = ?, updated_at = ?
                 where id = ? and status = 'PROCESSING' and attempt_count = ?
                """, terminal ? NotificationStatus.FAILED.name() : NotificationStatus.RETRY_PENDING.name(),
                Timestamp.from(nextAttempt), errorCode, Timestamp.from(now), id, attempt);
        jdbc.update("""
                update perfumestore.notification_attempts
                   set finished_at = ?, successful = false, error_code = ?
                 where notification_id = ? and attempt_number = ?
                """, Timestamp.from(now), errorCode, id, attempt);
    }

    public Optional<NotificationRecord> findByEventKey(String eventKey) {
        return jdbc.query("select * from perfumestore.notifications where event_key = ?", this::map, eventKey)
                .stream().findFirst();
    }

    public PageResponse<NotificationDtos.Summary> page(int page, int size, String sortColumn, String direction,
            String status, String eventType) {
        int offset = Math.multiplyExact(page, size);
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null) { where.append(" and status = ?"); args.add(status); }
        if (eventType != null) { where.append(" and event_type = ?"); args.add(eventType); }
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size); listArgs.add(offset);
        List<NotificationDtos.Summary> items = jdbc.query("select id, event_type, status, order_id, user_id, recipient_email, attempt_count, next_attempt_at, created_at, sent_at, last_error_code from perfumestore.notifications" + where + " order by " + sortColumn + " " + direction + ", id desc limit ? offset ?", this::summary, listArgs.toArray());
        Long total = jdbc.queryForObject("select count(*) from perfumestore.notifications" + where, Long.class, args.toArray());
        long count = total == null ? 0 : total;
        return new PageResponse<>(items, page, size, count, PageResponse.totalPages(count, size),
                List.of(new com.perfumestore.backend.shared.api.SortOrder(sortColumn, com.perfumestore.backend.shared.api.SortOrder.Direction.valueOf(direction.toUpperCase()))));
    }

    private NotificationRecord map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new NotificationRecord(rs.getObject("id", UUID.class), NotificationType.valueOf(rs.getString("event_type")),
                rs.getString("event_key"), rs.getObject("order_id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getString("recipient_email"), rs.getString("template_name"), rs.getString("template_version"),
                rs.getString("subject"), rs.getString("body_text"), rs.getString("body_html"), NotificationStatus.valueOf(rs.getString("status")),
                rs.getInt("attempt_count"), rs.getTimestamp("next_attempt_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), timestamp(rs, "last_attempt_at"), timestamp(rs, "sent_at"),
                timestamp(rs, "locked_until"), rs.getString("last_error_code"));
    }

    private NotificationDtos.Summary summary(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new NotificationDtos.Summary(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                rs.getObject(4, UUID.class), rs.getObject(5, UUID.class), rs.getString(6), rs.getInt(7),
                rs.getTimestamp(8).toInstant(), rs.getTimestamp(9).toInstant(), timestamp(rs, 10), rs.getString(11));
    }

    private Instant timestamp(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Instant timestamp(java.sql.ResultSet rs, int column) throws java.sql.SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
