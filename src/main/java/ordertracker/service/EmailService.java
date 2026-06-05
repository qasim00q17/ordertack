package ordertracker.service;

import ordertracker.entity.Order;

public interface EmailService {
    void sendOrderStatusUpdate(Order order, String previousStatus);
    void sendOrderConfirmation(Order order);
    void retryFailedNotifications();
}