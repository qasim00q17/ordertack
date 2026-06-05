package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class WebhookAckResponse {
    private String  status;
    private String  message;
    private Long    eventId;
    private Instant receivedAt;

    public static WebhookAckResponse accepted(Long eventId) {
        return WebhookAckResponse.builder()
                .status("ACCEPTED")
                .message("Webhook received and queued for processing")
                .eventId(eventId)
                .receivedAt(Instant.now())
                .build();
    }
}