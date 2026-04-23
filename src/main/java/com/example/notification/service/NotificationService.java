package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.SendNotificationRequest;
import com.example.notification.entity.NotificationLog;
import com.example.notification.repository.NotificationLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationLogRepository notificationLogRepository;
    
    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;
    
    // Metrics
    private final Counter emailSentCounter;
    private final Counter smsSentCounter;
    private final Counter notificationFailedCounter;
    
    public NotificationService(NotificationLogRepository notificationLogRepository,
                                MeterRegistry meterRegistry) {
        this.notificationLogRepository = notificationLogRepository;
        
        this.emailSentCounter = Counter.builder("notifications.email.sent")
                .description("Total number of emails sent")
                .register(meterRegistry);
        
        this.smsSentCounter = Counter.builder("notifications.sms.sent")
                .description("Total number of SMS sent")
                .register(meterRegistry);
        
        this.notificationFailedCounter = Counter.builder("notifications.failed")
                .description("Total number of failed notifications")
                .register(meterRegistry);
    }
    
    @Transactional
    public NotificationResponse sendOrderConfirmation(SendNotificationRequest request) {
        log.info("Sending ORDER_CONFIRMED notification for order: {}, customer: {}", 
                 request.getOrderId(), maskEmail(request.getCustomerEmail()));
        
        String emailMessage = buildOrderConfirmationMessage(request);
        String smsMessage = "Order #" + request.getOrderId().toString().substring(0, 8) + " confirmed!";
        
        return sendNotifications(request, "ORDER_CONFIRMED", 
                                 "Order Confirmation - ECI Store", 
                                 emailMessage, smsMessage);
    }
    
    @Transactional
    public NotificationResponse sendPaymentSuccess(SendNotificationRequest request) {
        log.info("Sending PAYMENT_SUCCESS notification for order: {}, customer: {}", 
                 request.getOrderId(), maskEmail(request.getCustomerEmail()));
        
        String emailMessage = buildPaymentSuccessMessage(request);
        String smsMessage = "Payment successful for order #" + request.getOrderId().toString().substring(0, 8);
        
        return sendNotifications(request, "PAYMENT_SUCCESS", 
                                 "Payment Successful - ECI Store", 
                                 emailMessage, smsMessage);
    }
    
    @Transactional
    public NotificationResponse sendShipmentCreated(SendNotificationRequest request) {
        log.info("Sending SHIPMENT_CREATED notification for order: {}, customer: {}", 
                 request.getOrderId(), maskEmail(request.getCustomerEmail()));
        
        String emailMessage = buildShipmentCreatedMessage(request);
        String smsMessage = "Your order #" + request.getOrderId().toString().substring(0, 8) + " has been shipped!";
        
        return sendNotifications(request, "SHIPMENT_CREATED", 
                                 "Your Order Has Shipped - ECI Store", 
                                 emailMessage, smsMessage);
    }
    
    @Transactional
    public NotificationResponse sendShipmentDelivered(SendNotificationRequest request) {
        log.info("Sending SHIPMENT_DELIVERED notification for order: {}, customer: {}", 
                 request.getOrderId(), maskEmail(request.getCustomerEmail()));
        
        String emailMessage = buildShipmentDeliveredMessage(request);
        String smsMessage = "Your order #" + request.getOrderId().toString().substring(0, 8) + " has been delivered!";
        
        return sendNotifications(request, "SHIPMENT_DELIVERED", 
                                 "Order Delivered - ECI Store", 
                                 emailMessage, smsMessage);
    }
    
    private NotificationResponse sendNotifications(SendNotificationRequest request, 
                                                    String event, 
                                                    String subject, 
                                                    String emailMessage, 
                                                    String smsMessage) {
        NotificationResponse emailResponse = null;
        NotificationResponse smsResponse = null;
        
        // Send Email if email address provided
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isEmpty() && emailEnabled) {
            emailResponse = sendEmail(request, event, subject, emailMessage);
        }
        
        // Send SMS if phone number provided
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isEmpty() && smsEnabled) {
            smsResponse = sendSms(request, event, smsMessage);
        }
        
        // Return the first successful response
        if (emailResponse != null) return emailResponse;
        if (smsResponse != null) return smsResponse;
        
        // If both failed or not sent, return a pending response
        NotificationResponse response = new NotificationResponse();
        response.setStatus("PENDING");
        response.setEvent(event);
        response.setOrderId(request.getOrderId());
        return response;
    }
    
    private NotificationResponse sendEmail(SendNotificationRequest request, String event, 
                                            String subject, String message) {
        try {
            // Simulate email sending (in real app, use JavaMailSender)
            log.info("📧 SENDING EMAIL to: {} | Subject: {} | Message: {}", 
                     maskEmail(request.getCustomerEmail()), subject, truncateMessage(message));
            
            // Simulate API call delay
            Thread.sleep(100);
            
            NotificationLog logEntry = new NotificationLog();
            logEntry.setOrderId(request.getOrderId());
            logEntry.setCustomerId(request.getCustomerId());
            logEntry.setCustomerEmail(maskEmail(request.getCustomerEmail()));  // Masked in DB
            logEntry.setType("EMAIL");
            logEntry.setEvent(event);
            logEntry.setSubject(subject);
            logEntry.setMessage(message);
            logEntry.setStatus("SENT");
            logEntry.setSentAt(LocalDateTime.now());
            
            notificationLogRepository.save(logEntry);
            emailSentCounter.increment();
            
            log.info("Email sent successfully to: {}", maskEmail(request.getCustomerEmail()));
            
            return new NotificationResponse(
                logEntry.getNotificationId(),
                logEntry.getOrderId(),
                "EMAIL",
                event,
                "SENT",
                logEntry.getSentAt(),
                logEntry.getCreatedAt()
            );
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", maskEmail(request.getCustomerEmail()), e);
            notificationFailedCounter.increment();
            
            NotificationLog logEntry = new NotificationLog();
            logEntry.setOrderId(request.getOrderId());
            logEntry.setCustomerId(request.getCustomerId());
            logEntry.setCustomerEmail(maskEmail(request.getCustomerEmail()));
            logEntry.setType("EMAIL");
            logEntry.setEvent(event);
            logEntry.setSubject(subject);
            logEntry.setMessage(message);
            logEntry.setStatus("FAILED");
            logEntry.setFailureReason(e.getMessage());
            
            notificationLogRepository.save(logEntry);
            
            return new NotificationResponse(
                logEntry.getNotificationId(),
                logEntry.getOrderId(),
                "EMAIL",
                event,
                "FAILED",
                null,
                logEntry.getCreatedAt()
            );
        }
    }
    
    private NotificationResponse sendSms(SendNotificationRequest request, String event, String message) {
        try {
            // Simulate SMS sending
            log.info("📱 SENDING SMS to: {} | Message: {}", 
                     maskPhone(request.getCustomerPhone()), message);
            
            // Simulate API call delay
            Thread.sleep(50);
            
            NotificationLog logEntry = new NotificationLog();
            logEntry.setOrderId(request.getOrderId());
            logEntry.setCustomerId(request.getCustomerId());
            logEntry.setCustomerPhone(maskPhone(request.getCustomerPhone()));  // Masked in DB
            logEntry.setType("SMS");
            logEntry.setEvent(event);
            logEntry.setMessage(message);
            logEntry.setStatus("SENT");
            logEntry.setSentAt(LocalDateTime.now());
            
            notificationLogRepository.save(logEntry);
            smsSentCounter.increment();
            
            log.info("SMS sent successfully to: {}", maskPhone(request.getCustomerPhone()));
            
            return new NotificationResponse(
                logEntry.getNotificationId(),
                logEntry.getOrderId(),
                "SMS",
                event,
                "SENT",
                logEntry.getSentAt(),
                logEntry.getCreatedAt()
            );
            
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", maskPhone(request.getCustomerPhone()), e);
            notificationFailedCounter.increment();
            
            NotificationLog logEntry = new NotificationLog();
            logEntry.setOrderId(request.getOrderId());
            logEntry.setCustomerId(request.getCustomerId());
            logEntry.setCustomerPhone(maskPhone(request.getCustomerPhone()));
            logEntry.setType("SMS");
            logEntry.setEvent(event);
            logEntry.setMessage(message);
            logEntry.setStatus("FAILED");
            logEntry.setFailureReason(e.getMessage());
            
            notificationLogRepository.save(logEntry);
            
            return new NotificationResponse(
                logEntry.getNotificationId(),
                logEntry.getOrderId(),
                "SMS",
                event,
                "FAILED",
                null,
                logEntry.getCreatedAt()
            );
        }
    }
    
    // Message builders
    private String buildOrderConfirmationMessage(SendNotificationRequest request) {
        return String.format("""
            Dear Customer,
            
            Thank you for your order!
            
            Order ID: %s
            Status: Confirmed
            
            We will notify you once your order is shipped.
            
            Thank you for shopping with ECI!
            """, request.getOrderId());
    }
    
    private String buildPaymentSuccessMessage(SendNotificationRequest request) {
        return String.format("""
            Dear Customer,
            
            Payment for your order has been successfully processed.
            
            Order ID: %s
            Payment Status: Successful
            
            Your order is now being processed for shipping.
            
            Thank you for shopping with ECI!
            """, request.getOrderId());
    }
    
    private String buildShipmentCreatedMessage(SendNotificationRequest request) {
        return String.format("""
            Dear Customer,
            
            Great news! Your order has been shipped.
            
            Order ID: %s
            Status: Shipped
            
            You can track your package using the tracking number provided.
            
            Thank you for shopping with ECI!
            """, request.getOrderId());
    }
    
    private String buildShipmentDeliveredMessage(SendNotificationRequest request) {
        return String.format("""
            Dear Customer,
            
            Your order has been delivered!
            
            Order ID: %s
            Status: Delivered
            
            We hope you enjoy your purchase. Please leave a review!
            
            Thank you for shopping with ECI!
            """, request.getOrderId());
    }
    
    // Mask sensitive data (as required by assignment)
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        return email.replaceAll("(^[^@]{3})[^@]+(@.*$)", "$1***$2");
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        if (phone.length() <= 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
    
    private String truncateMessage(String message) {
        if (message == null) return null;
        if (message.length() <= 100) return message;
        return message.substring(0, 100) + "...";
    }
    
    // Query methods
    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsByOrderId(UUID orderId) {
        log.debug("Fetching notifications for order: {}", orderId);
        return notificationLogRepository.findByOrderId(orderId);
    }
    
    @Transactional(readOnly = true)
    public List<NotificationLog> getAllNotifications() {
        log.debug("Fetching all notifications");
        return notificationLogRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public NotificationLog getNotificationById(UUID notificationId) {
        log.debug("Fetching notification by ID: {}", notificationId);
        return notificationLogRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
    }
}