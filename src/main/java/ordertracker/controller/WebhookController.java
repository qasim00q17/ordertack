package ordertracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.dto.request.PaymentWebhookRequest;
import ordertracker.dto.request.ShipmentWebhookRequest;
import ordertracker.dto.response.PageResponse;
import ordertracker.dto.response.WebhookAckResponse;
import ordertracker.dto.response.WebhookEventResponse;
import ordertracker.enums.WebhookStatus;
import ordertracker.service.WebhookService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook receiver and log viewer endpoints")
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper   objectMapper;

    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Receive payment gateway webhook events")
    public WebhookAckResponse handlePayment(
            @Valid @RequestBody PaymentWebhookRequest request,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            HttpServletRequest httpRequest) throws Exception {

        String rawPayload = objectMapper.writeValueAsString(request);
        String ip = getClientIp(httpRequest);

        Long eventId = webhookService.handlePayment(request, signature, rawPayload, ip);
        log.info("Payment webhook accepted: eventId={}", eventId);
        return WebhookAckResponse.accepted(eventId);
    }

    @PostMapping("/shipment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Receive shipment tracking webhook events")
    public WebhookAckResponse handleShipment(
            @Valid @RequestBody ShipmentWebhookRequest request,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            HttpServletRequest httpRequest) throws Exception {

        String rawPayload = objectMapper.writeValueAsString(request);
        String ip = getClientIp(httpRequest);

        Long eventId = webhookService.handleShipment(request, signature, rawPayload, ip);
        log.info("Shipment webhook accepted: eventId={}", eventId);
        return WebhookAckResponse.accepted(eventId);
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get webhook event logs — ADMIN only")
    public PageResponse<WebhookEventResponse> getLogs(
            @RequestParam(required = false) WebhookStatus status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant   = to   != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        return webhookService.getLogs(
                status, source, fromInstant, toInstant,
                PageRequest.of(page, size, Sort.by("receivedAt").descending())
        );
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get webhook event log by ID — ADMIN only")
    public WebhookEventResponse getLogById(@PathVariable Long id) {
        return webhookService.getLogById(id);
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip;
    }
}