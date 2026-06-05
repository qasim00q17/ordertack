package ordertracker.service;

import ordertracker.dto.request.CreateOrderRequest;
import ordertracker.dto.request.UpdateOrderRequest;
import ordertracker.dto.response.OrderResponse;
import ordertracker.dto.response.OrderStatusHistoryResponse;
import ordertracker.dto.response.PageResponse;
import ordertracker.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse create(CreateOrderRequest request);
    OrderResponse getById(Long id);
    OrderResponse getByOrderNumber(String orderNumber);
    PageResponse<OrderResponse> getMyOrders(Pageable pageable);
    PageResponse<OrderResponse> getMyOrdersByStatus(OrderStatus status, Pageable pageable);
    PageResponse<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse update(Long id, UpdateOrderRequest request);
    void cancel(Long id);
    List<OrderStatusHistoryResponse> getStatusHistory(Long id);
}