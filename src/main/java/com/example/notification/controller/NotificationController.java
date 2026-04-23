package com.example.notification.controller;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.SendNotificationRequest;
import com.example.notification.entity.NotificationLog;
import com.example.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@Tag(name = "Notification Management", description = "Endpoints for sending and tracking notifications")
public class NotificationController {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @PostMapping("/order-confirmation")
    @Operation(summary = "Send order confirmation", description = "Sends order confirmation email/SMS")
    public ResponseEntity<NotificationResponse> sendOrderConfirmation(@Valid @RequestBody SendNotificationRequest request) {
        log.info("POST /v1/notifications/order-confirmation - orderId: {}", request.getOrderId());
        NotificationResponse response = notificationService.sendOrderConfirmation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/payment-success")
    @Operation(summary = "Send payment success", description = "Sends payment success notification")
    public ResponseEntity<NotificationResponse> sendPaymentSuccess(@Valid @RequestBody SendNotificationRequest request) {
        log.info("POST /v1/notifications/payment-success - orderId: {}", request.getOrderId());
        NotificationResponse response = notificationService.sendPaymentSuccess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/shipment-created")
    @Operation(summary = "Send shipment created", description = "Sends shipment created notification")
    public ResponseEntity<NotificationResponse> sendShipmentCreated(@Valid @RequestBody SendNotificationRequest request) {
        log.info("POST /v1/notifications/shipment-created - orderId: {}", request.getOrderId());
        NotificationResponse response = notificationService.sendShipmentCreated(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/shipment-delivered")
    @Operation(summary = "Send shipment delivered", description = "Sends shipment delivered notification")
    public ResponseEntity<NotificationResponse> sendShipmentDelivered(@Valid @RequestBody SendNotificationRequest request) {
        log.info("POST /v1/notifications/shipment-delivered - orderId: {}", request.getOrderId());
        NotificationResponse response = notificationService.sendShipmentDelivered(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all notifications", description = "Returns all notification logs")
    public ResponseEntity<List<NotificationLog>> getAllNotifications() {
        log.info("GET /v1/notifications");
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
    
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get notifications by order ID", description = "Returns notifications for an order")
    public ResponseEntity<List<NotificationLog>> getNotificationsByOrderId(@PathVariable UUID orderId) {
        log.info("GET /v1/notifications/order/{}", orderId);
        return ResponseEntity.ok(notificationService.getNotificationsByOrderId(orderId));
    }
    
    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID", description = "Returns a single notification")
    public ResponseEntity<NotificationLog> getNotificationById(@PathVariable UUID notificationId) {
        log.info("GET /v1/notifications/{}", notificationId);
        return ResponseEntity.ok(notificationService.getNotificationById(notificationId));
    }
}