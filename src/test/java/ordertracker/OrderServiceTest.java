package ordertracker;

import ordertracker.dto.request.CreateOrderRequest;
import ordertracker.dto.response.OrderResponse;
import ordertracker.entity.Order;
import ordertracker.entity.User;
import ordertracker.enums.OrderStatus;
import ordertracker.enums.Role;
import ordertracker.exception.BusinessException;
import ordertracker.repository.OrderRepository;
import ordertracker.repository.OrderStatusHistoryRepository;
import ordertracker.service.impl.OrderServiceImpl;
import ordertracker.util.OrderNumberGenerator;
import ordertracker.util.SecurityUtils;
import ordertracker.webhook.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository              orderRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock OrderNumberGenerator         orderNumberGenerator;
    @Mock SecurityUtils                securityUtils;
    @Mock WebSocketNotificationService wsNotifier;

    @InjectMocks OrderServiceImpl orderService;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .fullName("Ali Mammadov")
                .email("ali@example.com")
                .password("encoded")
                .role(Role.USER)
                .enabled(true)
                .build();
        try {
            var idField = ordertracker.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception ignored) {}

        testOrder = Order.builder()
                .orderNumber("ORD-20240101-ABCD1234")
                .user(testUser)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .currency("USD")
                .shippingAddress("Baku, AZ")
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();
        try {
            var idField = ordertracker.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testOrder, 10L);
        } catch (Exception ignored) {}
    }

    @Test
    void create_order_success() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setShippingAddress("Baku, AZ");
        req.setCurrency("USD");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductName("Laptop");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("999.99"));
        req.setItems(List.of(item));

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(orderNumberGenerator.generate()).thenReturn("ORD-TEST-001");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.create(req);

        assertThat(response.getOrderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("999.99");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getById_ownerCanAccess() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        OrderResponse response = orderService.getById(10L);

        assertThat(response.getOrderNumber()).isEqualTo("ORD-20240101-ABCD1234");
    }

    @Test
    void getById_otherUser_throwsBusinessException() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(99L); // different user

        assertThatThrownBy(() -> orderService.getById(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void cancel_pending_order_succeeds() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(orderRepository.save(any())).thenReturn(testOrder);
        when(historyRepository.save(any())).thenReturn(null);

        orderService.cancel(10L);

        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(wsNotifier).broadcastOrderStatus(any(), any(), any(), eq(OrderStatus.CANCELLED));
    }

    @Test
    void cancel_delivered_order_throwsBusinessException() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> orderService.cancel(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel");
    }
}