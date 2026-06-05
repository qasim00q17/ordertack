package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;
import ordertracker.enums.OrderStatus;

import java.time.Instant;

@Data
@Builder
public class OrderStatusHistoryResponse {
    private Long        id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String      changedBy;
    private String      reason;
    private Instant     changedAt;
}