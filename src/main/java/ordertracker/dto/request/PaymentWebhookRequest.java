package ordertracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentWebhookRequest {

    @NotBlank
    private String eventType;

    @NotBlank
    private String paymentReference;
    @NotBlank
    private String orderId;

    private String failureReason;

    private String currency;
    private String amount;
    private String externalTransactionId;
    private String timestamp;
}