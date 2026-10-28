package com.medicare.medicine;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificationController {

    private final ReminderService reminderService;

    public NotificationController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/api/notifications")
    public List<ReminderService.NotificationView> notifications() {
        return reminderService.notifications();
    }
}
