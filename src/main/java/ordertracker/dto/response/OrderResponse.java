package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;
import ordertracker.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long        id;
    private String      orderNumber;
    private Long        userId;
    private String      userEmail;
    private OrderStatus status;
    private BigDecimal  totalAmount;
    private String      currency;
    private String      shippingAddress;
    private String      paymentReference;
    private String      trackingNumber;
    private String      notes;
    private Instant     createdAt;
    private Instant     updatedAt;
    private List<OrderItemResponse> items;
}