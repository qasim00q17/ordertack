package ordertracker.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.dto.request.PaymentWebhookRequest;
import ordertracker.dto.request.ShipmentWebhookRequest;
import ordertracker.dto.response.PageResponse;
import ordertracker.dto.response.WebhookEventResponse;
import ordertracker.entity.WebhookEvent;
import ordertracker.enums.WebhookEventType;
import ordertracker.enums.WebhookStatus;
import ordertracker.exception.ResourceNotFoundException;
import ordertracker.repository.WebhookEventRepository;
import ordertracker.service.WebhookService;
import ordertracker.util.HmacSignatureVerifier;
import ordertracker.webhook.WebhookProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookEventRepository webhookRepo;
    private final HmacSignatureVerifier  signatureVerifier;
    private final WebhookProcessor       webhookProcessor;
    private final ObjectMapper           objectMapper;

    @Value("${webhook.payment-secret}")
    private String paymentSecret;

    @Value("${webhook.shipment-secret}")
    private String shipmentSecret;

    @Override
    @Transactional
    public Long handlePayment(PaymentWebhookRequest request,
                              String signature, String rawPayload, String ip) {

        if (!signatureVerifier.verify(rawPayload, signature, paymentSecret)) {
            log.warn("Invalid payment webhook signature from {}", ip);
        }

        WebhookEvent event = WebhookEvent.builder()
                .source("PAYMENT")
                .eventType(resolvePaymentEventType(request.getEventType()))
                .status(WebhookStatus.RECEIVED)
                .orderReference(request.getOrderId())
                .payload(rawPayload)
                .signatureHeader(signature)
                .ipAddress(ip)
                .build();

        event = webhookRepo.save(event);
        log.info("Payment webhook received: type={} order={}", event.getEventType(), request.getOrderId());

        webhookProcessor.processPayment(event.getId(), request);
        return event.getId();
    }

    @Override
    @Transactional
    public Long handleShipment(ShipmentWebhookRequest request,
                               String signature, String rawPayload, String ip) {

        if (!signatureVerifier.verify(rawPayload, signature, shipmentSecret)) {
            log.warn("Invalid shipment webhook signature from {}", ip);
        }

        WebhookEvent event = WebhookEvent.builder()
                .source("SHIPMENT")
                .eventType(resolveShipmentEventType(request.getEventType()))
                .status(WebhookStatus.RECEIVED)
                .orderReference(request.getOrderId())
                .payload(rawPayload)
                .signatureHeader(signature)
                .ipAddress(ip)
                .build();

        event = webhookRepo.save(event);
        log.info("Shipment webhook received: type={} order={}", event.getEventType(), request.getOrderId());

        webhookProcessor.processShipment(event.getId(), request);
        return event.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WebhookEventResponse> getLogs(WebhookStatus status, String source,
                                                      Instant from, Instant to, Pageable pageable) {
        return PageResponse.from(
                webhookRepo.findFiltered(status, source, from, to, pageable).map(this::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookEventResponse getLogById(Long id) {
        return webhookRepo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", id));
    }

    private WebhookEventType resolvePaymentEventType(String type) {
        return switch (type.toLowerCase()) {
            case "payment.succeeded", "payment_succeeded" -> WebhookEventType.PAYMENT_SUCCEEDED;
            case "payment.failed",    "payment_failed"    -> WebhookEventType.PAYMENT_FAILED;
            case "payment.refunded",  "payment_refunded"  -> WebhookEventType.PAYMENT_REFUNDED;
            default -> WebhookEventType.UNKNOWN;
        };
    }

    private WebhookEventType resolveShipmentEventType(String type) {
        return switch (type.toLowerCase()) {
            case "shipment.created",          "shipment_created"            -> WebhookEventType.SHIPMENT_CREATED;
            case "shipment.shipped",          "shipment_shipped", "shipped" -> WebhookEventType.SHIPMENT_SHIPPED;
            case "shipment.out_for_delivery", "out_for_delivery"            -> WebhookEventType.SHIPMENT_OUT_FOR_DELIVERY;
            case "shipment.delivered",        "delivered"                   -> WebhookEventType.SHIPMENT_DELIVERED;
            case "shipment.failed",           "shipment_failed"             -> WebhookEventType.SHIPMENT_FAILED;
            default -> WebhookEventType.UNKNOWN;
        };
    }

    private WebhookEventResponse toResponse(WebhookEvent e) {
        return WebhookEventResponse.builder()
                .id(e.getId())
                .source(e.getSource())
                .eventType(e.getEventType())
                .status(e.getStatus())
                .orderReference(e.getOrderReference())
                .payload(e.getPayload())
                .responseMessage(e.getResponseMessage())
                .receivedAt(e.getReceivedAt())
                .processedAt(e.getProcessedAt())
                .retryCount(e.getRetryCount())
                .ipAddress(e.getIpAddress())
                .createdAt(e.getCreatedAt())
                .build();
    }
}