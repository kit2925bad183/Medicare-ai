package com.medicare.medicine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ReminderWorker {

    private static final Logger log = LoggerFactory.getLogger(ReminderWorker.class);

    private final ReminderService reminderService;

    public ReminderWorker(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(fixedDelayString = "${medicare.reminders.poll-delay-ms:30000}")
    public void poll() {
        try {
            reminderService.generateUpcomingOccurrences();
            reminderService.publishDueInAppNotifications();
            reminderService.processDueAppointmentReminders();
            reminderService.processPendingRetries();
        } catch (Exception ex) {
            log.warn("Reminder poll failed", ex);
        }
    }
}
