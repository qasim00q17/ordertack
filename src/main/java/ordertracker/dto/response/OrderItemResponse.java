package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long       id;
    private String     productName;
    private String     productSku;
    private Integer    quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}