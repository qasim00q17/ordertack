package ordertracker.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.dto.request.PaymentWebhookRequest;
import ordertracker.dto.request.ShipmentWebhookRequest;
import ordertracker.dto.response.PageResponse;
import ordertracker.dto.response.WebhookEventResponse;
import ordertracker.entity.Order;
import ordertracker.entity.WebhookEvent;
import ordertracker.enums.OrderStatus;
import ordertracker.enums.WebhookEventType;
import ordertracker.enums.WebhookStatus;
import ordertracker.exception.ResourceNotFoundException;
import ordertracker.repository.OrderRepository;
import ordertracker.repository.WebhookEventRepository;
import ordertracker.service.EmailService;
import ordertracker.service.WebhookService;
import ordertracker.util.HmacSignatureVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookEventRepository webhookRepo;
    private final OrderRepository        orderRepo;
    private final EmailService           emailService;
    private final HmacSignatureVerifier  signatureVerifier;
    private final OrderServiceImpl       orderService;
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

        WebhookEventType eventType = resolvePaymentEventType(request.getEventType());

        WebhookEvent event = WebhookEvent.builder()
                .source("PAYMENT")
                .eventType(eventType)
                .status(WebhookStatus.RECEIVED)
                .orderReference(request.getOrderId())
                .payload(rawPayload)
                .signatureHeader(signature)
                .ipAddress(ip)
                .build();

        event = webhookRepo.save(event);
        log.info("Payment webhook received: type={} order={}", eventType, request.getOrderId());

        processPaymentAsync(event.getId(), request);
        return event.getId();
    }

    @Override
    @Transactional
    public Long handleShipment(ShipmentWebhookRequest request,
                               String signature, String rawPayload, String ip) {

        if (!signatureVerifier.verify(rawPayload, signature, shipmentSecret)) {
            log.warn("Invalid shipment webhook signature from {}", ip);
        }

        WebhookEventType eventType = resolveShipmentEventType(request.getEventType());

        WebhookEvent event = WebhookEvent.builder()
                .source("SHIPMENT")
                .eventType(eventType)
                .status(WebhookStatus.RECEIVED)
                .orderReference(request.getOrderId())
                .payload(rawPayload)
                .signatureHeader(signature)
                .ipAddress(ip)
                .build();

        event = webhookRepo.save(event);
        log.info("Shipment webhook received: type={} order={}", eventType, request.getOrderId());

        processShipmentAsync(event.getId(), request);
        return event.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WebhookEventResponse> getLogs(WebhookStatus status, String source,
                                                      Instant from, Instant to, Pageable pageable) {
        return PageResponse.from(
                webhookRepo.findFiltered(status, source, from, to, pageable)
                        .map(this::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookEventResponse getLogById(Long id) {
        return webhookRepo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", id));
    }

    @Async("webhookExecutor")
    @Transactional
    public void processPaymentAsync(Long eventId, PaymentWebhookRequest request) {
        WebhookEvent event = webhookRepo.findById(eventId).orElse(null);
        if (event == null) return;

        try {
            event.setStatus(WebhookStatus.PROCESSING);
            webhookRepo.save(event);

            Order order = orderRepo.findByOrderNumber(request.getOrderId())
                    .orElseGet(() -> orderRepo.findByPaymentReference(request.getPaymentReference())
                            .orElse(null));

            if (order == null) {
                event.setStatus(WebhookStatus.IGNORED);
                event.setResponseMessage("Order not found: " + request.getOrderId());
                webhookRepo.save(event);
                return;
            }

            String prevStatus = order.getStatus().name();

            switch (event.getEventType()) {
                case PAYMENT_SUCCEEDED -> {
                    order.setPaymentReference(request.getPaymentReference());
                    orderService.updateStatusInternal(order, OrderStatus.CONFIRMED,
                            "WEBHOOK_PAYMENT", "Payment succeeded");
                    emailService.sendOrderConfirmation(order);
                }
                case PAYMENT_FAILED -> {
                    orderService.updateStatusInternal(order, OrderStatus.PAYMENT_FAILED,
                            "WEBHOOK_PAYMENT", "Payment failed: " + request.getFailureReason());
                    emailService.sendOrderStatusUpdate(order, prevStatus);
                }
                case PAYMENT_REFUNDED -> {
                    orderService.updateStatusInternal(order, OrderStatus.REFUNDED,
                            "WEBHOOK_PAYMENT", "Payment refunded");
                    emailService.sendOrderStatusUpdate(order, prevStatus);
                }
                default -> log.warn("Unhandled payment event type: {}", event.getEventType());
            }

            event.setStatus(WebhookStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            event.setResponseMessage("Processed successfully");

        } catch (Exception e) {
            log.error("Error processing payment webhook {}: {}", eventId, e.getMessage(), e);
            event.setStatus(WebhookStatus.FAILED);
            event.setResponseMessage(e.getMessage());
            event.setRetryCount(event.getRetryCount() + 1);
        }

        webhookRepo.save(event);
    }

    @Async("webhookExecutor")
    @Transactional
    public void processShipmentAsync(Long eventId, ShipmentWebhookRequest request) {
        WebhookEvent event = webhookRepo.findById(eventId).orElse(null);
        if (event == null) return;

        try {
            event.setStatus(WebhookStatus.PROCESSING);
            webhookRepo.save(event);

            Order order = orderRepo.findByOrderNumber(request.getOrderId())
                    .orElseGet(() -> orderRepo.findByTrackingNumber(request.getTrackingNumber())
                            .orElse(null));

            if (order == null) {
                event.setStatus(WebhookStatus.IGNORED);
                event.setResponseMessage("Order not found: " + request.getOrderId());
                webhookRepo.save(event);
                return;
            }

            String prevStatus = order.getStatus().name();

            switch (event.getEventType()) {
                case SHIPMENT_CREATED -> {
                    order.setTrackingNumber(request.getTrackingNumber());
                    orderService.updateStatusInternal(order, OrderStatus.PROCESSING,
                            "WEBHOOK_SHIPMENT", "Shipment created");
                }
                case SHIPMENT_SHIPPED -> {
                    order.setTrackingNumber(request.getTrackingNumber());
                    orderService.updateStatusInternal(order, OrderStatus.SHIPPED,
                            "WEBHOOK_SHIPMENT", "Order shipped via " + request.getCarrier());
                    emailService.sendOrderStatusUpdate(order, prevStatus);
                }
                case SHIPMENT_OUT_FOR_DELIVERY -> {
                    orderService.updateStatusInternal(order, OrderStatus.OUT_FOR_DELIVERY,
                            "WEBHOOK_SHIPMENT", "Out for delivery");
                    emailService.sendOrderStatusUpdate(order, prevStatus);
                }
                case SHIPMENT_DELIVERED -> {
                    orderService.updateStatusInternal(order, OrderStatus.DELIVERED,
                            "WEBHOOK_SHIPMENT", "Delivered");
                    emailService.sendOrderStatusUpdate(order, prevStatus);
                }
                case SHIPMENT_FAILED -> {
                    event.setStatus(WebhookStatus.PROCESSED);
                    event.setResponseMessage("Shipment failed: " + request.getFailureReason());
                    log.warn("Shipment failed for order {}: {}", order.getOrderNumber(), request.getFailureReason());
                }
                default -> log.warn("Unhandled shipment event type: {}", event.getEventType());
            }

            event.setStatus(WebhookStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            event.setResponseMessage("Processed successfully");

        } catch (Exception e) {
            log.error("Error processing shipment webhook {}: {}", eventId, e.getMessage(), e);
            event.setStatus(WebhookStatus.FAILED);
            event.setResponseMessage(e.getMessage());
            event.setRetryCount(event.getRetryCount() + 1);
        }

        webhookRepo.save(event);
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
            case "shipment.created",          "shipment_created"          -> WebhookEventType.SHIPMENT_CREATED;
            case "shipment.shipped",          "shipment_shipped", "shipped" -> WebhookEventType.SHIPMENT_SHIPPED;
            case "shipment.out_for_delivery", "out_for_delivery"           -> WebhookEventType.SHIPMENT_OUT_FOR_DELIVERY;
            case "shipment.delivered",        "delivered"                  -> WebhookEventType.SHIPMENT_DELIVERED;
            case "shipment.failed",           "shipment_failed"            -> WebhookEventType.SHIPMENT_FAILED;
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