package ordertracker.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.enums.OrderStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Async("webhookExecutor")
    public void broadcastOrderStatus(Long orderId, String orderNumber,
                                     OrderStatus prev, OrderStatus next) {
        OrderStatusEvent event = OrderStatusEvent.of(orderId, orderNumber, prev, next);

        messagingTemplate.convertAndSend("/topic/orders/" + orderId, event);

        messagingTemplate.convertAndSend("/topic/admin/orders", event);

        log.debug("WebSocket broadcast: order={} status={}", orderNumber, next);
    }

    @Async("webhookExecutor")
    public void sendToUser(String userEmail, Long orderId, String orderNumber,
                           OrderStatus prev, OrderStatus next) {
        OrderStatusEvent event = OrderStatusEvent.of(orderId, orderNumber, prev, next);
        messagingTemplate.convertAndSendToUser(userEmail, "/queue/orders", event);
        log.debug("WebSocket private msg: user={} order={} status={}", userEmail, orderNumber, next);
    }
}