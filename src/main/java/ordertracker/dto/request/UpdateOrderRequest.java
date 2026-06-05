package ordertracker.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import ordertracker.enums.OrderStatus;

@Data
public class UpdateOrderRequest {

    private OrderStatus status;

    @Size(max = 500)
    private String shippingAddress;

    @Size(max = 100)
    private String paymentReference;

    @Size(max = 100)
    private String trackingNumber;

    @Size(max = 1000)
    private String notes;

    @Size(max = 500)
    private String reason;   // reason for status change (audit)
}