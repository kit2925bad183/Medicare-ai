package com.medicare.notification;

import com.medicare.identity.User;
import com.medicare.medicine.NotificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class EmailNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final NotificationPreferenceRepository preferenceRepository;
    private final Clock clock;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${medicare.mail.from:noreply@medicare.local}")
    private String fromAddress;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    NotificationPreferenceRepository preferenceRepository,
                                    Clock clock) {
        this.mailSenderProvider = mailSenderProvider;
        this.preferenceRepository = preferenceRepository;
        this.clock = clock;
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public boolean isAvailable() {
        return mailHost != null && !mailHost.isBlank() && mailSenderProvider.getIfAvailable() != null;
    }

    @Override
    public void deliver(NotificationDelivery delivery) throws NotificationDeliveryException {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || mailHost == null || mailHost.isBlank()) {
            throw new NotificationDeliveryException("SMTP_NOT_CONFIGURED", "SMTP server is not configured in this environment.");
        }

        User recipient = delivery.getRecipient();
        var pref = preferenceRepository.findByUserId(recipient.getId()).orElse(null);
        if (pref != null && !pref.isEmailEnabled()) {
            throw new NotificationDeliveryException("CHANNEL_DISABLED", "Email reminders are disabled by user preference.");
        }

        if (pref != null && isQuietHours(pref, recipient.getTimeZone())) {
            throw new NotificationDeliveryException("QUIET_HOURS_ACTIVE", "Scheduled email falls inside configured quiet hours.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient.getEmail());

        if (delivery.getOccurrence() != null) {
            String medName = delivery.getOccurrence().getSchedule().getMedicineName();
            message.setSubject("MediCare Reminder: Scheduled Medication Due");
            message.setText("Hello " + recipient.getFirstName() + ",\n\n"
                + "This is a reminder that your medication (" + medName + ") is due.\n"
                + "Please log in to your MediCare AI dashboard to record your dose action.\n\n"
                + "Best regards,\nMediCare AI Team");
        } else if (delivery.getAppointment() != null) {
            var appt = delivery.getAppointment();
            String doctorName = appt.getDoctor() != null ? appt.getDoctor().getFullName() : "your doctor";
            message.setSubject("MediCare Reminder: Upcoming Healthcare Appointment");
            message.setText("Hello " + recipient.getFirstName() + ",\n\n"
                + "This is a reminder of your upcoming health appointment with " + doctorName + ".\n"
                + "Time: " + appt.getAppointmentAt() + "\n\n"
                + "Best regards,\nMediCare AI Team");
        } else {
            message.setSubject("MediCare Notification");
            message.setText("Hello " + recipient.getFirstName() + ",\n\nYou have a new reminder waiting in your dashboard.\n\nMediCare AI");
        }

        try {
            sender.send(message);
            delivery.setState("SENT");
            delivery.setSentAt(clock.instant());
            delivery.setNextRetryAt(null);
            log.info("Email reminder successfully dispatched for delivery ID {}", delivery.getId());
        } catch (MailException ex) {
            log.warn("Failed to dispatch email reminder for delivery ID {}: {}", delivery.getId(), ex.getMessage());
            throw new NotificationDeliveryException("SMTP_SEND_FAILED", "Failed to deliver email: " + ex.getMessage(), ex);
        }
    }

    private boolean isQuietHours(NotificationPreference pref, String timeZone) {
        LocalTime start = pref.getQuietStart();
        LocalTime end = pref.getQuietEnd();
        if (start == null || end == null) {
            return false;
        }
        ZoneId zone = ZoneId.of(timeZone != null ? timeZone : "UTC");
        LocalTime now = LocalTime.now(clock.withZone(zone));
        if (start.isBefore(end)) {
            return now.isAfter(start) && now.isBefore(end);
        } else {
            return now.isAfter(start) || now.isBefore(end);
        }
    }
}
