package ordertracker.repository;

import ordertracker.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByStatusAndAttemptCountLessThan(String status, int maxAttempt);
    List<NotificationLog> findByOrderId(Long orderId);
}