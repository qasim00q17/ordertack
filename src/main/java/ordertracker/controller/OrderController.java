package ordertracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ordertracker.dto.request.CreateOrderRequest;
import ordertracker.dto.request.UpdateOrderRequest;
import ordertracker.dto.response.OrderResponse;
import ordertracker.dto.response.OrderStatusHistoryResponse;
import ordertracker.dto.response.PageResponse;
import ordertracker.enums.OrderStatus;
import ordertracker.service.ExportService;
import ordertracker.service.OrderService;
import ordertracker.util.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService   orderService;
    private final ExportService  exportService;
    private final SecurityUtils  securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new order")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public OrderResponse getByOrderNumber(@PathVariable String orderNumber) {
        return orderService.getByOrderNumber(orderNumber);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my orders (paginated)")
    public PageResponse<OrderResponse> getMyOrders(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")             @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by status")      @RequestParam(required = false) OrderStatus status) {

        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return status != null
                ? orderService.getMyOrdersByStatus(status, pr)
                : orderService.getMyOrders(pr);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders — ADMIN only")
    public PageResponse<OrderResponse> getAllOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.getAllOrders(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order (status, address, notes, etc.)")
    public OrderResponse update(@PathVariable Long id,
                                @Valid @RequestBody UpdateOrderRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel an order")
    public void cancel(@PathVariable Long id) {
        orderService.cancel(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get status change history for an order")
    public List<OrderStatusHistoryResponse> getHistory(@PathVariable Long id) {
        return orderService.getStatusHistory(id);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export my orders as CSV")
    public ResponseEntity<byte[]> exportCsv() throws IOException {
        byte[] data = exportService.exportOrdersToCsv(securityUtils.getCurrentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export my orders as Excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        byte[] data = exportService.exportOrdersToExcel(securityUtils.getCurrentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/admin/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export ALL orders as CSV — ADMIN only")
    public ResponseEntity<byte[]> adminExportCsv() throws IOException {
        byte[] data = exportService.exportOrdersToCsv(null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all-orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}