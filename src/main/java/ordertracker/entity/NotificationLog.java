package ordertracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_logs",
        indexes = @Index(name = "idx_notif_order_id", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(name = "subject", nullable = false, length = 300)
    private String subject;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";   // PENDING | SENT | FAILED

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;
}