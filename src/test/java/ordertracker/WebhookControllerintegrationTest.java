package ordertracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import ordertracker.dto.request.PaymentWebhookRequest;
import ordertracker.dto.request.ShipmentWebhookRequest;
import ordertracker.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean WebhookService webhookService;

    @Test
    void paymentWebhook_accepted() throws Exception {
        PaymentWebhookRequest req = new PaymentWebhookRequest();
        req.setEventType("payment.succeeded");
        req.setPaymentReference("PAY-123");
        req.setOrderId("ORD-20240101-ABCD1234");

        when(webhookService.handlePayment(any(), any(), any(), any())).thenReturn(1L);

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value(1));
    }

    @Test
    void shipmentWebhook_accepted() throws Exception {
        ShipmentWebhookRequest req = new ShipmentWebhookRequest();
        req.setEventType("shipment.shipped");
        req.setOrderId("ORD-20240101-ABCD1234");
        req.setTrackingNumber("TRACK-999");

        when(webhookService.handleShipment(any(), any(), any(), any())).thenReturn(2L);

        mockMvc.perform(post("/api/webhooks/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value(2));
    }

    @Test
    void paymentWebhook_missingOrderId_returns422() throws Exception {
        PaymentWebhookRequest req = new PaymentWebhookRequest();
        req.setEventType("payment.succeeded");

        mockMvc.perform(post("/api/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
}