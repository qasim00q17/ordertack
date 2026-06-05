package ordertracker.entity;

import jakarta.persistence.*;
import lombok.*;
import ordertracker.enums.WebhookEventType;
import ordertracker.enums.WebhookStatus;

import java.time.Instant;

@Entity
@Table(name = "webhook_events",
        indexes = {
                @Index(name = "idx_webhook_order_ref", columnList = "order_reference"),
                @Index(name = "idx_webhook_status",    columnList = "status"),
                @Index(name = "idx_webhook_type",      columnList = "event_type"),
                @Index(name = "idx_webhook_received",  columnList = "received_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent extends BaseEntity {

    @Column(name = "source", nullable = false, length = 50)
    private String source;                  // "PAYMENT" | "SHIPMENT"

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private WebhookEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.RECEIVED;

    @Column(name = "order_reference", length = 100)
    private String orderReference;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "response_message", length = 500)
    private String responseMessage;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "signature_header", length = 200)
    private String signatureHeader;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}