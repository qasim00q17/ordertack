package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;
import ordertracker.enums.WebhookEventType;
import ordertracker.enums.WebhookStatus;

import java.time.Instant;

@Data
@Builder
public class WebhookEventResponse {
    private Long             id;
    private String           source;
    private WebhookEventType eventType;
    private WebhookStatus    status;
    private String           orderReference;
    private String           payload;
    private String           responseMessage;
    private Instant          receivedAt;
    private Instant          processedAt;
    private Integer          retryCount;
    private String           ipAddress;
    private Instant          createdAt;
}