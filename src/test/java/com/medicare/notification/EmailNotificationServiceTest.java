package com.medicare.notification;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.Doctor;
import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.medicine.MedicineSchedule;
import com.medicare.medicine.NotificationDelivery;
import com.medicare.medicine.ReminderOccurrence;
import com.medicare.medicine.ScheduleFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    private Clock clock;
    private EmailNotificationService emailService;
    private User recipient;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-01T14:00:00Z"), ZoneId.of("UTC")); // 14:00 UTC
        emailService = new EmailNotificationService(mailSenderProvider, preferenceRepository, clock);
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.test.local");
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@medicare.local");

        recipient = new User("patient@example.com", "pass", "John", "Doe", Role.PATIENT);
        recipient.setId(1L);
        recipient.setTimeZone("UTC");
    }

    @Test
    void testThrowsSmtpNotConfiguredWhenMailHostIsEmpty() {
        ReflectionTestUtils.setField(emailService, "mailHost", "");
        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");

        NotificationDeliveryException ex = assertThrows(NotificationDeliveryException.class,
            () -> emailService.deliver(delivery));
        assertEquals("SMTP_NOT_CONFIGURED", ex.getErrorCategory());
    }

    @Test
    void testThrowsChannelDisabledWhenUserPreferenceDisabled() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        NotificationPreference pref = new NotificationPreference(recipient);
        pref.setEmailEnabled(false);
        when(preferenceRepository.findByUserId(recipient.getId())).thenReturn(Optional.of(pref));

        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");

        NotificationDeliveryException ex = assertThrows(NotificationDeliveryException.class,
            () -> emailService.deliver(delivery));
        assertEquals("CHANNEL_DISABLED", ex.getErrorCategory());
    }

    @Test
    void testThrowsQuietHoursActiveWhenCurrentTimeWithinQuietHours() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        NotificationPreference pref = new NotificationPreference(recipient);
        pref.setEmailEnabled(true);
        // Current clock is 14:00 UTC
        pref.setQuietStart(LocalTime.of(13, 0));
        pref.setQuietEnd(LocalTime.of(16, 0));
        when(preferenceRepository.findByUserId(recipient.getId())).thenReturn(Optional.of(pref));

        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");

        NotificationDeliveryException ex = assertThrows(NotificationDeliveryException.class,
            () -> emailService.deliver(delivery));
        assertEquals("QUIET_HOURS_ACTIVE", ex.getErrorCategory());
    }

    @Test
    void testSuccessfulDeliveryForMedicineReminderWithoutLeakingSensitiveDiagnosis() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        NotificationPreference pref = new NotificationPreference(recipient);
        pref.setEmailEnabled(true);
        pref.setQuietStart(LocalTime.of(22, 0));
        pref.setQuietEnd(LocalTime.of(7, 0));
        when(preferenceRepository.findByUserId(recipient.getId())).thenReturn(Optional.of(pref));

        MedicineSchedule schedule = new MedicineSchedule(
            recipient, "Blood Pressure Med", "Confidential instructions", ScheduleFrequency.DAILY,
            LocalTime.of(14, 0), null, LocalDate.of(2026, 11, 1), "UTC"
        );
        ReminderOccurrence occurrence = new ReminderOccurrence(schedule, Instant.parse("2026-11-01T14:00:00Z"), "key-1");
        NotificationDelivery delivery = new NotificationDelivery(recipient, occurrence, "EMAIL", "PENDING");

        emailService.deliver(delivery);

        assertEquals("SENT", delivery.getState());
        assertNotNull(delivery.getSentAt());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertEquals("patient@example.com", sent.getTo()[0]);
        // Subject should be generic and not leak sensitive instructions
        assertEquals("MediCare Reminder: Scheduled Medication Due", sent.getSubject());
        assertTrue(sent.getText().contains("Blood Pressure Med"));
    }

    @Test
    void testSuccessfulDeliveryForAppointmentReminder() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        when(preferenceRepository.findByUserId(recipient.getId())).thenReturn(Optional.empty());

        Doctor doctor = new Doctor();
        doctor.setFullName("Dr. Sarah Smith");
        doctor.setSpecialty("Cardiology");

        Appointment appt = new Appointment();
        appt.setId(42L);
        appt.setPatient(recipient);
        appt.setDoctor(doctor);
        appt.setAppointmentAt(Instant.parse("2026-11-02T10:00:00Z"));
        appt.setStatus("BOOKED");

        NotificationDelivery delivery = new NotificationDelivery(recipient, appt, "EMAIL", "PENDING");

        emailService.deliver(delivery);

        assertEquals("SENT", delivery.getState());
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("MediCare Reminder: Upcoming Healthcare Appointment", captor.getValue().getSubject());
        assertTrue(captor.getValue().getText().contains("Dr. Sarah Smith"));
    }

    @Test
    void testMailExceptionIsMappedToSmtpSendFailed() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        when(preferenceRepository.findByUserId(recipient.getId())).thenReturn(Optional.empty());
        doThrow(new MailSendException("Connection refused")).when(javaMailSender).send(any(SimpleMailMessage.class));

        NotificationDelivery delivery = new NotificationDelivery(recipient, "EMAIL", "PENDING");

        NotificationDeliveryException ex = assertThrows(NotificationDeliveryException.class,
            () -> emailService.deliver(delivery));
        assertEquals("SMTP_SEND_FAILED", ex.getErrorCategory());
    }
}
