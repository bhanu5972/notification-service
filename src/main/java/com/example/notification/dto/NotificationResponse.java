package com.example.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationResponse {
    private UUID notificationId;
    private UUID orderId;
    private String type;
    private String event;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    
    // Default constructor
    public NotificationResponse() {}
    
    // Constructor
    public NotificationResponse(UUID notificationId, UUID orderId, String type, 
                                String event, String status, LocalDateTime sentAt, 
                                LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.orderId = orderId;
        this.type = type;
        this.event = event;
        this.status = status;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
    
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}