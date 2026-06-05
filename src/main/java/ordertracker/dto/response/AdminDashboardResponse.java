package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalOrders;
    private long totalUsers;
    private Map<String, Long> ordersByStatus;
    private long webhookTotal;
    private long webhookProcessed;
    private long webhookFailed;
    private double webhookSuccessRate;
    private long notificationsSent;
    private long notificationsFailed;
}