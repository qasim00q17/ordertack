package ordertracker.enums;

public enum OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REFUNDED
}