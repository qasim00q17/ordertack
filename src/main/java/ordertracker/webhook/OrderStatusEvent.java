package ordertracker.webhook;

import lombok.Builder;
import lombok.Data;
import ordertracker.enums.OrderStatus;

import java.time.Instant;

@Data
@Builder
public class OrderStatusEvent {
    private Long        orderId;
    private String      orderNumber;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String      message;
    private Instant     timestamp;

    public static OrderStatusEvent of(Long orderId, String orderNumber,
                                      OrderStatus prev, OrderStatus next) {
        return OrderStatusEvent.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .previousStatus(prev)
                .newStatus(next)
                .message("Order " + orderNumber + " status changed to " + next.name())
                .timestamp(Instant.now())
                .build();
    }
}