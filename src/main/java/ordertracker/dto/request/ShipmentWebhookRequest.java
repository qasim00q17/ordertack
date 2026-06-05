package ordertracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShipmentWebhookRequest {

    @NotBlank
    private String eventType;        // shipment.created | shipped | out_for_delivery | delivered | failed

    @NotBlank
    private String orderId;          // order number

    @NotBlank
    private String trackingNumber;

    private String carrier;
    private String estimatedDelivery;
    private String location;
    private String timestamp;
    private String failureReason;
}