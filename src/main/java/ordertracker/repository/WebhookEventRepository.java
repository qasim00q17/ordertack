package ordertracker.repository;

import ordertracker.entity.WebhookEvent;
import ordertracker.enums.WebhookEventType;
import ordertracker.enums.WebhookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Page<WebhookEvent> findByStatus(WebhookStatus status, Pageable pageable);

    Page<WebhookEvent> findBySource(String source, Pageable pageable);

    Page<WebhookEvent> findByEventType(WebhookEventType eventType, Pageable pageable);

    Page<WebhookEvent> findByReceivedAtBetween(Instant from, Instant to, Pageable pageable);

    List<WebhookEvent> findByStatusAndRetryCountLessThan(WebhookStatus status, int maxRetry);

    @Query("""
            SELECT w FROM WebhookEvent w
            WHERE (:status IS NULL OR w.status = :status)
              AND (:source IS NULL OR w.source = :source)
              AND (:from IS NULL OR w.receivedAt >= :from)
              AND (:to   IS NULL OR w.receivedAt <= :to)
            """)
    Page<WebhookEvent> findFiltered(
            @Param("status") WebhookStatus status,
            @Param("source") String source,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.status = :status")
    long countByStatus(@Param("status") WebhookStatus status);
}