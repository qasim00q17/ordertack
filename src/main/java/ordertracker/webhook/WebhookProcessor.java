package ordertracker.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.dto.request.PaymentWebhookRequest;
import ordertracker.dto.request.ShipmentWebhookRequest;
import ordertracker.entity.Order;
import ordertracker.entity.WebhookEvent;
import ordertracker.enums.OrderStatus;
import ordertracker.enums.WebhookEventType;
import ordertracker.enums.WebhookStatus;
import ordertracker.repository.OrderRepository;
import ordertracker.repository.WebhookEventRepository;
import ordertracker.service.EmailService;
import ordertracker.service.impl.OrderServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookProcessor {

    private final WebhookEventRepository webhookRepo;
    private final OrderRepository        orderRepo;
    private final EmailService           emailService;
    private final OrderServiceImpl       orderService;

    @Async("webhookExecutor")
    @Transactional
    public void processPayment(Long eventId, PaymentWebhookRequest request) {
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
    public void processShipment(Long eventId, ShipmentWebhookRequest request) {
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
                case SHIPMENT_FAILED ->
                        log.warn("Shipment failed for order {}: {}", order.getOrderNumber(), request.getFailureReason());
                default ->
                        log.warn("Unhandled shipment event type: {}", event.getEventType());
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
}