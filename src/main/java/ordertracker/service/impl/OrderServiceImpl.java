package ordertracker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.dto.request.CreateOrderRequest;
import ordertracker.dto.request.UpdateOrderRequest;
import ordertracker.dto.response.*;
import ordertracker.entity.*;
import ordertracker.enums.OrderStatus;
import ordertracker.exception.BusinessException;
import ordertracker.exception.ResourceNotFoundException;
import ordertracker.repository.*;
import ordertracker.service.OrderService;
import ordertracker.util.OrderNumberGenerator;
import ordertracker.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderNumberGenerator         orderNumberGenerator;
    private final SecurityUtils                securityUtils;

    @Override
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        BigDecimal total = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(orderNumberGenerator.generate())
                .user(currentUser)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .order(order)
                        .productName(i.getProductName())
                        .productSku(i.getProductSku())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        order.getItems().addAll(items);

        Order saved = orderRepository.save(order);
        recordHistory(saved, null, OrderStatus.PENDING, "SYSTEM", "Order created");
        log.info("Order created: {} for user: {}", saved.getOrderNumber(), currentUser.getEmail());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = findOrderWithAccess(id);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        checkAccess(order);
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<OrderResponse> page = orderRepository.findByUserId(userId, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrdersByStatus(OrderStatus status, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        Page<OrderResponse> page = orderRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        return PageResponse.from(orderRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse update(Long id, UpdateOrderRequest request) {
        Order order = findOrderWithAccess(id);
        OrderStatus previousStatus = order.getStatus();

        if (request.getStatus() != null && !request.getStatus().equals(previousStatus)) {
            validateTransition(previousStatus, request.getStatus());
            order.setStatus(request.getStatus());
            recordHistory(order, previousStatus, request.getStatus(),
                    securityUtils.getCurrentUser().getEmail(),
                    request.getReason());
            log.info("Order {} status changed: {} → {}", order.getOrderNumber(), previousStatus, request.getStatus());
        }

        if (request.getShippingAddress() != null) order.setShippingAddress(request.getShippingAddress());
        if (request.getPaymentReference() != null) order.setPaymentReference(request.getPaymentReference());
        if (request.getTrackingNumber() != null)   order.setTrackingNumber(request.getTrackingNumber());
        if (request.getNotes() != null)             order.setNotes(request.getNotes());

        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        Order order = findOrderWithAccess(id);
        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.SHIPPED) {
            throw new BusinessException("Cannot cancel order in status: " + order.getStatus());
        }
        OrderStatus prev = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        recordHistory(order, prev, OrderStatus.CANCELLED,
                securityUtils.getCurrentUser().getEmail(), "Cancelled by user");
        orderRepository.save(order);
        log.info("Order {} cancelled", order.getOrderNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getStatusHistory(Long id) {
        findOrderWithAccess(id);
        return historyRepository.findByOrderIdOrderByChangedAtDesc(id).stream()
                .map(h -> OrderStatusHistoryResponse.builder()
                        .id(h.getId())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .changedBy(h.getChangedBy())
                        .reason(h.getReason())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();
    }

    public void updateStatusInternal(Order order, OrderStatus newStatus,
                                     String changedBy, String reason) {
        OrderStatus prev = order.getStatus();
        order.setStatus(newStatus);
        recordHistory(order, prev, newStatus, changedBy, reason);
        orderRepository.save(order);
        log.info("Order {} status updated internally: {} → {}", order.getOrderNumber(), prev, newStatus);
    }

    private Order findOrderWithAccess(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        checkAccess(order);
        return order;
    }

    private void checkAccess(Order order) {
        if (!securityUtils.isAdmin()) {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (!order.getUser().getId().equals(currentUserId)) {
                throw new BusinessException("Access denied to order: " + order.getId());
            }
        }
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case PENDING          -> to == OrderStatus.PAYMENT_PENDING || to == OrderStatus.CANCELLED;
            case PAYMENT_PENDING  -> to == OrderStatus.CONFIRMED || to == OrderStatus.PAYMENT_FAILED;
            case PAYMENT_FAILED   -> to == OrderStatus.PAYMENT_PENDING || to == OrderStatus.CANCELLED;
            case CONFIRMED        -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING       -> to == OrderStatus.SHIPPED    || to == OrderStatus.CANCELLED;
            case SHIPPED          -> to == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED;
            case DELIVERED        -> to == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
        if (!valid) {
            throw new BusinessException(
                    "Invalid status transition: " + from + " → " + to);
        }
    }

    private void recordHistory(Order order, OrderStatus prev, OrderStatus next,
                               String by, String reason) {
        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .previousStatus(prev)
                .newStatus(next)
                .changedBy(by)
                .reason(reason)
                .build());
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .userId(o.getUser().getId())
                .userEmail(o.getUser().getEmail())
                .status(o.getStatus())
                .totalAmount(o.getTotalAmount())
                .currency(o.getCurrency())
                .shippingAddress(o.getShippingAddress())
                .paymentReference(o.getPaymentReference())
                .trackingNumber(o.getTrackingNumber())
                .notes(o.getNotes())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .items(o.getItems().stream()
                        .map(i -> OrderItemResponse.builder()
                                .id(i.getId())
                                .productName(i.getProductName())
                                .productSku(i.getProductSku())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .totalPrice(i.getTotalPrice())
                                .build())
                        .toList())
                .build();
    }
}