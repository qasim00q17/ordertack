package ordertracker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.entity.NotificationLog;
import ordertracker.entity.Order;
import ordertracker.repository.NotificationLogRepository;
import ordertracker.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender         mailSender;
    private final NotificationLogRepository notifRepo;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.retry-max-attempts:3}")
    private int maxAttempts;

    @Override
    @Async("emailExecutor")
    public void sendOrderStatusUpdate(Order order, String previousStatus) {
        String subject = "Order " + order.getOrderNumber() + " — Status Updated";
        String body = buildStatusUpdateBody(order, previousStatus);
        sendWithRetry(order, order.getUser().getEmail(), subject, body);
    }

    @Override
    @Async("emailExecutor")
    public void sendOrderConfirmation(Order order) {
        String subject = "Order Confirmed — " + order.getOrderNumber();
        String body    = buildConfirmationBody(order);
        sendWithRetry(order, order.getUser().getEmail(), subject, body);
    }

    @Override
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailedNotifications() {
        List<NotificationLog> failed =
                notifRepo.findByStatusAndAttemptCountLessThan("FAILED", maxAttempts);

        if (failed.isEmpty()) return;

        log.info("Retrying {} failed notifications", failed.size());

        for (NotificationLog notif : failed) {
            try {
                send(notif.getRecipientEmail(), notif.getSubject(), "Retry: " + notif.getSubject());
                notif.setStatus("SENT");
                notif.setSentAt(Instant.now());
                log.info("Retry succeeded for notification id={}", notif.getId());
            } catch (MailException e) {
                notif.setAttemptCount(notif.getAttemptCount() + 1);
                notif.setErrorMessage(e.getMessage());
                log.warn("Retry failed for notification id={}: {}", notif.getId(), e.getMessage());
            }
            notifRepo.save(notif);
        }
    }

    private void sendWithRetry(Order order, String to, String subject, String body) {
        NotificationLog log = NotificationLog.builder()
                .order(order)
                .recipientEmail(to)
                .subject(subject)
                .status("PENDING")
                .attemptCount(0)
                .build();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                send(to, subject, body);
                log.setStatus("SENT");
                log.setSentAt(Instant.now());
                log.setAttemptCount(attempt);
                EmailServiceImpl.log.info("Email sent to {} for order {}", to, order.getOrderNumber());
                break;
            } catch (MailException e) {
                log.setAttemptCount(attempt);
                log.setErrorMessage(e.getMessage());
                EmailServiceImpl.log.warn("Email attempt {}/{} failed for {}: {}", attempt, maxAttempts, to, e.getMessage());

                if (attempt == maxAttempts) {
                    log.setStatus("FAILED");
                } else {
                    sleep(2000L * attempt);
                }
            }
        }
        notifRepo.save(log);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    private String buildStatusUpdateBody(Order order, String previousStatus) {
        return """
                Hello %s,
                
                Your order %s has been updated.
                
                Previous status : %s
                Current status  : %s
                
                Shipping address: %s
                %s
                
                Thank you for shopping with us.
                OrderTracker Team
                """.formatted(
                order.getUser().getFullName(),
                order.getOrderNumber(),
                previousStatus,
                order.getStatus().name(),
                order.getShippingAddress(),
                order.getTrackingNumber() != null
                        ? "Tracking number: " + order.getTrackingNumber() : ""
        );
    }

    private String buildConfirmationBody(Order order) {
        return """
                Hello %s,
                
                Your order %s has been confirmed!
                
                Total amount : %s %s
                Shipping to  : %s
                
                We will notify you when your order ships.
                
                Thank you for shopping with us.
                OrderTracker Team
                """.formatted(
                order.getUser().getFullName(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getShippingAddress()
        );
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}