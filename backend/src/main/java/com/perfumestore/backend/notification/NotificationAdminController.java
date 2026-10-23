package com.perfumestore.backend.notification;

import com.perfumestore.backend.shared.api.PageQuery;
import com.perfumestore.backend.shared.api.PageResponse;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationAdminController {
    private final NotificationService notifications;

    public NotificationAdminController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping
    public PageResponse<NotificationDtos.Summary> list(@Valid PageQuery page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType) {
        return notifications.page(page, status, eventType);
    }
}
