package ordertracker.service.impl;

import lombok.RequiredArgsConstructor;
import ordertracker.dto.response.AdminDashboardResponse;
import ordertracker.enums.WebhookStatus;
import ordertracker.repository.NotificationLogRepository;
import ordertracker.repository.OrderRepository;
import ordertracker.repository.UserRepository;
import ordertracker.repository.WebhookEventRepository;
import ordertracker.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final OrderRepository        orderRepo;
    private final UserRepository         userRepo;
    private final WebhookEventRepository webhookRepo;
    private final NotificationLogRepository notifRepo;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalOrders = orderRepo.count();
        long totalUsers  = userRepo.count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        orderRepo.countGroupByStatus().forEach(row ->
                byStatus.put(row[0].toString(), (Long) row[1]));

        long webhookTotal     = webhookRepo.count();
        long webhookProcessed = webhookRepo.countByStatus(WebhookStatus.PROCESSED);
        long webhookFailed    = webhookRepo.countByStatus(WebhookStatus.FAILED);
        double successRate    = webhookTotal > 0
                ? (double) webhookProcessed / webhookTotal * 100 : 0;

        long notifSent   = notifRepo.findByStatusAndAttemptCountLessThan("SENT",   Integer.MAX_VALUE).size();
        long notifFailed = notifRepo.findByStatusAndAttemptCountLessThan("FAILED", Integer.MAX_VALUE).size();

        return AdminDashboardResponse.builder()
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .ordersByStatus(byStatus)
                .webhookTotal(webhookTotal)
                .webhookProcessed(webhookProcessed)
                .webhookFailed(webhookFailed)
                .webhookSuccessRate(Math.round(successRate * 100.0) / 100.0)
                .notificationsSent(notifSent)
                .notificationsFailed(notifFailed)
                .build();
    }
}