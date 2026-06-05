package ordertracker.service;

import ordertracker.dto.request.PaymentWebhookRequest;
import ordertracker.dto.request.ShipmentWebhookRequest;
import ordertracker.dto.response.PageResponse;
import ordertracker.dto.response.WebhookEventResponse;
import ordertracker.enums.WebhookStatus;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface WebhookService {
    Long handlePayment(PaymentWebhookRequest request, String signature, String rawPayload, String ip);
    Long handleShipment(ShipmentWebhookRequest request, String signature, String rawPayload, String ip);
    PageResponse<WebhookEventResponse> getLogs(WebhookStatus status, String source, Instant from, Instant to, Pageable pageable);
    WebhookEventResponse getLogById(Long id);
}