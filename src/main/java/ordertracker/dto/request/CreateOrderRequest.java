package ordertracker.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500)
    private String shippingAddress;

    @Size(max = 3, min = 3, message = "Currency must be 3-letter ISO code")
    private String currency = "USD";

    @Size(max = 1000)
    private String notes;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        private String productName;

        @Size(max = 50)
        private String productSku;

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal unitPrice;
    }
}